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

import io.fireflysoftware.core.lending.servicing.sdk.api.LoanAccrualApi;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanAccrualCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterLoanAccrualHandler extends CommandHandler<RegisterLoanAccrualCommand, UUID> {

    private final LoanAccrualApi loanAccrualApi;

    public RegisterLoanAccrualHandler(LoanAccrualApi loanAccrualApi) {
        this.loanAccrualApi = loanAccrualApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterLoanAccrualCommand cmd) {
        return loanAccrualApi.createAccrual(cmd.getLoanServicingCaseId(), cmd, UUID.randomUUID().toString())
                .mapNotNull(response ->
                        Objects.requireNonNull(Objects.requireNonNull(response)).getLoanAccrualId());
    }
}