package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.core.lending.servicing.sdk.api.LoanRepaymentRecordApi;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanRepaymentRecordCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveLoanRepaymentRecordHandler extends CommandHandler<RemoveLoanRepaymentRecordCommand, Void> {

    private final LoanRepaymentRecordApi loanRepaymentRecordApi;

    public RemoveLoanRepaymentRecordHandler(LoanRepaymentRecordApi loanRepaymentRecordApi) {
        this.loanRepaymentRecordApi = loanRepaymentRecordApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveLoanRepaymentRecordCommand cmd) {
        return loanRepaymentRecordApi.deleteRepaymentRecord(cmd.loanServicingCaseId(), cmd.loanRepaymentRecordId()).then();
    }
}