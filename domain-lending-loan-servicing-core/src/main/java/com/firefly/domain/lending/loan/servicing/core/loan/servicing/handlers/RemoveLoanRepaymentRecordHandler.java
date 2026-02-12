package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;

import com.firefly.core.lending.servicing.sdk.api.LoanRepaymentRecordApi;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanRepaymentRecordCommand;
import reactor.core.publisher.Mono;

import java.util.UUID;

@CommandHandlerComponent
public class RemoveLoanRepaymentRecordHandler extends CommandHandler<RemoveLoanRepaymentRecordCommand, Void> {

    private final LoanRepaymentRecordApi loanRepaymentRecordApi;

    public RemoveLoanRepaymentRecordHandler(LoanRepaymentRecordApi loanRepaymentRecordApi) {
        this.loanRepaymentRecordApi = loanRepaymentRecordApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveLoanRepaymentRecordCommand cmd) {
        return loanRepaymentRecordApi.deleteRepaymentRecord(cmd.loanServicingCaseId(), cmd.loanRepaymentRecordId(), UUID.randomUUID().toString()).then();
    }
}