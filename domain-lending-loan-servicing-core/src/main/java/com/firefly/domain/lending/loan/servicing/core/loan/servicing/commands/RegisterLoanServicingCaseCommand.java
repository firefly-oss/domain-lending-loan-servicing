package com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands;

import org.fireflyframework.cqrs.command.Command;

import com.firefly.core.lending.servicing.sdk.model.LoanServicingCaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class RegisterLoanServicingCaseCommand extends LoanServicingCaseDTO implements Command<UUID> {

}