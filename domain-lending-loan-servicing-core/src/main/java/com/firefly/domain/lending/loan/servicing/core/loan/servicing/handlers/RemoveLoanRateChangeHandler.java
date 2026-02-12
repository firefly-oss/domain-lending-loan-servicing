package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;

import com.firefly.core.lending.servicing.sdk.api.LoanRateChangeApi;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanRateChangeCommand;
import reactor.core.publisher.Mono;

import java.util.UUID;

@CommandHandlerComponent
public class RemoveLoanRateChangeHandler extends CommandHandler<RemoveLoanRateChangeCommand, Void> {

    private final LoanRateChangeApi loanRateChangeApi;

    public RemoveLoanRateChangeHandler(LoanRateChangeApi loanRateChangeApi) {
        this.loanRateChangeApi = loanRateChangeApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveLoanRateChangeCommand cmd) {
        return loanRateChangeApi.deleteRateChange(cmd.loanServicingCaseId(), cmd.loanRateChangeId(), UUID.randomUUID().toString()).then();
    }
}