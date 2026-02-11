package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.core.lending.servicing.sdk.api.LoanServicingEventApi;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanServicingEventCommand;
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