package com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands;

import org.fireflyframework.cqrs.command.Command;

import com.firefly.core.lending.servicing.sdk.model.LoanRepaymentScheduleDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class RegisterLoanRepaymentScheduleCommand extends LoanRepaymentScheduleDTO implements Command<UUID> {
    private UUID loanServicingCaseId;

    public RegisterLoanRepaymentScheduleCommand withLoanServicingCaseId(UUID loanServicingCaseId) {
        this.loanServicingCaseId = loanServicingCaseId;
        return this;
    }
}