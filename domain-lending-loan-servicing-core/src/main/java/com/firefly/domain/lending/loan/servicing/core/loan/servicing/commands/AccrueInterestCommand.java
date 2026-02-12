package com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands;


import lombok.Data;
import org.fireflyframework.cqrs.command.Command;

import java.util.UUID;

@Data
public class AccrueInterestCommand  implements Command<UUID> {

}