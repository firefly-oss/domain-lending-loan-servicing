package io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands;

import org.fireflyframework.cqrs.command.Command;

import io.fireflysoftware.core.lending.servicing.sdk.model.LoanRepaymentRecordDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class RegisterLoanRepaymentRecordCommand extends LoanRepaymentRecordDTO implements Command<UUID> {
    private UUID loanServicingCaseId;

    public RegisterLoanRepaymentRecordCommand withLoanServicingCaseId(UUID loanServicingCaseId) {
        this.loanServicingCaseId = loanServicingCaseId;
        return this;
    }
}