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


package io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.workflows;

import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.fireflyframework.orchestration.core.context.ExecutionContext;

import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanAccrualCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanDisbursementCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanRateChangeCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanRepaymentRecordCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanRepaymentScheduleCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanServicingCaseCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanServicingEventCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanAccrualCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanDisbursementCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanRateChangeCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanRepaymentRecordCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanRepaymentScheduleCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanServicingCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanServicingEventCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static io.fireflysoftware.domain.lending.loan.servicing.core.loan.utils.constants.GlobalConstants.CTX_LOAN_SERVICING_ID;
import static io.fireflysoftware.domain.lending.loan.servicing.core.loan.utils.constants.RegisterLoanServicingConstants.*;


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
    public Mono<UUID> registerLoanServicing(RegisterLoanServicingCaseCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd)
                .doOnNext(loanServicingId -> ctx.putVariable(CTX_LOAN_SERVICING_ID, loanServicingId));
    }

    public Mono<Void> removeLoanServicing(UUID loanApplicationId) {
        return commandBus.send(new RemoveLoanServicingCommand(loanApplicationId));
    }

    @SagaStep(id = STEP_REGISTER_LOAN_ACCRUAL, compensate = COMPENSATE_REMOVE_LOAN_ACCRUAL, dependsOn = STEP_REGISTER_LOAN_SERVICING)
    @StepEvent(type = EVENT_LOAN_ACCRUAL_REGISTERED)
    public Mono<UUID> registerLoanAccrual(RegisterLoanAccrualCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd.withLoanServicingCaseId(ctx.getVariableAs(CTX_LOAN_SERVICING_ID, UUID.class)));
    }

    public Mono<Void> removeLoanAccrual(UUID loanAccrualId, ExecutionContext ctx) {
        return commandBus.send(new RemoveLoanAccrualCommand(ctx.getVariableAs(CTX_LOAN_SERVICING_ID, UUID.class), loanAccrualId));
    }

    @SagaStep(id = STEP_REGISTER_LOAN_DISBURSEMENT, compensate = COMPENSATE_REMOVE_LOAN_DISBURSEMENT, dependsOn = STEP_REGISTER_LOAN_SERVICING)
    @StepEvent(type = EVENT_LOAN_DISBURSEMENT_REGISTERED)
    public Mono<UUID> registerLoanDisbursement(RegisterLoanDisbursementCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd.withLoanServicingCaseId(ctx.getVariableAs(CTX_LOAN_SERVICING_ID, UUID.class)));
    }

    public Mono<Void> removeLoanDisbursement(UUID loanDisbursementId, ExecutionContext ctx) {
        return commandBus.send(new RemoveLoanDisbursementCommand(ctx.getVariableAs(CTX_LOAN_SERVICING_ID, UUID.class), loanDisbursementId));
    }

    @SagaStep(id = STEP_REGISTER_LOAN_RATE_CHANGE, compensate = COMPENSATE_REMOVE_LOAN_RATE_CHANGE, dependsOn = STEP_REGISTER_LOAN_SERVICING)
    @StepEvent(type = EVENT_LOAN_RATE_CHANGE_REGISTERED)
    public Mono<UUID> registerLoanRateChange(RegisterLoanRateChangeCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd.withLoanServicingCaseId(ctx.getVariableAs(CTX_LOAN_SERVICING_ID, UUID.class)));
    }

    public Mono<Void> removeLoanRateChange(UUID loanRateChangeId, ExecutionContext ctx) {
        return commandBus.send(new RemoveLoanRateChangeCommand(ctx.getVariableAs(CTX_LOAN_SERVICING_ID, UUID.class), loanRateChangeId));
    }

    @SagaStep(id = STEP_REGISTER_LOAN_REPAYMENT_RECORD, compensate = COMPENSATE_REMOVE_LOAN_REPAYMENT_RECORD, dependsOn = STEP_REGISTER_LOAN_SERVICING)
    @StepEvent(type = EVENT_LOAN_REPAYMENT_RECORD_REGISTERED)
    public Mono<UUID> registerLoanRepaymentRecord(RegisterLoanRepaymentRecordCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd.withLoanServicingCaseId(ctx.getVariableAs(CTX_LOAN_SERVICING_ID, UUID.class)));
    }

    public Mono<Void> removeLoanRepaymentRecord(UUID loanRepaymentRecordId, ExecutionContext ctx) {
        return commandBus.send(new RemoveLoanRepaymentRecordCommand(ctx.getVariableAs(CTX_LOAN_SERVICING_ID, UUID.class), loanRepaymentRecordId));
    }

    @SagaStep(id = STEP_REGISTER_LOAN_REPAYMENT_SCHEDULE, compensate = COMPENSATE_REMOVE_LOAN_REPAYMENT_SCHEDULE, dependsOn = STEP_REGISTER_LOAN_SERVICING)
    @StepEvent(type = EVENT_LOAN_REPAYMENT_SCHEDULE_REGISTERED)
    public Mono<UUID> registerLoanRepaymentSchedule(RegisterLoanRepaymentScheduleCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd.withLoanServicingCaseId(ctx.getVariableAs(CTX_LOAN_SERVICING_ID, UUID.class)));
    }

    public Mono<Void> removeLoanRepaymentSchedule(UUID loanRepaymentScheduleId, ExecutionContext ctx) {
        return commandBus.send(new RemoveLoanRepaymentScheduleCommand(ctx.getVariableAs(CTX_LOAN_SERVICING_ID, UUID.class), loanRepaymentScheduleId));
    }

    @SagaStep(id = STEP_REGISTER_LOAN_SERVICING_EVENT, compensate = COMPENSATE_REMOVE_LOAN_SERVICING_EVENT, dependsOn = STEP_REGISTER_LOAN_SERVICING)
    @StepEvent(type = EVENT_LOAN_SERVICING_EVENT_REGISTERED)
    public Mono<UUID> registerLoanServicingEvent(RegisterLoanServicingEventCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd.withLoanServicingCaseId(ctx.getVariableAs(CTX_LOAN_SERVICING_ID, UUID.class)));
    }

    public Mono<Void> removeLoanServicingEvent(UUID loanServicingEventId, ExecutionContext ctx) {
        return commandBus.send(new RemoveLoanServicingEventCommand(ctx.getVariableAs(CTX_LOAN_SERVICING_ID, UUID.class), loanServicingEventId));
    }

}
