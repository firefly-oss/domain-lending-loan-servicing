package com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands;

import com.firefly.common.cqrs.command.Command;

import java.util.UUID;

public record RemoveLoanRepaymentScheduleCommand(
        UUID loanServicingCaseId,
        UUID loanRepaymentScheduleId
) implements Command<Void>{}