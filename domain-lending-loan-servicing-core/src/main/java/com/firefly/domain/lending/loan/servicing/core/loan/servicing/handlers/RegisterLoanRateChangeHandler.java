package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.core.lending.servicing.sdk.api.LoanRateChangeApi;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanRateChangeCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterLoanRateChangeHandler extends CommandHandler<RegisterLoanRateChangeCommand, UUID> {

    private final LoanRateChangeApi loanRateChangeApi;

    public RegisterLoanRateChangeHandler(LoanRateChangeApi loanRateChangeApi) {
        this.loanRateChangeApi = loanRateChangeApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterLoanRateChangeCommand cmd) {
        return loanRateChangeApi.createRateChange(cmd.getLoanServicingCaseId(), cmd, UUID.randomUUID().toString())
                .mapNotNull(response ->
                        Objects.requireNonNull(Objects.requireNonNull(response)).getLoanRateChangeId());
    }
}