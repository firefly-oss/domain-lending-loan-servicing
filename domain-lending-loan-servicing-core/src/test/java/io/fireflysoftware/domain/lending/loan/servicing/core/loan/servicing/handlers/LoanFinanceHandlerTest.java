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


package io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.handlers;

import io.fireflysoftware.core.lending.servicing.sdk.api.LoanBalanceApi;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanRepaymentRecordApi;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanRepaymentScheduleApi;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanServicingCaseApi;
import io.fireflysoftware.core.lending.servicing.sdk.model.LoanRepaymentRecordDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.LoanRepaymentScheduleDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.LoanServicingCaseDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.PaginationResponseLoanBalanceDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.PaginationResponseLoanRepaymentScheduleDTO;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.CalendarTerms;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.LoanPrice;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.LoanPricePort;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.MemoryLoanCalendar;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.ProductInterestCatalog;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.SandboxLedgerAdapter;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.SandboxLoanPriceAdapter;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.ScheduleState;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.StoredInstallment;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.AllocateRepaymentCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.GenerateAmortizationScheduleCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.ReplaceRemainingScheduleCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.schedule.AmortizationScheduleCalculator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoanFinanceHandlerTest {

    @Test
    void scheduleIsStoredFromThePricedRateAndReadBack() {
        UUID caseId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        LoanServicingCaseDTO loan = new LoanServicingCaseDTO(caseId);
        loan.setProductId(productId);
        loan.setPrincipalAmount(new BigDecimal("1200.00"));
        loan.setLoanTerm(12);
        loan.setInterestRate(new BigDecimal("1.00"));
        loan.setPaymentFrequency(LoanServicingCaseDTO.PaymentFrequencyEnum.MONTHLY);
        loan.setAmortizationMethod(LoanServicingCaseDTO.AmortizationMethodEnum.EQUAL_INSTALLMENT);
        loan.setOriginationDate(LocalDate.of(2026, 1, 1));

        LoanServicingCaseApi cases = mock(LoanServicingCaseApi.class);
        when(cases.getServicingCaseById(eq(caseId), any())).thenReturn(Mono.just(loan));
        when(cases.updateServicingCase(eq(caseId), any(), any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(1)));
        RecordingPrice prices = new RecordingPrice(new BigDecimal("7.99"), new BigDecimal("88.88"), "EUR");
        MemoryLoanCalendar calendar = new MemoryLoanCalendar();
        GenerateAmortizationScheduleHandler handler = new GenerateAmortizationScheduleHandler(cases, prices, calendar);

        List<UUID> ids = handler.doHandle(new GenerateAmortizationScheduleCommand(caseId, LocalDate.of(2026, 2, 1))).block();
        List<StoredInstallment> read = calendar.read(caseId).block();

        assertThat(prices.principal).isEqualByComparingTo("1200.00");
        assertThat(prices.term).isEqualTo(12);
        assertThat(prices.productId).isEqualTo(productId);
        assertThat(loan.getInterestRate()).isEqualByComparingTo("7.99");
        assertThat(ids).hasSize(12);
        assertThat(read).hasSize(12);
        assertThat(read.get(0).dueDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(read.get(0).principalDue()).isEqualByComparingTo(
                AmortizationScheduleCalculator.generate(
                        new BigDecimal("1200.00"), new BigDecimal("7.99"), 12, "MONTHLY",
                        "EQUAL_INSTALLMENT", LocalDate.of(2026, 2, 1), new BigDecimal("88.88"))
                        .get(0).principalDue());
        assertThat(read.get(0).interestDue()).isEqualByComparingTo(
                AmortizationScheduleCalculator.generate(
                        new BigDecimal("1200.00"), new BigDecimal("7.99"), 12, "MONTHLY",
                        "EQUAL_INSTALLMENT", LocalDate.of(2026, 2, 1), new BigDecimal("88.88"))
                        .get(0).interestDue());
        assertThat(read.get(11).remainingBalance()).isEqualByComparingTo("0.00");
        assertThat(read).allSatisfy(row -> assertThat(row.loanServicingCaseId()).isEqualTo(caseId));
    }

    @Test
    void repaymentMarksTheInstallmentPaidAndPostsCash() {
        UUID caseId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        UUID repaymentId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        LoanServicingCaseDTO loan = new LoanServicingCaseDTO(caseId);
        loan.setProductId(ProductInterestCatalog.PERSONAL_LOAN);
        loan.setPrincipalAmount(new BigDecimal("10000.00"));
        loan.setLoanTerm(12);

        LoanRepaymentScheduleDTO row = new LoanRepaymentScheduleDTO(scheduleId);
        row.setLoanServicingCaseId(caseId);
        row.setInstallmentNumber(1);
        row.setDueDate(LocalDate.of(2026, 11, 1));
        row.setPrincipalDue(new BigDecimal("80.00"));
        row.setInterestDue(new BigDecimal("20.00"));
        row.setFeeDue(BigDecimal.ZERO.setScale(2));
        row.setTotalDue(new BigDecimal("100.00"));
        row.setIsPaid(false);
        PaginationResponseLoanRepaymentScheduleDTO schedulesPage = new PaginationResponseLoanRepaymentScheduleDTO();
        schedulesPage.setContent(List.of(row));
        PaginationResponseLoanBalanceDTO balances = new PaginationResponseLoanBalanceDTO();
        balances.setContent(List.of());

        LoanRepaymentRecordApi records = mock(LoanRepaymentRecordApi.class);
        LoanBalanceApi balanceApi = mock(LoanBalanceApi.class);
        LoanRepaymentScheduleApi schedules = mock(LoanRepaymentScheduleApi.class);
        LoanServicingCaseApi cases = mock(LoanServicingCaseApi.class);
        when(balanceApi.findAllBalances(eq(caseId), any(), any())).thenReturn(Mono.just(balances));
        when(cases.getServicingCaseById(eq(caseId), any())).thenReturn(Mono.just(loan));
        when(schedules.findAllRepaymentSchedules(eq(caseId), any(), any())).thenReturn(Mono.just(schedulesPage));
        when(schedules.updateRepaymentSchedule(eq(caseId), eq(scheduleId), any(), any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(2)));
        when(records.createRepaymentRecord(eq(caseId), any(), any())).thenAnswer(invocation -> {
            LoanRepaymentRecordDTO incoming = invocation.getArgument(1);
            LoanRepaymentRecordDTO saved = new LoanRepaymentRecordDTO(repaymentId);
            saved.setLoanServicingCaseId(incoming.getLoanServicingCaseId());
            saved.setPaymentAmount(incoming.getPaymentAmount());
            saved.setPaymentDate(incoming.getPaymentDate());
            saved.setTransactionId(incoming.getTransactionId());
            saved.setIsPartialPayment(incoming.getIsPartialPayment());
            return Mono.just(saved);
        });

        SandboxLedgerAdapter ledger = new SandboxLedgerAdapter();
        AllocateRepaymentHandler handler = new AllocateRepaymentHandler(
                records, balanceApi, schedules, cases, ledger, new SandboxLoanPriceAdapter());
        LoanRepaymentRecordDTO payment = new LoanRepaymentRecordDTO();
        payment.setPaymentAmount(new BigDecimal("100.00"));
        payment.setPaymentDate(LocalDate.of(2026, 11, 1));
        payment.setTransactionId(transactionId);
        payment.setIsPartialPayment(false);

        AllocateRepaymentCommand.Allocation allocation = handler.doHandle(
                new AllocateRepaymentCommand(caseId, payment, false)).block();

        ArgumentCaptor<LoanRepaymentScheduleDTO> updated = ArgumentCaptor.forClass(LoanRepaymentScheduleDTO.class);
        verify(schedules).updateRepaymentSchedule(eq(caseId), eq(scheduleId), updated.capture(), any());
        assertThat(updated.getValue().getIsPaid()).isTrue();
        assertThat(updated.getValue().getPaidDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(updated.getValue().getPaidAmount()).isEqualByComparingTo("100.00");
        assertThat(allocation.ledgerPostingId()).isNotNull();
        assertThat(allocation.repaymentId()).isEqualTo(repaymentId);
        assertThat(ledger.post(new io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.CashMovement(
                new BigDecimal("100.00"), "EUR", transactionId.toString(),
                UUID.fromString("00000000-0000-4000-8000-000000000021"),
                UUID.fromString("00000000-0000-4000-8000-000000000022"))).block())
                .isEqualTo(allocation.ledgerPostingId());
    }

    @Test
    void restructureReplacesTheRemainingScheduleAndKeepsThePreviousRows() {
        UUID caseId = UUID.randomUUID();
        LoanServicingCaseDTO loan = new LoanServicingCaseDTO(caseId);
        loan.setProductId(ProductInterestCatalog.PERSONAL_LOAN);
        loan.setPrincipalAmount(new BigDecimal("5000.00"));
        loan.setLoanTerm(12);
        loan.setInterestRate(new BigDecimal("1.00"));
        loan.setPaymentFrequency(LoanServicingCaseDTO.PaymentFrequencyEnum.MONTHLY);
        loan.setAmortizationMethod(LoanServicingCaseDTO.AmortizationMethodEnum.EQUAL_INSTALLMENT);

        LoanServicingCaseApi cases = mock(LoanServicingCaseApi.class);
        when(cases.getServicingCaseById(eq(caseId), any())).thenReturn(Mono.just(loan));
        when(cases.updateServicingCase(eq(caseId), any(), any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(1)));
        MemoryLoanCalendar calendar = new MemoryLoanCalendar();
        calendar.store(caseId, new CalendarTerms(
                new BigDecimal("5000.00"), new BigDecimal("1.00"), 3, "MONTHLY",
                "EQUAL_INSTALLMENT", LocalDate.of(2026, 3, 1), null)).block();
        List<UUID> original = calendar.read(caseId).block().stream().map(StoredInstallment::id).toList();

        ReplaceRemainingScheduleHandler handler = new ReplaceRemainingScheduleHandler(
                cases, new SandboxLoanPriceAdapter(), calendar);
        loan.setLoanTerm(24);
        var revision = handler.doHandle(new ReplaceRemainingScheduleCommand(caseId)).block();
        List<StoredInstallment> current = calendar.read(caseId).block();

        assertThat(revision.loanServicingCaseId()).isEqualTo(caseId);
        assertThat(revision.previous()).extracting(StoredInstallment::id).containsExactlyElementsOf(original);
        assertThat(revision.previous()).allSatisfy(row -> assertThat(row.status()).isEqualTo(ScheduleState.SUPERSEDED));
        assertThat(current).hasSize(24);
        assertThat(current).allSatisfy(row -> {
            assertThat(row.loanServicingCaseId()).isEqualTo(caseId);
            assertThat(row.status()).isEqualTo(ScheduleState.CURRENT);
        });
        assertThat(loan.getInterestRate()).isEqualByComparingTo("7.99");

        List<StoredInstallment> restored = calendar.restore(revision).block();
        assertThat(restored).extracting(StoredInstallment::id).containsExactlyInAnyOrderElementsOf(original);
        assertThat(restored).allSatisfy(row -> assertThat(row.loanServicingCaseId()).isEqualTo(caseId));
    }

    private static final class RecordingPrice implements LoanPricePort {
        private final LoanPrice price;
        private BigDecimal principal;
        private int term;
        private UUID productId;

        private RecordingPrice(BigDecimal rate, BigDecimal installment, String currency) {
            this.price = new LoanPrice(rate, installment, currency);
        }

        @Override
        public Mono<LoanPrice> price(BigDecimal principal, int term, UUID productId) {
            this.principal = principal;
            this.term = term;
            this.productId = productId;
            return Mono.just(price);
        }
    }
}
