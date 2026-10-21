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

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;

import io.fireflysoftware.core.lending.servicing.sdk.api.LoanServicingEventApi;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanServicingEventCommand;
import reactor.core.publisher.Mono;

import java.util.UUID;

@CommandHandlerComponent
public class RemoveLoanServicingEventHandler extends CommandHandler<RemoveLoanServicingEventCommand, Void> {

    private final LoanServicingEventApi loanServicingEventApi;

    public RemoveLoanServicingEventHandler(LoanServicingEventApi loanServicingEventApi) {
        this.loanServicingEventApi = loanServicingEventApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveLoanServicingEventCommand cmd) {
        return loanServicingEventApi.deleteServicingEvent(cmd.loanServicingCaseId(), cmd.loanServicingEventId(), UUID.randomUUID().toString()).then();
    }
}