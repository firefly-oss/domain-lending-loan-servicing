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
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanServicingCaseQuery;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanRateChangeCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanRateChangeCommand;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Saga(name = "SetLoanRateSaga")
@Service
public class SetLoanRateSaga {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    public SetLoanRateSaga(CommandBus commandBus, QueryBus queryBus) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
    }

    @SagaStep(id = "retrieveCase")
    @StepEvent(type = "loanServicing.case.retrieved")
    public Mono<LoanServicingCaseDTO> retrieveCase(GetLoanServicingCaseQuery query, ExecutionContext ctx) {
        return queryBus.<LoanServicingCaseDTO>query(query)
                .switchIfEmpty(Mono.error(new IllegalStateException("Loan servicing case was not found")))
                .doOnNext(record -> ctx.putVariable("case", record));
    }

    @SagaStep(id = "registerRateChange", dependsOn = "retrieveCase", compensate = "removeRateChange")
    @StepEvent(type = "loanRate.changed")
    public Mono<UUID> registerRateChange(RegisterLoanRateChangeCommand command, ExecutionContext ctx) {
        LoanServicingCaseDTO loan = ctx.getVariableAs("case", LoanServicingCaseDTO.class);
        if (command.getOldInterestRate() == null && loan != null) {
            command.setOldInterestRate(loan.getInterestRate());
        }
        if (command.getNewInterestRate() == null || command.getEffectiveDate() == null || command.getReasonCode() == null) {
            return Mono.error(new IllegalArgumentException("New rate, effective date, and reason are required"));
        }
        ctx.putVariable("caseId", command.getLoanServicingCaseId());
        return commandBus.send(command);
    }

    public Mono<Void> removeRateChange(UUID rateChangeId, ExecutionContext ctx) {
        UUID caseId = ctx.getVariableAs("caseId", UUID.class);
        if (rateChangeId == null || caseId == null) {
            return Mono.empty();
        }
        return commandBus.send(new RemoveLoanRateChangeCommand(caseId, rateChangeId));
    }
}
