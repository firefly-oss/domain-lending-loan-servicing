package io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;

import io.fireflysoftware.core.lending.servicing.sdk.api.LoanServicingEventApi;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanServicingEventCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterLoanServicingEventHandler extends CommandHandler<RegisterLoanServicingEventCommand, UUID> {

    private final LoanServicingEventApi loanServicingEventApi;

    public RegisterLoanServicingEventHandler(LoanServicingEventApi loanServicingEventApi) {
        this.loanServicingEventApi = loanServicingEventApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterLoanServicingEventCommand cmd) {
        return loanServicingEventApi.createServicingEvent(cmd.getLoanServicingCaseId(), cmd, UUID.randomUUID().toString())
                .mapNotNull(response ->
                        Objects.requireNonNull(Objects.requireNonNull(response)).getLoanServicingEventId());
    }
}