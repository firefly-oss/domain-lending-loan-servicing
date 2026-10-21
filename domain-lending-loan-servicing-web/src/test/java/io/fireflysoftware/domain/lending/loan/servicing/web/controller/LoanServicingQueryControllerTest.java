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


package io.fireflysoftware.domain.lending.loan.servicing.web.controller;

import io.fireflysoftware.core.lending.servicing.sdk.model.*;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.*;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.AmortizationCalendarEntry;
import org.fireflyframework.cqrs.query.QueryBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServicingQueryControllerTest {

    private static final String BASE = "/api/v1/loan-servicing/cases/{caseId}";

    @Mock
    private QueryBus queryBus;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        LoanServicingQueryController controller = new LoanServicingQueryController(queryBus);
        webTestClient = WebTestClient.bindToController(controller).build();
    }

    // ---- GET /balances ----

    @Test
    void getBalances_shouldReturn200() {
        UUID caseId = UUID.randomUUID();
        when(queryBus.<PaginationResponseLoanBalanceDTO>query(any(GetLoanBalancesQuery.class)))
                .thenReturn(Mono.just(new PaginationResponseLoanBalanceDTO()));

        webTestClient.get().uri(BASE + "/balances", caseId)
                .exchange()
                .expectStatus().isOk();

        verify(queryBus).query(any(GetLoanBalancesQuery.class));
    }

    @Test
    void getBalances_shouldReturn500_onError() {
        UUID caseId = UUID.randomUUID();
        when(queryBus.<PaginationResponseLoanBalanceDTO>query(any(GetLoanBalancesQuery.class)))
                .thenReturn(Mono.error(new RuntimeException("fail")));

        webTestClient.get().uri(BASE + "/balances", caseId)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void getCalendar_shouldReturnTheRemainingBalance() {
        UUID caseId = UUID.randomUUID();
        when(queryBus.<List<AmortizationCalendarEntry>>query(any(GetAmortizationCalendarQuery.class)))
                .thenReturn(Mono.just(List.of(new AmortizationCalendarEntry(
                        1, LocalDate.of(2026, 2, 1),
                        new BigDecimal("88.00"), new BigDecimal("12.00"), new BigDecimal("1112.00")))));

        webTestClient.get().uri(BASE + "/calendar", caseId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].dueDate").isEqualTo("2026-02-01")
                .jsonPath("$[0].principal").isEqualTo(88.00)
                .jsonPath("$[0].interest").isEqualTo(12.00)
                .jsonPath("$[0].remainingBalance").isEqualTo(1112.00);

        verify(queryBus).query(any(GetAmortizationCalendarQuery.class));
    }

    // ---- GET /schedule ----

    @Test
    void getSchedule_shouldReturn200() {
        UUID caseId = UUID.randomUUID();
        when(queryBus.<PaginationResponseLoanRepaymentScheduleDTO>query(any(GetLoanScheduleQuery.class)))
                .thenReturn(Mono.just(new PaginationResponseLoanRepaymentScheduleDTO()));

        webTestClient.get().uri(BASE + "/schedule", caseId)
                .exchange()
                .expectStatus().isOk();

        verify(queryBus).query(any(GetLoanScheduleQuery.class));
    }

    // ---- GET /installment-plan ----

    @Test
    void getInstallmentPlan_shouldReturn200() {
        UUID caseId = UUID.randomUUID();
        when(queryBus.<PaginationResponseLoanInstallmentPlanDTO>query(any(GetLoanInstallmentPlanQuery.class)))
                .thenReturn(Mono.just(new PaginationResponseLoanInstallmentPlanDTO()));

        webTestClient.get().uri(BASE + "/installment-plan", caseId)
                .exchange()
                .expectStatus().isOk();

        verify(queryBus).query(any(GetLoanInstallmentPlanQuery.class));
    }

    // ---- GET /installment-records ----

    @Test
    void getInstallmentRecords_shouldReturn200() {
        UUID caseId = UUID.randomUUID();
        when(queryBus.<PaginationResponseLoanInstallmentRecordDTO>query(any(GetLoanInstallmentRecordsQuery.class)))
                .thenReturn(Mono.just(new PaginationResponseLoanInstallmentRecordDTO()));

        webTestClient.get().uri(BASE + "/installment-records", caseId)
                .exchange()
                .expectStatus().isOk();

        verify(queryBus).query(any(GetLoanInstallmentRecordsQuery.class));
    }

    // ---- GET /disbursements ----

    @Test
    void getDisbursements_shouldReturn200() {
        UUID caseId = UUID.randomUUID();
        when(queryBus.<PaginationResponseLoanDisbursementDTO>query(any(GetLoanDisbursementsQuery.class)))
                .thenReturn(Mono.just(new PaginationResponseLoanDisbursementDTO()));

        webTestClient.get().uri(BASE + "/disbursements", caseId)
                .exchange()
                .expectStatus().isOk();

        verify(queryBus).query(any(GetLoanDisbursementsQuery.class));
    }

    // ---- GET /disbursement-plan ----

    @Test
    void getDisbursementPlan_shouldReturn200() {
        UUID caseId = UUID.randomUUID();
        when(queryBus.<PaginationResponse>query(any(GetLoanDisbursementPlanQuery.class)))
                .thenReturn(Mono.just(new PaginationResponse()));

        webTestClient.get().uri(BASE + "/disbursement-plan", caseId)
                .exchange()
                .expectStatus().isOk();

        verify(queryBus).query(any(GetLoanDisbursementPlanQuery.class));
    }

    // ---- GET /repayments ----

    @Test
    void getRepayments_shouldReturn200() {
        UUID caseId = UUID.randomUUID();
        when(queryBus.<PaginationResponseLoanRepaymentRecordDTO>query(any(GetLoanRepaymentsQuery.class)))
                .thenReturn(Mono.just(new PaginationResponseLoanRepaymentRecordDTO()));

        webTestClient.get().uri(BASE + "/repayments", caseId)
                .exchange()
                .expectStatus().isOk();

        verify(queryBus).query(any(GetLoanRepaymentsQuery.class));
    }

    // ---- GET /rate-changes ----

    @Test
    void getRateChanges_shouldReturn200() {
        UUID caseId = UUID.randomUUID();
        when(queryBus.<PaginationResponseLoanRateChangeDTO>query(any(GetLoanRateChangesQuery.class)))
                .thenReturn(Mono.just(new PaginationResponseLoanRateChangeDTO()));

        webTestClient.get().uri(BASE + "/rate-changes", caseId)
                .exchange()
                .expectStatus().isOk();

        verify(queryBus).query(any(GetLoanRateChangesQuery.class));
    }

    // ---- GET /accruals ----

    @Test
    void getAccruals_shouldReturn200() {
        UUID caseId = UUID.randomUUID();
        when(queryBus.<PaginationResponseLoanAccrualDTO>query(any(GetLoanAccrualsQuery.class)))
                .thenReturn(Mono.just(new PaginationResponseLoanAccrualDTO()));

        webTestClient.get().uri(BASE + "/accruals", caseId)
                .exchange()
                .expectStatus().isOk();

        verify(queryBus).query(any(GetLoanAccrualsQuery.class));
    }

    // ---- GET /escrow ----

    @Test
    void getEscrow_shouldReturn200() {
        UUID caseId = UUID.randomUUID();
        when(queryBus.<PaginationResponse>query(any(GetLoanEscrowQuery.class)))
                .thenReturn(Mono.just(new PaginationResponse()));

        webTestClient.get().uri(BASE + "/escrow", caseId)
                .exchange()
                .expectStatus().isOk();

        verify(queryBus).query(any(GetLoanEscrowQuery.class));
    }

    // ---- GET /rebates ----

    @Test
    void getRebates_shouldReturn200() {
        UUID caseId = UUID.randomUUID();
        when(queryBus.<PaginationResponse>query(any(GetLoanRebatesQuery.class)))
                .thenReturn(Mono.just(new PaginationResponse()));

        webTestClient.get().uri(BASE + "/rebates", caseId)
                .exchange()
                .expectStatus().isOk();

        verify(queryBus).query(any(GetLoanRebatesQuery.class));
    }

    // ---- GET /restructurings ----

    @Test
    void getRestructurings_shouldReturn200() {
        UUID caseId = UUID.randomUUID();
        when(queryBus.<PaginationResponseLoanRestructuringDTO>query(any(GetLoanRestructuringsQuery.class)))
                .thenReturn(Mono.just(new PaginationResponseLoanRestructuringDTO()));

        webTestClient.get().uri(BASE + "/restructurings", caseId)
                .exchange()
                .expectStatus().isOk();

        verify(queryBus).query(any(GetLoanRestructuringsQuery.class));
    }

    // ---- GET /events ----

    @Test
    void getEvents_shouldReturn200() {
        UUID caseId = UUID.randomUUID();
        when(queryBus.<PaginationResponseLoanServicingEventDTO>query(any(GetLoanEventsQuery.class)))
                .thenReturn(Mono.just(new PaginationResponseLoanServicingEventDTO()));

        webTestClient.get().uri(BASE + "/events", caseId)
                .exchange()
                .expectStatus().isOk();

        verify(queryBus).query(any(GetLoanEventsQuery.class));
    }

    // ---- GET /notifications ----

    @Test
    void getNotifications_shouldReturn200() {
        UUID caseId = UUID.randomUUID();
        when(queryBus.<PaginationResponse>query(any(GetLoanNotificationsQuery.class)))
                .thenReturn(Mono.just(new PaginationResponse()));

        webTestClient.get().uri(BASE + "/notifications", caseId)
                .exchange()
                .expectStatus().isOk();

        verify(queryBus).query(any(GetLoanNotificationsQuery.class));
    }

    @Test
    void getNotifications_shouldReturn500_onError() {
        UUID caseId = UUID.randomUUID();
        when(queryBus.<PaginationResponse>query(any(GetLoanNotificationsQuery.class)))
                .thenReturn(Mono.error(new RuntimeException("fail")));

        webTestClient.get().uri(BASE + "/notifications", caseId)
                .exchange()
                .expectStatus().is5xxServerError();
    }
}
