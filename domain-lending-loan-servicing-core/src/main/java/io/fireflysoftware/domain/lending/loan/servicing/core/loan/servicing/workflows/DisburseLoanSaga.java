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

import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanDisbursementCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanDisbursementCommand;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Saga(name = "DisburseLoanSaga")
@Service
public class DisburseLoanSaga {

    private final CommandBus commandBus;

    public DisburseLoanSaga(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @SagaStep(id = "registerDisbursement", compensate = "removeDisbursement")
    @StepEvent(type = "loanDisbursement.registered")
    public Mono<UUID> registerDisbursement(RegisterLoanDisbursementCommand command, ExecutionContext ctx) {
        ctx.putVariable("caseId", command.getLoanServicingCaseId());
        return commandBus.send(command);
    }

    public Mono<Void> removeDisbursement(UUID disbursementId, ExecutionContext ctx) {
        UUID caseId = ctx.getVariableAs("caseId", UUID.class);
        if (disbursementId == null || caseId == null) {
            return Mono.empty();
        }
        return commandBus.send(new RemoveLoanDisbursementCommand(caseId, disbursementId));
    }
}
