package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;

import com.firefly.core.lending.servicing.sdk.api.LoanServicingCaseApi;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanServicingCommand;
import reactor.core.publisher.Mono;

import java.util.UUID;

@CommandHandlerComponent
public class RemoveLoanServicingHandler extends CommandHandler<RemoveLoanServicingCommand, Void> {

    private final LoanServicingCaseApi loanServicingCaseApi;

    public RemoveLoanServicingHandler(LoanServicingCaseApi loanServicingCaseApi) {
        this.loanServicingCaseApi = loanServicingCaseApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveLoanServicingCommand cmd) {
        return loanServicingCaseApi.deleteServicingCase(cmd.loanServicingId(), UUID.randomUUID().toString()).then();
    }
}
