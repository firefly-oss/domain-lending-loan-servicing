package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.core.lending.servicing.sdk.api.LoanServicingCaseApi;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanServicingCaseCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterLoanServicingHandler extends CommandHandler<RegisterLoanServicingCaseCommand, UUID> {

    private final LoanServicingCaseApi loanServicingCaseApi;

    public RegisterLoanServicingHandler( LoanServicingCaseApi loanServicingCaseApi) {
        this.loanServicingCaseApi = loanServicingCaseApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterLoanServicingCaseCommand cmd) {
        return loanServicingCaseApi.createServicingCase(cmd, UUID.randomUUID().toString())
                .mapNotNull(naturalPersonDTO ->
                        Objects.requireNonNull(Objects.requireNonNull(naturalPersonDTO)).getLoanServicingCaseId());
    }
}