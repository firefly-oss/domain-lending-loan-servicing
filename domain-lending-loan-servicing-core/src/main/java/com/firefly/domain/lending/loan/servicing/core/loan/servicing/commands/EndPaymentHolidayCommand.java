package com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands;

import org.fireflyframework.cqrs.command.Command;

import lombok.Data;

import java.util.UUID;

@Data
public class EndPaymentHolidayCommand  implements Command<UUID> {

}