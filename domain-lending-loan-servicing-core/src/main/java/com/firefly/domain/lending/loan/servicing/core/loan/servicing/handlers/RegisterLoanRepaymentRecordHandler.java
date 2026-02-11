package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.core.lending.servicing.sdk.api.LoanRepaymentRecordApi;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanRepaymentRecordCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterLoanRepaymentRecordHandler extends CommandHandler<RegisterLoanRepaymentRecordCommand, UUID> {

    private final LoanRepaymentRecordApi loanRepaymentRecordApi;

    public RegisterLoanRepaymentRecordHandler(LoanRepaymentRecordApi loanRepaymentRecordApi) {
        this.loanRepaymentRecordApi = loanRepaymentRecordApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterLoanRepaymentRecordCommand cmd) {
        return loanRepaymentRecordApi.createRepaymentRecord(cmd.getLoanServicingCaseId(), cmd, UUID.randomUUID().toString())
                .mapNotNull(response ->
                        Objects.requireNonNull(Objects.requireNonNull(response)).getLoanRepaymentRecordId());
    }
}