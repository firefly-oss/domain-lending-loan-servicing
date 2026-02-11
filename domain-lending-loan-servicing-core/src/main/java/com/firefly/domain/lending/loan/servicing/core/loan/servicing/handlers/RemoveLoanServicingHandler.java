package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.core.lending.servicing.sdk.api.LoanServicingCaseApi;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanServicingCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveLoanServicingHandler extends CommandHandler<RemoveLoanServicingCommand, Void> {

    private final LoanServicingCaseApi loanServicingCaseApi;

    public RemoveLoanServicingHandler(LoanServicingCaseApi loanServicingCaseApi) {
        this.loanServicingCaseApi = loanServicingCaseApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveLoanServicingCommand cmd) {
        return loanServicingCaseApi.deleteServicingCase(cmd.loanServicingId()).then();
    }
}
