package com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands;

import com.firefly.common.cqrs.command.Command;
import lombok.Data;

import java.util.UUID;

@Data
public class SubmitLoanCaseCommand implements Command<UUID> {

    private RegisterLoanServicingCaseCommand loanServicingCase;
    private RegisterLoanAccrualCommand loanAccrual;
    private RegisterLoanDisbursementCommand loanDisbursement;
    private RegisterLoanRateChangeCommand loanRateChange;
    private RegisterLoanRepaymentRecordCommand loanRepaymentRecord;
    private RegisterLoanRepaymentScheduleCommand loanRepaymentSchedule;
    private RegisterLoanServicingEventCommand loanServicingEvent;


}