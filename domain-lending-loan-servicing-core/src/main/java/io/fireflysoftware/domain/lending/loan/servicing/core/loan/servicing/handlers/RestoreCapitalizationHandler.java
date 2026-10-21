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

import io.fireflysoftware.core.lending.servicing.sdk.api.LoanBalanceApi;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.CapitalizeLoanInterestCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RestoreCapitalizationCommand;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.util.UUID;

@CommandHandlerComponent
public class RestoreCapitalizationHandler extends CommandHandler<RestoreCapitalizationCommand, Void> {

    private final LoanBalanceApi balanceApi;

    public RestoreCapitalizationHandler(LoanBalanceApi balanceApi) {
        this.balanceApi = balanceApi;
    }

    @Override
    protected Mono<Void> doHandle(RestoreCapitalizationCommand cmd) {
        CapitalizeLoanInterestCommand.Capitalization capitalization = cmd.capitalization();
        if (capitalization == null || capitalization.previousBalanceId() == null) {
            return Mono.empty();
        }
        UUID caseId = capitalization.loanServicingCaseId();
        return balanceApi.getBalanceById(caseId, capitalization.previousBalanceId(), key())
                .flatMap(previous -> {
                    previous.setIsCurrent(true);
                    return balanceApi.updateBalance(caseId, previous.getLoanBalanceId(), previous, key());
                })
                .then(capitalization.newBalanceId() == null
                        ? Mono.empty()
                        : balanceApi.deleteBalance(caseId, capitalization.newBalanceId(), key()))
                .then();
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }
}
