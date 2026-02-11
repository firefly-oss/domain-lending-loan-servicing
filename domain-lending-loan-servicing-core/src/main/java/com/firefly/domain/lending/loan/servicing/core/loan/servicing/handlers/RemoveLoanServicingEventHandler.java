package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.core.lending.servicing.sdk.api.LoanServicingEventApi;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanServicingEventCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveLoanServicingEventHandler extends CommandHandler<RemoveLoanServicingEventCommand, Void> {

    private final LoanServicingEventApi loanServicingEventApi;

    public RemoveLoanServicingEventHandler(LoanServicingEventApi loanServicingEventApi) {
        this.loanServicingEventApi = loanServicingEventApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveLoanServicingEventCommand cmd) {
        return loanServicingEventApi.deleteServicingEvent(cmd.loanServicingCaseId(), cmd.loanServicingEventId()).then();
    }
}