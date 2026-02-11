package com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands;

import com.firefly.common.cqrs.command.Command;
import com.firefly.core.lending.servicing.sdk.model.LoanServicingEventDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class RegisterLoanServicingEventCommand extends LoanServicingEventDTO implements Command<UUID> {
    private UUID loanServicingCaseId;

    public RegisterLoanServicingEventCommand withLoanServicingCaseId(UUID loanServicingCaseId) {
        this.loanServicingCaseId = loanServicingCaseId;
        return this;
    }
}