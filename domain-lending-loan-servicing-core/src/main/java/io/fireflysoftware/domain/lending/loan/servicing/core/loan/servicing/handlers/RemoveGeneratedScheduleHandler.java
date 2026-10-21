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


package io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.handlers;

import io.fireflysoftware.core.lending.servicing.sdk.api.LoanRepaymentScheduleApi;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveGeneratedScheduleCommand;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@CommandHandlerComponent
public class RemoveGeneratedScheduleHandler extends CommandHandler<RemoveGeneratedScheduleCommand, Void> {

    private final LoanRepaymentScheduleApi loanRepaymentScheduleApi;

    public RemoveGeneratedScheduleHandler(LoanRepaymentScheduleApi loanRepaymentScheduleApi) {
        this.loanRepaymentScheduleApi = loanRepaymentScheduleApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveGeneratedScheduleCommand cmd) {
        if (cmd.scheduleIds() == null || cmd.scheduleIds().isEmpty()) {
            return Mono.empty();
        }
        return Flux.fromIterable(cmd.scheduleIds())
                .concatMap(id -> loanRepaymentScheduleApi.deleteRepaymentSchedule(
                        cmd.loanServicingCaseId(), id, UUID.randomUUID().toString()))
                .then();
    }
}
