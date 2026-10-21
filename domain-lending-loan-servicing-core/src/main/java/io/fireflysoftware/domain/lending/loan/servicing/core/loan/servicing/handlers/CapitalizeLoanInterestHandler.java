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
import io.fireflysoftware.core.lending.servicing.sdk.model.FilterRequestLoanBalanceDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.LoanBalanceDTO;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.CapitalizeLoanInterestCommand;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@CommandHandlerComponent
public class CapitalizeLoanInterestHandler extends CommandHandler<CapitalizeLoanInterestCommand, CapitalizeLoanInterestCommand.Capitalization> {

    private final LoanBalanceApi balanceApi;

    public CapitalizeLoanInterestHandler(LoanBalanceApi balanceApi) {
        this.balanceApi = balanceApi;
    }

    @Override
    protected Mono<CapitalizeLoanInterestCommand.Capitalization> doHandle(CapitalizeLoanInterestCommand cmd) {
        return balanceApi.findAllBalances(cmd.loanServicingCaseId(), new FilterRequestLoanBalanceDTO(), null)
                .flatMap(page -> {
                    LoanBalanceDTO current = null;
                    if (page != null && page.getContent() != null) {
                        for (LoanBalanceDTO balance : page.getContent()) {
                            if (Boolean.TRUE.equals(balance.getIsCurrent())) {
                                current = balance;
                                break;
                            }
                        }
                    }
                    if (current == null) {
                        return Mono.error(new IllegalStateException("Loan has no current balance to capitalize"));
                    }
                    BigDecimal interest = current.getInterestOutstanding() == null ? BigDecimal.ZERO : current.getInterestOutstanding();
                    if (interest.signum() <= 0) {
                        return Mono.error(new IllegalStateException("No accrued interest to capitalize"));
                    }
                    BigDecimal principal = current.getPrincipalOutstanding() == null ? BigDecimal.ZERO : current.getPrincipalOutstanding();
                    BigDecimal fees = current.getFeesOutstanding() == null ? BigDecimal.ZERO : current.getFeesOutstanding();
                    LoanBalanceDTO next = new LoanBalanceDTO();
                    next.setLoanServicingCaseId(cmd.loanServicingCaseId());
                    next.setPrincipalOutstanding(principal.add(interest));
                    next.setInterestOutstanding(BigDecimal.ZERO);
                    next.setFeesOutstanding(fees);
                    next.setTotalOutstanding(principal.add(interest).add(fees));
                    next.setBalanceDate(cmd.capitalizationDate() == null ? LocalDate.now() : cmd.capitalizationDate());
                    next.setIsCurrent(true);
                    LoanBalanceDTO previous = current;
                    return balanceApi.createBalance(cmd.loanServicingCaseId(), next, key())
                            .flatMap(created -> {
                                previous.setIsCurrent(false);
                                return balanceApi.updateBalance(cmd.loanServicingCaseId(), previous.getLoanBalanceId(), previous, key())
                                        .thenReturn(new CapitalizeLoanInterestCommand.Capitalization(
                                                cmd.loanServicingCaseId(), created.getLoanBalanceId(), previous.getLoanBalanceId()))
                                        .onErrorResume(error -> balanceApi.deleteBalance(
                                                        cmd.loanServicingCaseId(), created.getLoanBalanceId(), key())
                                                .then(Mono.error(error)));
                            });
                });
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }
}
