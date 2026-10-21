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

import io.fireflysoftware.core.lending.servicing.sdk.model.LoanServicingCaseDTO;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.ApplyLoanServicingStatusCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanServicingCaseQuery;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanRestructuringCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanRestructuringCommand;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

@Saga(name = "ApplyRestructureSaga")
@Service
public class ApplyRestructureSaga {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    public ApplyRestructureSaga(CommandBus commandBus, QueryBus queryBus) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
    }

    @SagaStep(id = "retrieveCase")
    @StepEvent(type = "loanServicing.case.retrieved")
    public Mono<LoanServicingCaseDTO> retrieveCase(GetLoanServicingCaseQuery query, ExecutionContext ctx) {
        return queryBus.<LoanServicingCaseDTO>query(query)
                .switchIfEmpty(Mono.error(new IllegalStateException("Loan servicing case was not found")))
                .doOnNext(record -> {
                    LoanServicingCaseDTO snapshot = new LoanServicingCaseDTO();
                    BeanUtils.copyProperties(record, snapshot);
                    ctx.putVariable("snapshot", snapshot);
                    ctx.putVariable("case", record);
                });
    }

    @SagaStep(id = "registerRestructuring", dependsOn = "retrieveCase", compensate = "removeRestructuring")
    @StepEvent(type = "loanRestructuring.registered")
    public Mono<UUID> registerRestructuring(RegisterLoanRestructuringCommand command, ExecutionContext ctx) {
        LoanServicingCaseDTO loan = ctx.getVariableAs("case", LoanServicingCaseDTO.class);
        if (loan == null) {
            return Mono.error(new IllegalStateException("Loan servicing case was not found"));
        }
        fillPreviousTerms(command, loan);
        if (command.getNewPrincipalAmount() == null && command.getNewInterestRate() == null && command.getNewLoanTerm() == null) {
            return Mono.error(new IllegalArgumentException("New restructuring terms are required"));
        }
        ctx.putVariable("caseId", command.getLoanServicingCaseId());
        return commandBus.<UUID>send(command).doOnNext(id -> ctx.putVariable("restructuringId", id));
    }

    public Mono<Void> removeRestructuring(UUID restructuringId, ExecutionContext ctx) {
        UUID caseId = ctx.getVariableAs("caseId", UUID.class);
        UUID id = restructuringId == null ? (UUID) ctx.getVariable("restructuringId") : restructuringId;
        if (id == null || caseId == null) {
            return Mono.empty();
        }
        return commandBus.send(new RemoveLoanRestructuringCommand(caseId, id));
    }

    @SagaStep(id = "applyNewTerms", dependsOn = "registerRestructuring", compensate = "restoreTerms")
    @StepEvent(type = "loanServicing.restructured")
    public Mono<LoanServicingCaseDTO> applyNewTerms(RegisterLoanRestructuringCommand command, ExecutionContext ctx) {
        LoanServicingCaseDTO current = ctx.getVariableAs("case", LoanServicingCaseDTO.class);
        if (command.getNewPrincipalAmount() != null) {
            current.setPrincipalAmount(command.getNewPrincipalAmount());
        }
        if (command.getNewInterestRate() != null) {
            current.setInterestRate(command.getNewInterestRate());
        }
        if (command.getNewLoanTerm() != null) {
            current.setLoanTerm(command.getNewLoanTerm());
        }
        if (command.getNewMaturityDate() != null) {
            current.setMaturityDate(command.getNewMaturityDate());
        }
        if (command.getNewAmortizationMethod() != null) {
            current.setAmortizationMethod(LoanServicingCaseDTO.AmortizationMethodEnum.fromValue(
                    command.getNewAmortizationMethod().getValue()));
        }
        if (command.getNewPaymentFrequency() != null) {
            current.setPaymentFrequency(LoanServicingCaseDTO.PaymentFrequencyEnum.fromValue(
                    command.getNewPaymentFrequency().getValue()));
        }
        if (command.getNewInterestCalculationMethod() != null) {
            current.setInterestCalculationMethod(LoanServicingCaseDTO.InterestCalculationMethodEnum.fromValue(
                    command.getNewInterestCalculationMethod().getValue()));
        }
        if (command.getNewCompoundingFrequency() != null) {
            current.setCompoundingFrequency(LoanServicingCaseDTO.CompoundingFrequencyEnum.fromValue(
                    command.getNewCompoundingFrequency().getValue()));
        }
        if (command.getNewDayCountConvention() != null) {
            current.setDayCountConvention(LoanServicingCaseDTO.DayCountConventionEnum.fromValue(
                    command.getNewDayCountConvention().getValue()));
        }
        current.setServicingStatus(LoanServicingCaseDTO.ServicingStatusEnum.RESTRUCTURED);
        if (command.getReason() != null) {
            current.setRemarks(command.getReason());
        }
        return commandBus.send(new ApplyLoanServicingStatusCommand(current.getLoanServicingCaseId(), current));
    }

    public Mono<Void> restoreTerms(LoanServicingCaseDTO updated, ExecutionContext ctx) {
        LoanServicingCaseDTO snapshot = ctx.getVariableAs("snapshot", LoanServicingCaseDTO.class);
        if (snapshot == null || snapshot.getLoanServicingCaseId() == null) {
            return Mono.empty();
        }
        return commandBus.send(new ApplyLoanServicingStatusCommand(snapshot.getLoanServicingCaseId(), snapshot)).then();
    }

    private static void fillPreviousTerms(RegisterLoanRestructuringCommand command, LoanServicingCaseDTO loan) {
        command.withLoanServicingCaseId(loan.getLoanServicingCaseId());
        if (command.getOldPrincipalAmount() == null) {
            command.setOldPrincipalAmount(loan.getPrincipalAmount());
        }
        if (command.getOldInterestRate() == null) {
            command.setOldInterestRate(loan.getInterestRate());
        }
        if (command.getOldLoanTerm() == null) {
            command.setOldLoanTerm(loan.getLoanTerm());
        }
        if (command.getOldMaturityDate() == null) {
            command.setOldMaturityDate(loan.getMaturityDate());
        }
        if (command.getOldAmortizationMethod() == null && loan.getAmortizationMethod() != null) {
            command.setOldAmortizationMethod(RegisterLoanRestructuringCommand.OldAmortizationMethodEnum.fromValue(
                    loan.getAmortizationMethod().getValue()));
        }
        if (command.getOldPaymentFrequency() == null && loan.getPaymentFrequency() != null) {
            command.setOldPaymentFrequency(RegisterLoanRestructuringCommand.OldPaymentFrequencyEnum.fromValue(
                    loan.getPaymentFrequency().getValue()));
        }
        if (command.getOldInterestCalculationMethod() == null && loan.getInterestCalculationMethod() != null) {
            command.setOldInterestCalculationMethod(RegisterLoanRestructuringCommand.OldInterestCalculationMethodEnum.fromValue(
                    loan.getInterestCalculationMethod().getValue()));
        }
        if (command.getOldCompoundingFrequency() == null && loan.getCompoundingFrequency() != null) {
            command.setOldCompoundingFrequency(RegisterLoanRestructuringCommand.OldCompoundingFrequencyEnum.fromValue(
                    loan.getCompoundingFrequency().getValue()));
        }
        if (command.getOldDayCountConvention() == null && loan.getDayCountConvention() != null) {
            command.setOldDayCountConvention(RegisterLoanRestructuringCommand.OldDayCountConventionEnum.fromValue(
                    loan.getDayCountConvention().getValue()));
        }
        if (command.getRestructuringDate() == null) {
            command.setRestructuringDate(LocalDate.now());
        }
        if (command.getReason() == null || command.getReason().isBlank()) {
            command.setReason("Loan restructured");
        }
    }
}
