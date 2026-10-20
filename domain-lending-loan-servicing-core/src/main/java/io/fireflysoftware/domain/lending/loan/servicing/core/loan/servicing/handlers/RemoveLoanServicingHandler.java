package io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;

import io.fireflysoftware.core.lending.servicing.sdk.api.LoanServicingCaseApi;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanServicingCommand;
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
