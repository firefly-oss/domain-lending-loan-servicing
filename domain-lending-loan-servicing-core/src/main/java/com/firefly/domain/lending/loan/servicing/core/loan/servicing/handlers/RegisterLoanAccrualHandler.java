package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;

import com.firefly.core.lending.servicing.sdk.api.LoanAccrualApi;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanAccrualCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterLoanAccrualHandler extends CommandHandler<RegisterLoanAccrualCommand, UUID> {

    private final LoanAccrualApi loanAccrualApi;

    public RegisterLoanAccrualHandler(LoanAccrualApi loanAccrualApi) {
        this.loanAccrualApi = loanAccrualApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterLoanAccrualCommand cmd) {
        return loanAccrualApi.createAccrual(cmd.getLoanServicingCaseId(), cmd, UUID.randomUUID().toString())
                .mapNotNull(response ->
                        Objects.requireNonNull(Objects.requireNonNull(response)).getLoanAccrualId());
    }
}