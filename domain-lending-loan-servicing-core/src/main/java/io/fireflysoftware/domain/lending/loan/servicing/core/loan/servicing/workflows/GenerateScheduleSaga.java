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

import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.GenerateAmortizationScheduleCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveGeneratedScheduleCommand;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Saga(name = "GenerateScheduleSaga")
@Service
public class GenerateScheduleSaga {

    private final CommandBus commandBus;

    public GenerateScheduleSaga(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @SagaStep(id = "generateSchedule", compensate = "removeGeneratedSchedule")
    @StepEvent(type = "loanSchedule.generated")
    public Mono<List<UUID>> generateSchedule(GenerateAmortizationScheduleCommand command, ExecutionContext ctx) {
        ctx.putVariable("caseId", command.loanServicingCaseId());
        return commandBus.send(command);
    }

    public Mono<Void> removeGeneratedSchedule(List<UUID> scheduleIds, ExecutionContext ctx) {
        UUID caseId = ctx.getVariableAs("caseId", UUID.class);
        if (caseId == null || scheduleIds == null || scheduleIds.isEmpty()) {
            return Mono.empty();
        }
        return commandBus.send(new RemoveGeneratedScheduleCommand(caseId, scheduleIds));
    }
}
