package com.firefly.domain.lending.loan.servicing.web.controller;

import com.firefly.core.lending.servicing.sdk.model.*;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.query.QueryBus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/loan-servicing/cases/{caseId}")
@RequiredArgsConstructor
@Tag(name = "Loan Servicing Queries", description = "Query endpoints for loan servicing case data")
public class LoanServicingQueryController {

    private final QueryBus queryBus;

    @GetMapping("/balances")
    @Operation(summary = "Get loan balances", description = "Retrieve all balances for a loan servicing case.")
    @ApiResponse(responseCode = "200", description = "Balances retrieved successfully")
    public Mono<ResponseEntity<PaginationResponseLoanBalanceDTO>> getLoanBalances(
            @Parameter(description = "Loan case identifier") @PathVariable("caseId") UUID caseId) {
        log.debug("GET /api/v1/loan-servicing/cases/{}/balances", caseId);
        return queryBus.<PaginationResponseLoanBalanceDTO>query(
                        GetLoanBalancesQuery.builder().loanCaseId(caseId).build())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/schedule")
    @Operation(summary = "Get loan schedule", description = "Retrieve the repayment schedule for a loan servicing case.")
    @ApiResponse(responseCode = "200", description = "Schedule retrieved successfully")
    public Mono<ResponseEntity<PaginationResponseLoanRepaymentScheduleDTO>> getLoanSchedule(
            @Parameter(description = "Loan case identifier") @PathVariable("caseId") UUID caseId) {
        log.debug("GET /api/v1/loan-servicing/cases/{}/schedule", caseId);
        return queryBus.<PaginationResponseLoanRepaymentScheduleDTO>query(
                        GetLoanScheduleQuery.builder().loanCaseId(caseId).build())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/installment-plan")
    @Operation(summary = "Get loan installment plan", description = "Retrieve the installment plan for a loan servicing case.")
    @ApiResponse(responseCode = "200", description = "Installment plan retrieved successfully")
    public Mono<ResponseEntity<PaginationResponseLoanInstallmentPlanDTO>> getLoanInstallmentPlan(
            @Parameter(description = "Loan case identifier") @PathVariable("caseId") UUID caseId) {
        log.debug("GET /api/v1/loan-servicing/cases/{}/installment-plan", caseId);
        return queryBus.<PaginationResponseLoanInstallmentPlanDTO>query(
                        GetLoanInstallmentPlanQuery.builder().loanCaseId(caseId).build())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/installment-records")
    @Operation(summary = "Get loan installment records", description = "Retrieve installment records for a loan servicing case.")
    @ApiResponse(responseCode = "200", description = "Installment records retrieved successfully")
    public Mono<ResponseEntity<PaginationResponseLoanInstallmentRecordDTO>> getLoanInstallmentRecords(
            @Parameter(description = "Loan case identifier") @PathVariable("caseId") UUID caseId) {
        log.debug("GET /api/v1/loan-servicing/cases/{}/installment-records", caseId);
        return queryBus.<PaginationResponseLoanInstallmentRecordDTO>query(
                        GetLoanInstallmentRecordsQuery.builder().loanCaseId(caseId).build())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/disbursements")
    @Operation(summary = "Get loan disbursements", description = "Retrieve all disbursements for a loan servicing case.")
    @ApiResponse(responseCode = "200", description = "Disbursements retrieved successfully")
    public Mono<ResponseEntity<PaginationResponseLoanDisbursementDTO>> getLoanDisbursements(
            @Parameter(description = "Loan case identifier") @PathVariable("caseId") UUID caseId) {
        log.debug("GET /api/v1/loan-servicing/cases/{}/disbursements", caseId);
        return queryBus.<PaginationResponseLoanDisbursementDTO>query(
                        GetLoanDisbursementsQuery.builder().loanCaseId(caseId).build())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/disbursement-plan")
    @Operation(summary = "Get loan disbursement plan", description = "Retrieve the disbursement plan for a loan servicing case.")
    @ApiResponse(responseCode = "200", description = "Disbursement plan retrieved successfully")
    public Mono<ResponseEntity<PaginationResponse>> getLoanDisbursementPlan(
            @Parameter(description = "Loan case identifier") @PathVariable("caseId") UUID caseId) {
        log.debug("GET /api/v1/loan-servicing/cases/{}/disbursement-plan", caseId);
        return queryBus.<PaginationResponse>query(
                        GetLoanDisbursementPlanQuery.builder().loanCaseId(caseId).build())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/repayments")
    @Operation(summary = "Get loan repayments", description = "Retrieve all repayment records for a loan servicing case.")
    @ApiResponse(responseCode = "200", description = "Repayments retrieved successfully")
    public Mono<ResponseEntity<PaginationResponseLoanRepaymentRecordDTO>> getLoanRepayments(
            @Parameter(description = "Loan case identifier") @PathVariable("caseId") UUID caseId) {
        log.debug("GET /api/v1/loan-servicing/cases/{}/repayments", caseId);
        return queryBus.<PaginationResponseLoanRepaymentRecordDTO>query(
                        GetLoanRepaymentsQuery.builder().loanCaseId(caseId).build())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/rate-changes")
    @Operation(summary = "Get loan rate changes", description = "Retrieve all rate changes for a loan servicing case.")
    @ApiResponse(responseCode = "200", description = "Rate changes retrieved successfully")
    public Mono<ResponseEntity<PaginationResponseLoanRateChangeDTO>> getLoanRateChanges(
            @Parameter(description = "Loan case identifier") @PathVariable("caseId") UUID caseId) {
        log.debug("GET /api/v1/loan-servicing/cases/{}/rate-changes", caseId);
        return queryBus.<PaginationResponseLoanRateChangeDTO>query(
                        GetLoanRateChangesQuery.builder().loanCaseId(caseId).build())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/accruals")
    @Operation(summary = "Get loan accruals", description = "Retrieve all accruals for a loan servicing case.")
    @ApiResponse(responseCode = "200", description = "Accruals retrieved successfully")
    public Mono<ResponseEntity<PaginationResponseLoanAccrualDTO>> getLoanAccruals(
            @Parameter(description = "Loan case identifier") @PathVariable("caseId") UUID caseId) {
        log.debug("GET /api/v1/loan-servicing/cases/{}/accruals", caseId);
        return queryBus.<PaginationResponseLoanAccrualDTO>query(
                        GetLoanAccrualsQuery.builder().loanCaseId(caseId).build())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/escrow")
    @Operation(summary = "Get loan escrow", description = "Retrieve escrow information for a loan servicing case.")
    @ApiResponse(responseCode = "200", description = "Escrow data retrieved successfully")
    public Mono<ResponseEntity<PaginationResponse>> getLoanEscrow(
            @Parameter(description = "Loan case identifier") @PathVariable("caseId") UUID caseId) {
        log.debug("GET /api/v1/loan-servicing/cases/{}/escrow", caseId);
        return queryBus.<PaginationResponse>query(
                        GetLoanEscrowQuery.builder().loanCaseId(caseId).build())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/rebates")
    @Operation(summary = "Get loan rebates", description = "Retrieve all rebates for a loan servicing case.")
    @ApiResponse(responseCode = "200", description = "Rebates retrieved successfully")
    public Mono<ResponseEntity<PaginationResponse>> getLoanRebates(
            @Parameter(description = "Loan case identifier") @PathVariable("caseId") UUID caseId) {
        log.debug("GET /api/v1/loan-servicing/cases/{}/rebates", caseId);
        return queryBus.<PaginationResponse>query(
                        GetLoanRebatesQuery.builder().loanCaseId(caseId).build())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/restructurings")
    @Operation(summary = "Get loan restructurings", description = "Retrieve all restructurings for a loan servicing case.")
    @ApiResponse(responseCode = "200", description = "Restructurings retrieved successfully")
    public Mono<ResponseEntity<PaginationResponseLoanRestructuringDTO>> getLoanRestructurings(
            @Parameter(description = "Loan case identifier") @PathVariable("caseId") UUID caseId) {
        log.debug("GET /api/v1/loan-servicing/cases/{}/restructurings", caseId);
        return queryBus.<PaginationResponseLoanRestructuringDTO>query(
                        GetLoanRestructuringsQuery.builder().loanCaseId(caseId).build())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/events")
    @Operation(summary = "Get loan events", description = "Retrieve all servicing events for a loan servicing case.")
    @ApiResponse(responseCode = "200", description = "Events retrieved successfully")
    public Mono<ResponseEntity<PaginationResponseLoanServicingEventDTO>> getLoanEvents(
            @Parameter(description = "Loan case identifier") @PathVariable("caseId") UUID caseId) {
        log.debug("GET /api/v1/loan-servicing/cases/{}/events", caseId);
        return queryBus.<PaginationResponseLoanServicingEventDTO>query(
                        GetLoanEventsQuery.builder().loanCaseId(caseId).build())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/notifications")
    @Operation(summary = "Get loan notifications", description = "Retrieve all notifications for a loan servicing case.")
    @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully")
    public Mono<ResponseEntity<PaginationResponse>> getLoanNotifications(
            @Parameter(description = "Loan case identifier") @PathVariable("caseId") UUID caseId) {
        log.debug("GET /api/v1/loan-servicing/cases/{}/notifications", caseId);
        return queryBus.<PaginationResponse>query(
                        GetLoanNotificationsQuery.builder().loanCaseId(caseId).build())
                .map(ResponseEntity::ok);
    }
}
