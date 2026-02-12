package com.firefly.domain.lending.loan.servicing.core.loan.servicing.services.impl;

import org.fireflyframework.transactional.saga.core.SagaResult;
import org.fireflyframework.transactional.saga.engine.SagaEngine;
import org.fireflyframework.transactional.saga.engine.StepInputs;

import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.*;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.services.LoanServicingService;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.workflows.RegisterLoanServicingSaga;
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
                .forStep(RegisterLoanServicingSaga::registerLoanServicing, command.getLoanServicingCase())
                .forStep(RegisterLoanServicingSaga::registerLoanAccrual, command.getLoanAccrual())
                .forStep(RegisterLoanServicingSaga::registerLoanDisbursement, command.getLoanDisbursement())
                .forStep(RegisterLoanServicingSaga::registerLoanRateChange, command.getLoanRateChange())
                .forStep(RegisterLoanServicingSaga::registerLoanRepaymentRecord, command.getLoanRepaymentRecord())
                .forStep(RegisterLoanServicingSaga::registerLoanRepaymentSchedule, command.getLoanRepaymentSchedule())
                .forStep(RegisterLoanServicingSaga::registerLoanServicingEvent, command.getLoanServicingEvent())

                .build();

        return engine.execute(RegisterLoanServicingSaga.class, inputs);
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
