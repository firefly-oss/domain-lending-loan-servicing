package io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands;

import org.fireflyframework.cqrs.command.Command;

import io.fireflysoftware.core.lending.servicing.sdk.model.LoanServicingCaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class RegisterLoanServicingCaseCommand extends LoanServicingCaseDTO implements Command<UUID> {

}