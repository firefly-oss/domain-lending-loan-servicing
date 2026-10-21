/*
 * Copyright 2025 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.services.impl;

import io.fireflysoftware.core.lending.servicing.sdk.model.LoanAccrualDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.LoanRateChangeDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.LoanServicingCaseDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.PaginationResponseLoanRepaymentScheduleDTO;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.AccrueInterestCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.AllocateRepaymentCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.ApplyPrepaymentCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.ApplyRepaymentCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.ApplyRestructureCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.CapitalizeInterestCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.CapitalizeLoanInterestCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.ChangeLoanServicingStatusCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.ChangeReferenceIndexCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.ChargeOffCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.CloseLoanCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.DisburseCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.EndPaymentHolidayCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.GenerateAmortizationScheduleCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.GeneratePenaltiesCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.GenerateScheduleCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanScheduleQuery;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanServicingCaseQuery;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.GrantPaymentHolidayCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.MarkInArrearsCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.NormalizeArrearsCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanAccrualCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanDisbursementCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanRateChangeCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RegisterLoanRestructuringCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.SetFixedRateCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.SubmitLoanCaseCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.WriteBackCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.services.LoanServicingService;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.saga.engine.SagaEngine;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import org.fireflyframework.orchestration.saga.engine.StepInputs;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static io.fireflysoftware.domain.lending.loan.servicing.core.loan.utils.constants.RegisterLoanServicingConstants.SAGA_REGISTER_LOAN_SERVICING;

@Service
public class LoanServicingServiceImpl implements LoanServicingService {

    private final SagaEngine engine;
    private final QueryBus queryBus;

    @Autowired
    public LoanServicingServiceImpl(SagaEngine engine, QueryBus queryBus) {
        this.engine = engine;
        this.queryBus = queryBus;
    }

    @Override
    public Mono<SagaResult> submitLoanCase(SubmitLoanCaseCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId("registerLoanServicing", command.getLoanServicingCase())
                .forStepId("registerLoanAccrual", command.getLoanAccrual())
                .forStepId("registerLoanDisbursement", command.getLoanDisbursement())
                .forStepId("registerLoanRateChange", command.getLoanRateChange())
                .forStepId("registerLoanRepaymentRecord", command.getLoanRepaymentRecord())
                .forStepId("registerLoanRepaymentSchedule", command.getLoanRepaymentSchedule())
                .forStepId("registerLoanServicingEvent", command.getLoanServicingEvent())
                .build();
        return engine.execute(SAGA_REGISTER_LOAN_SERVICING, inputs);
    }

    @Override
    public Mono<SagaResult> disburse(String loanId, DisburseCommand command) {
        UUID caseId = caseId(loanId);
        RegisterLoanDisbursementCommand register = copy(command, new RegisterLoanDisbursementCommand())
                .withLoanServicingCaseId(caseId);
        return engine.execute("DisburseLoanSaga", StepInputs.builder()
                .forStepId("registerDisbursement", register)
                .build());
    }

    @Override
    public Mono<SagaResult> generateSchedule(String loanId, GenerateScheduleCommand command) {
        UUID caseId = caseId(loanId);
        return engine.execute("GenerateScheduleSaga", StepInputs.builder()
                .forStepId("generateSchedule", new GenerateAmortizationScheduleCommand(caseId, command.getFirstDueDate()))
                .build());
    }

    @Override
    public Mono<SagaResult> applyRepayment(String loanId, ApplyRepaymentCommand command) {
        return allocate(loanId, command, false);
    }

    @Override
    public Mono<SagaResult> applyPrepayment(String loanId, ApplyPrepaymentCommand command) {
        if (command.getNote() == null || command.getNote().isBlank()) {
            command.setNote("Early principal reduction");
        }
        return allocate(loanId, command, true);
    }

    @Override
    public Mono<SagaResult> accrueInterest(String loanId, AccrueInterestCommand command) {
        return recordAccrual(loanId, command, LoanAccrualDTO.AccrualTypeEnum.INTEREST);
    }

    @Override
    public Mono<SagaResult> capitalizeInterest(String loanId, CapitalizeInterestCommand command) {
        UUID caseId = caseId(loanId);
        return engine.execute("CapitalizeInterestSaga", StepInputs.builder()
                .forStepId("capitalizeInterest", new CapitalizeLoanInterestCommand(caseId, command.getCapitalizationDate()))
                .build());
    }

    @Override
    public Mono<SagaResult> generatePenalties(String loanId, GeneratePenaltiesCommand command) {
        LoanAccrualDTO.AccrualTypeEnum type = command.getAccrualType() == null
                ? LoanAccrualDTO.AccrualTypeEnum.PENALTY
                : command.getAccrualType();
        return recordAccrual(loanId, command, type);
    }

    @Override
    public Mono<SagaResult> setFixedRate(String loanId, SetFixedRateCommand command) {
        return changeRate(loanId, command, LoanRateChangeDTO.ReasonCodeEnum.RENEGOTIATION);
    }

    @Override
    public Mono<SagaResult> changeReferenceIndex(String loanId, ChangeReferenceIndexCommand command) {
        return changeRate(loanId, command, LoanRateChangeDTO.ReasonCodeEnum.INDEX_ADJUSTMENT);
    }

    @Override
    public Mono<SagaResult> grantPaymentHoliday(String loanId, GrantPaymentHolidayCommand command) {
        return changeStatus(caseId(loanId), "FORBEARANCE", false, text(command.getDescription(), "Payment holiday granted"), "DEFERMENT");
    }

    @Override
    public Mono<SagaResult> endPaymentHoliday(String loanId, EndPaymentHolidayCommand command) {
        return changeStatus(caseId(loanId), "ACTIVE", false, text(command.getDescription(), "Payment holiday ended"), "NOTICE");
    }

    @Override
    public Mono<SagaResult> applyRestructure(String loanId, ApplyRestructureCommand command) {
        UUID caseId = caseId(loanId);
        RegisterLoanRestructuringCommand register = copy(command, new RegisterLoanRestructuringCommand())
                .withLoanServicingCaseId(caseId);
        return engine.execute("ApplyRestructureSaga", StepInputs.builder()
                .forStepId("retrieveCase", new GetLoanServicingCaseQuery(caseId))
                .forStepId("registerRestructuring", register)
                .forStepId("applyNewTerms", register)
                .build());
    }

    @Override
    public Mono<SagaResult> markInArrears(String loanId, MarkInArrearsCommand command) {
        return changeStatus(caseId(loanId), "DELINQUENT", false, text(command.getReason(), "Loan marked in arrears"), "NOTICE");
    }

    @Override
    public Mono<SagaResult> normalizeArrears(String loanId, NormalizeArrearsCommand command) {
        return changeStatus(caseId(loanId), "ACTIVE", false, text(command.getReason(), "Arrears normalized"), "NOTICE");
    }

    @Override
    public Mono<SagaResult> chargeOff(String loanId, ChargeOffCommand command) {
        return changeStatus(caseId(loanId), "CHARGED_OFF", false, text(command.getReason(), "Loan charged off"), "NOTICE");
    }

    @Override
    public Mono<SagaResult> writeBack(String loanId, WriteBackCommand command) {
        return changeStatus(caseId(loanId), "ACTIVE", false, text(command.getReason(), "Charged-off balance written back"), "NOTICE");
    }

    @Override
    public Mono<SagaResult> closeLoan(String loanId, CloseLoanCommand command) {
        return changeStatus(caseId(loanId), "CLOSED", true, text(command.getReason(), "Loan closed"), "NOTICE");
    }

    @Override
    public Mono<Object> getLoanDetails(String loanId) {
        UUID caseId = caseId(loanId);
        return queryBus.<LoanServicingCaseDTO>query(new GetLoanServicingCaseQuery(caseId))
                .zipWith(queryBus.<PaginationResponseLoanRepaymentScheduleDTO>query(
                        GetLoanScheduleQuery.builder().loanCaseId(caseId).build()))
                .map(tuple -> {
                    Map<String, Object> details = new LinkedHashMap<>();
                    details.put("servicingCase", tuple.getT1());
                    details.put("schedule", tuple.getT2());
                    return details;
                });
    }

    private Mono<SagaResult> allocate(String loanId, io.fireflysoftware.core.lending.servicing.sdk.model.LoanRepaymentRecordDTO repayment, boolean rebuildSchedule) {
        UUID caseId = caseId(loanId);
        repayment.setLoanServicingCaseId(caseId);
        return engine.execute("AllocateRepaymentSaga", StepInputs.builder()
                .forStepId("allocateRepayment", new AllocateRepaymentCommand(caseId, repayment, rebuildSchedule))
                .build());
    }

    private Mono<SagaResult> recordAccrual(String loanId, LoanAccrualDTO accrual, LoanAccrualDTO.AccrualTypeEnum defaultType) {
        UUID caseId = caseId(loanId);
        RegisterLoanAccrualCommand register = copy(accrual, new RegisterLoanAccrualCommand()).withLoanServicingCaseId(caseId);
        if (register.getAccrualType() == null) {
            register.setAccrualType(defaultType);
        }
        return engine.execute("RecordLoanAccrualSaga", StepInputs.builder()
                .forStepId("registerAccrual", register)
                .build());
    }

    private Mono<SagaResult> changeRate(String loanId, LoanRateChangeDTO rate, LoanRateChangeDTO.ReasonCodeEnum defaultReason) {
        UUID caseId = caseId(loanId);
        RegisterLoanRateChangeCommand register = copy(rate, new RegisterLoanRateChangeCommand()).withLoanServicingCaseId(caseId);
        if (register.getReasonCode() == null) {
            register.setReasonCode(defaultReason);
        }
        return engine.execute("SetLoanRateSaga", StepInputs.builder()
                .forStepId("retrieveCase", new GetLoanServicingCaseQuery(caseId))
                .forStepId("registerRateChange", register)
                .build());
    }

    private Mono<SagaResult> changeStatus(UUID caseId, String status, boolean requireZeroExposure, String reason, String eventType) {
        return engine.execute("ChangeLoanServicingStatusSaga", StepInputs.builder()
                .forStepId("retrieveCase", new GetLoanServicingCaseQuery(caseId))
                .forStepId("updateStatus", new ChangeLoanServicingStatusCommand(
                        caseId, status, requireZeroExposure, reason, eventType, LocalDate.now()))
                .build());
    }

    private static UUID caseId(String loanId) {
        try {
            return UUID.fromString(loanId);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new IllegalArgumentException("Loan id must be a UUID");
        }
    }

    private static String text(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static <T> T copy(Object source, T target) {
        if (source != null) {
            BeanUtils.copyProperties(source, target);
        }
        return target;
    }
}
