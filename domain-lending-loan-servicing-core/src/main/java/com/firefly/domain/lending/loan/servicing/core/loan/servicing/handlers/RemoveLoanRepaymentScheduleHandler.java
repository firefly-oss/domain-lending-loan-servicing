package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;

import com.firefly.core.lending.servicing.sdk.api.LoanRepaymentScheduleApi;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RemoveLoanRepaymentScheduleCommand;
import reactor.core.publisher.Mono;

import java.util.UUID;

@CommandHandlerComponent
public class RemoveLoanRepaymentScheduleHandler extends CommandHandler<RemoveLoanRepaymentScheduleCommand, Void> {

    private final LoanRepaymentScheduleApi loanRepaymentScheduleApi;

    public RemoveLoanRepaymentScheduleHandler(LoanRepaymentScheduleApi loanRepaymentScheduleApi) {
        this.loanRepaymentScheduleApi = loanRepaymentScheduleApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveLoanRepaymentScheduleCommand cmd) {
        return loanRepaymentScheduleApi.deleteRepaymentSchedule(cmd.loanServicingCaseId(), cmd.loanRepaymentScheduleId(), UUID.randomUUID().toString()).then();
    }
}