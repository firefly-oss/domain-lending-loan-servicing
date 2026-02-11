package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.core.lending.servicing.sdk.api.LoanAccrualApi;
import com.firefly.core.lending.servicing.sdk.api.LoanDisbursementApi;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanAccrualCommand;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanDisbursementCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveLoanDisbursementHandler extends CommandHandler<RemoveLoanDisbursementCommand, Void> {

    private final LoanDisbursementApi loanDisbursementApi;

    public RemoveLoanDisbursementHandler(LoanDisbursementApi loanDisbursementApi) {
        this.loanDisbursementApi = loanDisbursementApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveLoanDisbursementCommand cmd) {
        return loanDisbursementApi.deleteDisbursement(cmd.loanServicingCaseId(), cmd.loanDisbursementId()).then();
    }
}