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
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanDisbursementApi;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanAccrualCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanDisbursementCommand;
import reactor.core.publisher.Mono;

import java.util.UUID;

@CommandHandlerComponent
public class RemoveLoanDisbursementHandler extends CommandHandler<RemoveLoanDisbursementCommand, Void> {

    private final LoanDisbursementApi loanDisbursementApi;

    public RemoveLoanDisbursementHandler(LoanDisbursementApi loanDisbursementApi) {
        this.loanDisbursementApi = loanDisbursementApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveLoanDisbursementCommand cmd) {
        return loanDisbursementApi.deleteDisbursement(cmd.loanServicingCaseId(), cmd.loanDisbursementId(), UUID.randomUUID().toString()).then();
    }
}