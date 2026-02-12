package com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands;

import org.fireflyframework.cqrs.command.Command;


import java.util.UUID;

public record RemoveLoanDisbursementCommand(
        UUID loanServicingCaseId,
        UUID loanDisbursementId
) implements Command<Void>{}