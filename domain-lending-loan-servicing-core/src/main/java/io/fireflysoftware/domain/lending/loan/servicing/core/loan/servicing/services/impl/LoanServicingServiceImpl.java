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


package io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.services.impl;

import org.fireflyframework.orchestration.saga.engine.SagaResult;
import org.fireflyframework.orchestration.saga.engine.SagaEngine;
import org.fireflyframework.orchestration.saga.engine.StepInputs;

import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.*;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.services.LoanServicingService;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.workflows.RegisterLoanServicingSaga;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class LoanServicingServiceImpl implements LoanServicingService {

    private final SagaEngine engine;

    @Autowired
    public LoanServicingServiceImpl(SagaEngine engine){
        this.engine=engine;
    }

    @Override
    public Mono<SagaResult> submitLoanCase(SubmitLoanCaseCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId("registerLoanServicing", command.getLoanServicingCase())
                .forStepId("registerLoanAccrual", command.getLoanAccrual())
                .forStepId("registerLoanDisbursement", command.getLoanDisbursement())
                .forStepId("registerLoanRateChange", command.getLoanRateChange())
                .forStepId("registerLoanRepaymentRecord", command.getLoanRepaymentRecord())
                .forStepId("registerLoanRepaymentSchedule", command.getLoanRepaymentSchedule())
                .forStepId("registerLoanServicingEvent", command.getLoanServicingEvent())

                .build();

        return engine.execute("RegisterLoanServicingSaga", inputs);
    }

    @Override
    public Mono<SagaResult> disburse(String loanId, DisburseCommand command) {
        // TODO: Implement loan disbursement logic
        return Mono.empty();
    }

    @Override
    public Mono<SagaResult> generateSchedule(String loanId, GenerateScheduleCommand command) {
        // TODO: Implement amortization schedule generation logic
        return Mono.empty();
    }

    @Override
    public Mono<SagaResult> applyRepayment(String loanId, ApplyRepaymentCommand command) {
        // TODO: Implement repayment application logic
        return Mono.empty();
    }

    @Override
    public Mono<SagaResult> applyPrepayment(String loanId, ApplyPrepaymentCommand command) {
        // TODO: Implement prepayment logic
        return Mono.empty();
    }

    @Override
    public Mono<SagaResult> accrueInterest(String loanId, AccrueInterestCommand command) {
        // TODO: Implement interest accrual logic
        return Mono.empty();
    }

    @Override
    public Mono<SagaResult> capitalizeInterest(String loanId, CapitalizeInterestCommand command) {
        // TODO: Implement interest capitalization logic
        return Mono.empty();
    }

    @Override
    public Mono<SagaResult> generatePenalties(String loanId, GeneratePenaltiesCommand command) {
        // TODO: Implement penalty generation logic
        return Mono.empty();
    }

    @Override
    public Mono<SagaResult> setFixedRate(String loanId, SetFixedRateCommand command) {
        // TODO: Implement fixed rate setting logic
        return Mono.empty();
    }

    @Override
    public Mono<SagaResult> changeReferenceIndex(String loanId, ChangeReferenceIndexCommand command) {
        // TODO: Implement reference index change logic
        return Mono.empty();
    }

    @Override
    public Mono<SagaResult> grantPaymentHoliday(String loanId, GrantPaymentHolidayCommand command) {
        // TODO: Implement payment holiday granting logic
        return Mono.empty();
    }

    @Override
    public Mono<SagaResult> endPaymentHoliday(String loanId, EndPaymentHolidayCommand command) {
        // TODO: Implement payment holiday ending logic
        return Mono.empty();
    }

    @Override
    public Mono<SagaResult> applyRestructure(String loanId, ApplyRestructureCommand command) {
        // TODO: Implement loan restructuring logic
        return Mono.empty();
    }

    @Override
    public Mono<SagaResult> markInArrears(String loanId, MarkInArrearsCommand command) {
        // TODO: Implement arrears marking logic
        return Mono.empty();
    }

    @Override
    public Mono<SagaResult> normalizeArrears(String loanId, NormalizeArrearsCommand command) {
        // TODO: Implement arrears normalization logic
        return Mono.empty();
    }

    @Override
    public Mono<SagaResult> chargeOff(String loanId, ChargeOffCommand command) {
        // TODO: Implement charge-off logic
        return Mono.empty();
    }

    @Override
    public Mono<SagaResult> writeBack(String loanId, WriteBackCommand command) {
        // TODO: Implement write-back logic
        return Mono.empty();
    }

    @Override
    public Mono<SagaResult> closeLoan(String loanId, CloseLoanCommand command) {
        // TODO: Implement loan closure logic
        return Mono.empty();
    }

    @Override
    public Mono<Object> getLoanDetails(String loanId) {
        // TODO: Implement loan details retrieval logic
        return Mono.just(new Object());
    }

}
