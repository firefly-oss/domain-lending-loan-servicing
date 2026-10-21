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
import io.fireflysoftware.core.lending.servicing.sdk.model.PaginationResponseLoanBalanceDTO;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.ApplyLoanServicingStatusCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.ChangeLoanServicingStatusCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanBalancesQuery;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanServicingCaseQuery;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanServicingEventCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanServicingEventCommand;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

@Saga(name = "ChangeLoanServicingStatusSaga")
@Service
public class ChangeLoanServicingStatusSaga {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    public ChangeLoanServicingStatusSaga(CommandBus commandBus, QueryBus queryBus) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
    }

    @SagaStep(id = "retrieveCase")
    @StepEvent(type = "loanServicing.case.retrieved")
    public Mono<LoanServicingCaseDTO> retrieveCase(GetLoanServicingCaseQuery query, ExecutionContext ctx) {
        return queryBus.<LoanServicingCaseDTO>query(query)
                .switchIfEmpty(Mono.error(new IllegalStateException("Loan servicing case was not found")))
                .doOnNext(record -> {
                    ctx.putVariable("case", record);
                    if (record.getServicingStatus() != null) {
                        ctx.putVariable("previousStatus", record.getServicingStatus().getValue());
                    }
                });
    }

    @SagaStep(id = "updateStatus", dependsOn = "retrieveCase", compensate = "restoreStatus")
    @StepEvent(type = "loanServicing.status.updated")
    public Mono<LoanServicingCaseDTO> updateStatus(ChangeLoanServicingStatusCommand command, ExecutionContext ctx) {
        LoanServicingCaseDTO current = ctx.getVariableAs("case", LoanServicingCaseDTO.class);
        if (current == null || current.getLoanServicingCaseId() == null) {
            return Mono.error(new IllegalStateException("Loan servicing case was not found"));
        }
        if (command.status() == null || command.status().isBlank()) {
            return Mono.error(new IllegalArgumentException("Status is required"));
        }
        Mono<Void> gate = command.requireZeroExposure()
                ? assertZeroExposure(command.loanServicingCaseId())
                : Mono.empty();
        return gate.then(Mono.defer(() -> {
            current.setServicingStatus(LoanServicingCaseDTO.ServicingStatusEnum.fromValue(command.status()));
            if (command.reason() != null && !command.reason().isBlank()) {
                current.setRemarks(command.reason());
            }
            return commandBus.<LoanServicingCaseDTO>send(
                            new ApplyLoanServicingStatusCommand(current.getLoanServicingCaseId(), current))
                    .flatMap(updated -> recordEvent(command, current.getLoanServicingCaseId(), ctx).thenReturn(updated));
        }));
    }

    public Mono<Void> restoreStatus(LoanServicingCaseDTO updated, ExecutionContext ctx) {
        if (updated == null || updated.getLoanServicingCaseId() == null) {
            return Mono.empty();
        }
        String previous = ctx.getVariableAs("previousStatus", String.class);
        UUID eventId = (UUID) ctx.getVariable("eventId");
        Mono<Void> removeEvent = eventId == null
                ? Mono.empty()
                : commandBus.send(new RemoveLoanServicingEventCommand(updated.getLoanServicingCaseId(), eventId));
        if (previous == null) {
            return removeEvent;
        }
        updated.setServicingStatus(LoanServicingCaseDTO.ServicingStatusEnum.fromValue(previous));
        return removeEvent.then(commandBus.send(
                new ApplyLoanServicingStatusCommand(updated.getLoanServicingCaseId(), updated)).then());
    }

    private Mono<Void> assertZeroExposure(UUID caseId) {
        return queryBus.<PaginationResponseLoanBalanceDTO>query(GetLoanBalancesQuery.builder().loanCaseId(caseId).build())
                .flatMap(page -> {
                    boolean exposure = page != null && page.getContent() != null && page.getContent().stream()
                            .anyMatch(balance -> Boolean.TRUE.equals(balance.getIsCurrent())
                                    && balance.getTotalOutstanding() != null
                                    && balance.getTotalOutstanding().signum() > 0);
                    if (exposure) {
                        return Mono.error(new IllegalStateException("Loan still has outstanding exposure"));
                    }
                    return Mono.empty();
                });
    }

    private Mono<UUID> recordEvent(ChangeLoanServicingStatusCommand command, UUID caseId, ExecutionContext ctx) {
        if (command.eventType() == null || command.eventType().isBlank()) {
            return Mono.empty();
        }
        RegisterLoanServicingEventCommand event = new RegisterLoanServicingEventCommand()
                .withLoanServicingCaseId(caseId);
        event.setEventType(io.fireflysoftware.core.lending.servicing.sdk.model.LoanServicingEventDTO.EventTypeEnum
                .fromValue(command.eventType()));
        event.setEventDate(command.eventDate() == null ? LocalDate.now() : command.eventDate());
        event.setDescription(command.reason() == null || command.reason().isBlank()
                ? "Servicing status changed to " + command.status()
                : command.reason());
        return commandBus.<UUID>send(event).doOnNext(id -> ctx.putVariable("eventId", id));
    }
}
