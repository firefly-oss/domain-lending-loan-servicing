package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;

import com.firefly.core.lending.servicing.sdk.api.LoanRepaymentScheduleApi;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanRepaymentScheduleCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterLoanRepaymentScheduleHandler extends CommandHandler<RegisterLoanRepaymentScheduleCommand, UUID> {

    private final LoanRepaymentScheduleApi loanRepaymentScheduleApi;

    public RegisterLoanRepaymentScheduleHandler(LoanRepaymentScheduleApi loanRepaymentScheduleApi) {
        this.loanRepaymentScheduleApi = loanRepaymentScheduleApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterLoanRepaymentScheduleCommand cmd) {
        return loanRepaymentScheduleApi.createRepaymentSchedule(cmd.getLoanServicingCaseId(), cmd, UUID.randomUUID().toString())
                .mapNotNull(response ->
                        Objects.requireNonNull(Objects.requireNonNull(response)).getLoanRepaymentScheduleId());
    }
}