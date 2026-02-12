package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;

import com.firefly.core.lending.servicing.sdk.api.LoanAccrualApi;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanAccrualCommand;
import reactor.core.publisher.Mono;

import java.util.UUID;

@CommandHandlerComponent
public class RemoveLoanAccrualHandler extends CommandHandler<RemoveLoanAccrualCommand, Void> {

    private final LoanAccrualApi loanAccrualApi;

    public RemoveLoanAccrualHandler(LoanAccrualApi loanAccrualApi) {
        this.loanAccrualApi = loanAccrualApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveLoanAccrualCommand cmd) {
        return loanAccrualApi.deleteAccrual(cmd.loanServicingCaseId(), cmd.loanAccrualId(), UUID.randomUUID().toString()).then();
    }
}