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

import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.AllocateRepaymentCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RestoreRepaymentAllocationCommand;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Saga(name = "AllocateRepaymentSaga")
@Service
public class AllocateRepaymentSaga {

    private final CommandBus commandBus;

    public AllocateRepaymentSaga(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @SagaStep(id = "allocateRepayment", compensate = "restoreAllocation")
    @StepEvent(type = "loanRepayment.allocated")
    public Mono<AllocateRepaymentCommand.Allocation> allocateRepayment(AllocateRepaymentCommand command, ExecutionContext ctx) {
        return commandBus.send(command);
    }

    public Mono<Void> restoreAllocation(AllocateRepaymentCommand.Allocation allocation, ExecutionContext ctx) {
        if (allocation == null) {
            return Mono.empty();
        }
        return commandBus.send(new RestoreRepaymentAllocationCommand(allocation));
    }
}
