package io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;

import io.fireflysoftware.core.lending.servicing.sdk.api.LoanAccrualApi;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanDisbursementApi;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanAccrualCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanDisbursementCommand;
import reactor.core.publisher.Mono;

import java.util.UUID;

@CommandHandlerComponent
public class RemoveLoanDisbursementHandler extends CommandHandler<RemoveLoanDisbursementCommand, Void> {

    private final LoanDisbursementApi loanDisbursementApi;

    public RemoveLoanDisbursementHandler(LoanDisbursementApi loanDisbursementApi) {
        this.loanDisbursementApi = loanDisbursementApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveLoanDisbursementCommand cmd) {
        return loanDisbursementApi.deleteDisbursement(cmd.loanServicingCaseId(), cmd.loanDisbursementId(), UUID.randomUUID().toString()).then();
    }
}