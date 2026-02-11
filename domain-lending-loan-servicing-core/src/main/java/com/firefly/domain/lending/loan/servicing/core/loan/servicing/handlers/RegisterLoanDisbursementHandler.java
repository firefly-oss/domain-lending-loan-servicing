package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.core.lending.servicing.sdk.api.LoanDisbursementApi;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanDisbursementCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterLoanDisbursementHandler extends CommandHandler<RegisterLoanDisbursementCommand, UUID> {

    private final LoanDisbursementApi loanDisbursementApi;

    public RegisterLoanDisbursementHandler(LoanDisbursementApi loanDisbursementApi) {
        this.loanDisbursementApi = loanDisbursementApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterLoanDisbursementCommand cmd) {
        return loanDisbursementApi.createDisbursement(cmd.getLoanServicingCaseId(), cmd, UUID.randomUUID().toString())
                .mapNotNull(response ->
                        Objects.requireNonNull(Objects.requireNonNull(response)).getLoanDisbursementId());
    }
}