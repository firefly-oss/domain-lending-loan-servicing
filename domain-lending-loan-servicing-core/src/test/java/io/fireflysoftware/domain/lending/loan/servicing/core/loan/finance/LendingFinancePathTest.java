/*
 * Copyright 2025 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance;

import io.fireflysoftware.core.banking.ledger.sdk.api.TransactionLegsApi;
import io.fireflysoftware.core.banking.ledger.sdk.api.TransactionsApi;
import io.fireflysoftware.core.banking.ledger.sdk.model.TransactionDTO;
import io.fireflysoftware.core.banking.ledger.sdk.model.TransactionLegDTO;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanAccrualApi;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanRepaymentScheduleApi;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanServicingCaseApi;
import io.fireflysoftware.core.lending.servicing.sdk.model.LoanAccrualDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.LoanServicingCaseDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.PaginationResponseLoanAccrualDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.PaginationResponseLoanRepaymentScheduleDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.PaginationResponseLoanServicingCaseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LendingFinancePathTest {

    @Test
    void sandboxAdaptersAreTheDefaultAndDoNotOpenASocket() {
        new ApplicationContextRunner()
                .withUserConfiguration(SandboxLoanPriceAdapter.class, SdkLoanPriceAdapter.class)
                .run(context -> assertThat(context.getBean(LoanPricePort.class)).isInstanceOf(SandboxLoanPriceAdapter.class));
        new ApplicationContextRunner()
                .withUserConfiguration(SandboxLedgerAdapter.class, SdkLedgerAdapter.class)
                .run(context -> {
                    LedgerPort port = context.getBean(LedgerPort.class);
                    assertThat(port).isInstanceOf(SandboxLedgerAdapter.class);
                    UUID posting = port.post(new CashMovement(
                            new BigDecimal("10.00"), "USD", "ref-1",
                            UUID.randomUUID(), UUID.randomUUID())).block();
                    assertThat(posting).isNotNull();
                });
        new ApplicationContextRunner()
                .withUserConfiguration(SandboxGeneralLedgerAdapter.class, AccountingDomainLedgerAdapter.class)
                .run(context -> assertThat(context.getBean(GeneralLedgerPort.class)).isInstanceOf(SandboxGeneralLedgerAdapter.class));
    }

    @Test
    void priceComesFromTheProductInterestConfiguration() {
        LoanPrice personal = new SandboxLoanPriceAdapter()
                .price(new BigDecimal("10000.00"), 12, ProductInterestCatalog.PERSONAL_LOAN).block();
        LoanPrice leasing = new SandboxLoanPriceAdapter()
                .price(new BigDecimal("30000.00"), 24, ProductInterestCatalog.LEASING).block();
        assertThat(personal.annualRatePercent()).isEqualByComparingTo("7.99");
        assertThat(personal.currency()).isEqualTo("EUR");
        assertThat(personal.installmentAmount()).isPositive();
        assertThat(leasing.annualRatePercent()).isEqualByComparingTo("5.90");
        assertThat(leasing.currency()).isEqualTo("EUR");
    }

    @Test
    void calendarAdapterReadsTheRemainingBalanceFromTheCoreResponse() {
        UUID caseId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        String body = """
                [{"loanRepaymentScheduleId":"%s","loanServicingCaseId":"%s","installmentNumber":1,"dueDate":"2026-02-01","principalDue":88.00,"interestDue":12.00,"remainingBalance":1112.00,"isPaid":false,"scheduleStatus":"CURRENT"}]
                """.formatted(scheduleId, caseId);
        AtomicInteger calls = new AtomicInteger();
        WebClient client = WebClient.builder().exchangeFunction(request -> {
            calls.incrementAndGet();
            assertThat(request.url().getPath()).contains("/repayment-schedules/calendar");
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body)
                    .build());
        }).build();
        List<StoredInstallment> read = new SdkLoanCalendarAdapter(client, "http://localhost:8082")
                .read(caseId).block();
        assertThat(calls).hasValue(1);
        assertThat(read).hasSize(1);
        assertThat(read.get(0).dueDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(read.get(0).principalDue()).isEqualByComparingTo("88.00");
        assertThat(read.get(0).interestDue()).isEqualByComparingTo("12.00");
        assertThat(read.get(0).remainingBalance()).isEqualByComparingTo("1112.00");
        assertThat(read.get(0).loanServicingCaseId()).isEqualTo(caseId);
    }

    @Test
    void liveLedgerAdapterPostsThroughTheLedgerSdk() {
        UUID postingId = UUID.randomUUID();
        TransactionsApi transactions = mock(TransactionsApi.class);
        TransactionLegsApi legs = mock(TransactionLegsApi.class);
        TransactionDTO created = mock(TransactionDTO.class);
        when(created.getTransactionId()).thenReturn(postingId);
        when(created.getTransactionStatus()).thenReturn(TransactionDTO.TransactionStatusEnum.POSTED);
        AtomicInteger lookups = new AtomicInteger();
        when(transactions.findByExternalReference(any(), any())).thenAnswer(invocation -> {
            if (lookups.getAndIncrement() == 0) {
                return Mono.error(WebClientResponseException.create(
                        404, "Not Found", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8));
            }
            return Mono.just(created);
        });
        when(transactions.createTransaction(any(), any())).thenReturn(Mono.just(created));
        when(legs.createTransactionLeg(any(), any(), any())).thenReturn(Mono.just(new TransactionLegDTO()));
        SdkLedgerAdapter adapter = new SdkLedgerAdapter(transactions, legs);
        CashMovement movement = new CashMovement(
                new BigDecimal("25.00"), "EUR", "txn-1", UUID.randomUUID(), UUID.randomUUID());
        assertThat(adapter.post(movement).block()).isEqualTo(postingId);
        assertThat(adapter.post(movement).block()).isEqualTo(postingId);
        verify(transactions, times(1)).createTransaction(any(), any());
        verify(legs, times(2)).createTransactionLeg(any(), any(), any());
    }

    @Test
    void endOfDayAccrualMatchesTheJournalAndDoesNotPostTwice() {
        UUID caseId = UUID.randomUUID();
        LoanServicingCaseDTO loan = new LoanServicingCaseDTO(caseId);
        loan.setProductId(ProductInterestCatalog.PERSONAL_LOAN);
        loan.setPrincipalAmount(new BigDecimal("10000.00"));
        loan.setLoanTerm(12);
        loan.setInterestRate(new BigDecimal("7.99"));
        loan.setServicingStatus(LoanServicingCaseDTO.ServicingStatusEnum.ACTIVE);
        loan.setDayCountConvention(LoanServicingCaseDTO.DayCountConventionEnum.ACTUAL_365);

        LoanServicingCaseApi cases = mock(LoanServicingCaseApi.class);
        PaginationResponseLoanServicingCaseDTO casePage = new PaginationResponseLoanServicingCaseDTO();
        casePage.setContent(List.of(loan));
        casePage.setTotalPages(1);
        when(cases.findAllServicingCases(any(), any())).thenReturn(Mono.just(casePage));

        List<LoanAccrualDTO> saved = new ArrayList<>();
        LoanAccrualApi accruals = mock(LoanAccrualApi.class);
        when(accruals.findAllAccruals(any(), any(), any())).thenAnswer(invocation -> {
            PaginationResponseLoanAccrualDTO page = new PaginationResponseLoanAccrualDTO();
            page.setContent(List.copyOf(saved));
            return Mono.just(page);
        });
        when(accruals.createAccrual(any(), any(), any())).thenAnswer(invocation -> {
            LoanAccrualDTO incoming = invocation.getArgument(1);
            LoanAccrualDTO dto = new LoanAccrualDTO(UUID.randomUUID());
            dto.setLoanServicingCaseId(incoming.getLoanServicingCaseId());
            dto.setAccrualAmount(incoming.getAccrualAmount());
            dto.setAccrualType(incoming.getAccrualType());
            dto.setAccrualDate(incoming.getAccrualDate());
            dto.setNote(incoming.getNote());
            saved.add(dto);
            return Mono.just(dto);
        });

        LoanRepaymentScheduleApi schedules = mock(LoanRepaymentScheduleApi.class);
        PaginationResponseLoanRepaymentScheduleDTO empty = new PaginationResponseLoanRepaymentScheduleDTO();
        empty.setContent(List.of());
        when(schedules.findAllRepaymentSchedules(any(), any(), any())).thenReturn(Mono.just(empty));

        AtomicInteger posts = new AtomicInteger();
        SandboxGeneralLedgerAdapter sandbox = new SandboxGeneralLedgerAdapter();
        GeneralLedgerPort ledger = new GeneralLedgerPort() {
            @Override
            public Mono<InterestPosting> postInterest(LocalDate businessDate, BigDecimal amount, String currency) {
                posts.incrementAndGet();
                return sandbox.postInterest(businessDate, amount, currency);
            }

            @Override
            public Mono<InterestPosting> findInterest(LocalDate businessDate) {
                return sandbox.findInterest(businessDate);
            }
        };

        LoanEndOfDayService service = new LoanEndOfDayService(
                cases, accruals, schedules, new SandboxLoanPriceAdapter(), ledger);
        LocalDate businessDate = LocalDate.of(2026, 10, 21);
        EndOfDayResult first = service.run(businessDate).block();
        EndOfDayResult second = service.run(businessDate).block();

        assertThat(saved).hasSize(1);
        assertThat(posts).hasValue(1);
        assertThat(first.journals()).hasSize(1);
        assertThat(second.journals()).hasSize(1);
        assertThat(second.journals().get(0).id()).isEqualTo(first.journals().get(0).id());
        assertThat(first.journals().get(0).businessDate()).isEqualTo(businessDate);
        assertThat(first.journals().get(0).amount()).isEqualByComparingTo(saved.get(0).getAccrualAmount());
        assertThat(first.journals().get(0).currency()).isEqualTo("EUR");
        assertThat(saved.get(0).getAccrualDate()).isEqualTo(businessDate);
    }
}
