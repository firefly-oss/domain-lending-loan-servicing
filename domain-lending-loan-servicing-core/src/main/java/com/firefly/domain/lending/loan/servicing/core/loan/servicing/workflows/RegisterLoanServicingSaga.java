package com.firefly.domain.lending.loan.servicing.core.loan.servicing.workflows;

import com.firefly.common.cqrs.command.CommandBus;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanAccrualCommand;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanDisbursementCommand;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanRateChangeCommand;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanRepaymentRecordCommand;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanRepaymentScheduleCommand;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanServicingCaseCommand;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanServicingEventCommand;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanAccrualCommand;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanDisbursementCommand;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanRateChangeCommand;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanRepaymentRecordCommand;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanRepaymentScheduleCommand;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanServicingCommand;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanServicingEventCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import com.firefly.transactional.saga.annotations.Saga;
import com.firefly.transactional.saga.annotations.SagaStep;
import com.firefly.transactional.saga.annotations.StepEvent;
import com.firefly.transactional.saga.core.SagaContext;

import java.util.UUID;

import static com.firefly.domain.lending.loan.servicing.core.loan.utils.constants.GlobalConstants.CTX_LOAN_SERVICING_ID;
import static com.firefly.domain.lending.loan.servicing.core.loan.utils.constants.RegisterLoanServicingConstants.*;


@Saga(name = SAGA_REGISTER_LOAN_SERVICING)
@Service
public class RegisterLoanServicingSaga {

    private final CommandBus commandBus;

    @Autowired
    public RegisterLoanServicingSaga(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @SagaStep(id = STEP_REGISTER_LOAN_SERVICING, compensate = COMPENSATE_REMOVE_LOAN_SERVICING)
    @StepEvent(type = EVENT_LOAN_SERVICING_REGISTERED)
    public Mono<UUID> registerLoanServicing(RegisterLoanServicingCaseCommand cmd, SagaContext ctx) {
        return commandBus.send(cmd)
                .doOnNext(loanServicingId -> ctx.variables().put(CTX_LOAN_SERVICING_ID, loanServicingId));
    }

    public Mono<Void> removeLoanServicing(UUID loanApplicationId) {
        return commandBus.send(new RemoveLoanServicingCommand(loanApplicationId));
    }

    @SagaStep(id = STEP_REGISTER_LOAN_ACCRUAL, compensate = COMPENSATE_REMOVE_LOAN_ACCRUAL, dependsOn = STEP_REGISTER_LOAN_SERVICING)
    @StepEvent(type = EVENT_LOAN_ACCRUAL_REGISTERED)
    public Mono<UUID> registerLoanAccrual(RegisterLoanAccrualCommand cmd, SagaContext ctx) {
        return commandBus.send(cmd.withLoanServicingCaseId(ctx.getVariableAs(CTX_LOAN_SERVICING_ID, UUID.class)));
    }

    public Mono<Void> removeLoanAccrual(UUID loanAccrualId, SagaContext ctx) {
        return commandBus.send(new RemoveLoanAccrualCommand(ctx.getVariableAs(CTX_LOAN_SERVICING_ID, UUID.class), loanAccrualId));
    }

    @SagaStep(id = STEP_REGISTER_LOAN_DISBURSEMENT, compensate = COMPENSATE_REMOVE_LOAN_DISBURSEMENT, dependsOn = STEP_REGISTER_LOAN_SERVICING)
    @StepEvent(type = EVENT_LOAN_DISBURSEMENT_REGISTERED)
    public Mono<UUID> registerLoanDisbursement(RegisterLoanDisbursementCommand cmd, SagaContext ctx) {
        return commandBus.send(cmd.withLoanServicingCaseId(ctx.getVariableAs(CTX_LOAN_SERVICING_ID, UUID.class)));
    }

    public Mono<Void> removeLoanDisbursement(UUID loanDisbursementId, SagaContext ctx) {
        return commandBus.send(new RemoveLoanDisbursementCommand(ctx.getVariableAs(CTX_LOAN_SERVICING_ID, UUID.class), loanDisbursementId));
    }

    @SagaStep(id = STEP_REGISTER_LOAN_RATE_CHANGE, compensate = COMPENSATE_REMOVE_LOAN_RATE_CHANGE, dependsOn = STEP_REGISTER_LOAN_SERVICING)
    @StepEvent(type = EVENT_LOAN_RATE_CHANGE_REGISTERED)
    public Mono<UUID> registerLoanRateChange(RegisterLoanRateChangeCommand cmd, SagaContext ctx) {
        return commandBus.send(cmd.withLoanServicingCaseId(ctx.getVariableAs(CTX_LOAN_SERVICING_ID, UUID.class)));
    }

    public Mono<Void> removeLoanRateChange(UUID loanRateChangeId, SagaContext ctx) {
        return commandBus.send(new RemoveLoanRateChangeCommand(ctx.getVariableAs(CTX_LOAN_SERVICING_ID, UUID.class), loanRateChangeId));
    }

    @SagaStep(id = STEP_REGISTER_LOAN_REPAYMENT_RECORD, compensate = COMPENSATE_REMOVE_LOAN_REPAYMENT_RECORD, dependsOn = STEP_REGISTER_LOAN_SERVICING)
    @StepEvent(type = EVENT_LOAN_REPAYMENT_RECORD_REGISTERED)
    public Mono<UUID> registerLoanRepaymentRecord(RegisterLoanRepaymentRecordCommand cmd, SagaContext ctx) {
        return commandBus.send(cmd.withLoanServicingCaseId(ctx.getVariableAs(CTX_LOAN_SERVICING_ID, UUID.class)));
    }

    public Mono<Void> removeLoanRepaymentRecord(UUID loanRepaymentRecordId, SagaContext ctx) {
        return commandBus.send(new RemoveLoanRepaymentRecordCommand(ctx.getVariableAs(CTX_LOAN_SERVICING_ID, UUID.class), loanRepaymentRecordId));
    }

    @SagaStep(id = STEP_REGISTER_LOAN_REPAYMENT_SCHEDULE, compensate = COMPENSATE_REMOVE_LOAN_REPAYMENT_SCHEDULE, dependsOn = STEP_REGISTER_LOAN_SERVICING)
    @StepEvent(type = EVENT_LOAN_REPAYMENT_SCHEDULE_REGISTERED)
    public Mono<UUID> registerLoanRepaymentSchedule(RegisterLoanRepaymentScheduleCommand cmd, SagaContext ctx) {
        return commandBus.send(cmd.withLoanServicingCaseId(ctx.getVariableAs(CTX_LOAN_SERVICING_ID, UUID.class)));
    }

    public Mono<Void> removeLoanRepaymentSchedule(UUID loanRepaymentScheduleId, SagaContext ctx) {
        return commandBus.send(new RemoveLoanRepaymentScheduleCommand(ctx.getVariableAs(CTX_LOAN_SERVICING_ID, UUID.class), loanRepaymentScheduleId));
    }

    @SagaStep(id = STEP_REGISTER_LOAN_SERVICING_EVENT, compensate = COMPENSATE_REMOVE_LOAN_SERVICING_EVENT, dependsOn = STEP_REGISTER_LOAN_SERVICING)
    @StepEvent(type = EVENT_LOAN_SERVICING_EVENT_REGISTERED)
    public Mono<UUID> registerLoanServicingEvent(RegisterLoanServicingEventCommand cmd, SagaContext ctx) {
        return commandBus.send(cmd.withLoanServicingCaseId(ctx.getVariableAs(CTX_LOAN_SERVICING_ID, UUID.class)));
    }

    public Mono<Void> removeLoanServicingEvent(UUID loanServicingEventId, SagaContext ctx) {
        return commandBus.send(new RemoveLoanServicingEventCommand(ctx.getVariableAs(CTX_LOAN_SERVICING_ID, UUID.class), loanServicingEventId));
    }

}
