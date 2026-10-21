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

import io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.EndOfDayResult;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.LoanEndOfDayService;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.*;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.services.LoanServicingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@Tag(name = "Loan Servicing", description = "CQ queries and registration for Loan Servicing")
public class LoanServicingController {

    private final LoanServicingService loanServicingService;
    private final LoanEndOfDayService loanEndOfDayService;

    @Operation(summary = "Run end of day", description = "Accrue interest for open loans and post it to the general ledger.")
    @PostMapping("/end-of-day")
    public Mono<ResponseEntity<EndOfDayResult>> endOfDay(@RequestBody EndOfDayRequest request) {
        return loanEndOfDayService.run(request.businessDate()).map(ResponseEntity::ok);
    }

    @Operation(summary = "Submit loan case", description = "Submit a new loan case.")
    @PostMapping
    public Mono<ResponseEntity<Object>> submitApplication(@Valid @RequestBody SubmitLoanCaseCommand command) {
        return loanServicingService.submitLoanCase(command)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Disburse loan", description = "Disburse to target account on value date; update principal.")
    @PostMapping("/{loanId}/disburse")
    public Mono<ResponseEntity<Object>> disburse(@PathVariable String loanId, @Valid @RequestBody DisburseCommand command) {
        return loanServicingService.disburse(loanId, command)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Generate amortization schedule", description = "Generate amortization schedule (method: French/German, etc.).")
    @PostMapping("/{loanId}/schedule")
    public Mono<ResponseEntity<Object>> generateSchedule(@PathVariable String loanId, @Valid @RequestBody GenerateScheduleCommand command) {
        return loanServicingService.generateSchedule(loanId, command)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Apply repayment", description = "Apply repayment (interest→penalties→fees→principal order).")
    @PostMapping("/{loanId}/repayments")
    public Mono<ResponseEntity<Object>> applyRepayment(@PathVariable String loanId, @Valid @RequestBody ApplyRepaymentCommand command) {
        return loanServicingService.applyRepayment(loanId, command)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Apply early principal reduction", description = "Apply early principal reduction; recalc or shorten per policy.")
    @PostMapping("/{loanId}/prepayment")
    public Mono<ResponseEntity<Object>> applyPrepayment(@PathVariable String loanId, @Valid @RequestBody ApplyPrepaymentCommand command) {
        return loanServicingService.applyPrepayment(loanId, command)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Accrue interest", description = "Accrue interest for a period.")
    @PostMapping("/{loanId}/interest/accrue")
    public Mono<ResponseEntity<Object>> accrueInterest(@PathVariable String loanId, @Valid @RequestBody AccrueInterestCommand command) {
        return loanServicingService.accrueInterest(loanId, command)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Capitalize interest", description = "Capitalize past-due interest into principal.")
    @PostMapping("/{loanId}/interest/capitalize")
    public Mono<ResponseEntity<Object>> capitalizeInterest(@PathVariable String loanId, @Valid @RequestBody CapitalizeInterestCommand command) {
        return loanServicingService.capitalizeInterest(loanId, command)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Generate penalties", description = "Generate late fees/penalties for specified period.")
    @PostMapping("/{loanId}/penalties")
    public Mono<ResponseEntity<Object>> generatePenalties(@PathVariable String loanId, @Valid @RequestBody GeneratePenaltiesCommand command) {
        return loanServicingService.generatePenalties(loanId, command)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Set new fixed rate", description = "Set new fixed rate effective from date.")
    @PostMapping("/{loanId}/rate")
    public Mono<ResponseEntity<Object>> setFixedRate(@PathVariable String loanId, @Valid @RequestBody SetFixedRateCommand command) {
        return loanServicingService.setFixedRate(loanId, command)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Change reference index", description = "Change reference index & spread for variable rate.")
    @PostMapping("/{loanId}/index")
    public Mono<ResponseEntity<Object>> changeReferenceIndex(@PathVariable String loanId, @Valid @RequestBody ChangeReferenceIndexCommand command) {
        return loanServicingService.changeReferenceIndex(loanId, command)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Grant payment holiday", description = "Grant payment holiday and adjust schedule/accrual.")
    @PostMapping("/{loanId}/payment-holiday/grant")
    public Mono<ResponseEntity<Object>> grantPaymentHoliday(@PathVariable String loanId, @Valid @RequestBody GrantPaymentHolidayCommand command) {
        return loanServicingService.grantPaymentHoliday(loanId, command)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "End payment holiday", description = "End holiday and resume normal accrual.")
    @PostMapping("/{loanId}/payment-holiday/end")
    public Mono<ResponseEntity<Object>> endPaymentHoliday(@PathVariable String loanId, @Valid @RequestBody EndPaymentHolidayCommand command) {
        return loanServicingService.endPaymentHoliday(loanId, command)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Apply restructuring", description = "Apply restructuring (tenor, rate, write-offs) with new schedule.")
    @PostMapping("/{loanId}/restructure")
    public Mono<ResponseEntity<Object>> applyRestructure(@PathVariable String loanId, @Valid @RequestBody ApplyRestructureCommand command) {
        return loanServicingService.applyRestructure(loanId, command)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Mark loan in arrears", description = "Flag loan in arrears as of a business date.")
    @PostMapping("/{loanId}/arrears/mark")
    public Mono<ResponseEntity<Object>> markInArrears(@PathVariable String loanId, @Valid @RequestBody MarkInArrearsCommand command) {
        return loanServicingService.markInArrears(loanId, command)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Normalize arrears", description = "Return loan to current status.")
    @PostMapping("/{loanId}/arrears/normalize")
    public Mono<ResponseEntity<Object>> normalizeArrears(@PathVariable String loanId, @Valid @RequestBody NormalizeArrearsCommand command) {
        return loanServicingService.normalizeArrears(loanId, command)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Charge-off loan", description = "Charge-off to losses (meets delinquency policy).")
    @PostMapping("/{loanId}/charge-off")
    public Mono<ResponseEntity<Object>> chargeOff(@PathVariable String loanId, @Valid @RequestBody ChargeOffCommand command) {
        return loanServicingService.chargeOff(loanId, command)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Write back charged-off amount", description = "Write back previously charged-off amount.")
    @PostMapping("/{loanId}/write-back")
    public Mono<ResponseEntity<Object>> writeBack(@PathVariable String loanId, @Valid @RequestBody WriteBackCommand command) {
        return loanServicingService.writeBack(loanId, command)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Close loan", description = "Close loan with zero exposure and no pending installments.")
    @PostMapping("/{loanId}/close")
    public Mono<ResponseEntity<Object>> closeLoan(@PathVariable String loanId, @Valid @RequestBody CloseLoanCommand command) {
        return loanServicingService.closeLoan(loanId, command)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Retrieve loan details", description = "Retrieve loan details and schedule (projection).")
    @GetMapping("/{loanId}")
    public Mono<ResponseEntity<Object>> getLoanDetails(@PathVariable String loanId) {
        return loanServicingService.getLoanDetails(loanId)
                .map(ResponseEntity::ok);
    }

    public record EndOfDayRequest(LocalDate businessDate) {
    }

}
