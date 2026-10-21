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


package io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.handlers;

import io.fireflysoftware.core.lending.servicing.sdk.api.LoanBalanceApi;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanRepaymentRecordApi;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanRepaymentScheduleApi;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanServicingCaseApi;
import io.fireflysoftware.core.lending.servicing.sdk.model.FilterRequestLoanBalanceDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.FilterRequestLoanRepaymentScheduleDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.LoanBalanceDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.LoanRepaymentRecordDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.LoanRepaymentScheduleDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.LoanServicingCaseDTO;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.AllocateRepaymentCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.schedule.AmortizationScheduleCalculator;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@CommandHandlerComponent
public class AllocateRepaymentHandler extends CommandHandler<AllocateRepaymentCommand, AllocateRepaymentCommand.Allocation> {

    private final LoanRepaymentRecordApi repaymentRecordApi;
    private final LoanBalanceApi balanceApi;
    private final LoanRepaymentScheduleApi scheduleApi;
    private final LoanServicingCaseApi servicingCaseApi;

    public AllocateRepaymentHandler(
            LoanRepaymentRecordApi repaymentRecordApi,
            LoanBalanceApi balanceApi,
            LoanRepaymentScheduleApi scheduleApi,
            LoanServicingCaseApi servicingCaseApi) {
        this.repaymentRecordApi = repaymentRecordApi;
        this.balanceApi = balanceApi;
        this.scheduleApi = scheduleApi;
        this.servicingCaseApi = servicingCaseApi;
    }

    @Override
    protected Mono<AllocateRepaymentCommand.Allocation> doHandle(AllocateRepaymentCommand cmd) {
        LoanRepaymentRecordDTO repayment = cmd.repayment();
        if (repayment == null || repayment.getPaymentAmount() == null || repayment.getPaymentDate() == null
                || repayment.getTransactionId() == null) {
            return Mono.error(new IllegalArgumentException("Payment amount, date, and transaction id are required"));
        }
        repayment.setLoanServicingCaseId(cmd.loanServicingCaseId());
        if (repayment.getIsPartialPayment() == null) {
            repayment.setIsPartialPayment(false);
        }
        return balanceApi.findAllBalances(cmd.loanServicingCaseId(), new FilterRequestLoanBalanceDTO(), null)
                .flatMap(page -> {
                    LoanBalanceDTO current = currentBalance(page == null ? null : page.getContent());
                    return repaymentRecordApi.createRepaymentRecord(cmd.loanServicingCaseId(), repayment, key())
                            .flatMap(saved -> applyBalance(cmd, saved.getLoanRepaymentRecordId(), current, repayment));
                });
    }

    private Mono<AllocateRepaymentCommand.Allocation> applyBalance(
            AllocateRepaymentCommand cmd,
            UUID repaymentId,
            LoanBalanceDTO current,
            LoanRepaymentRecordDTO repayment) {
        if (current == null) {
            return Mono.just(new AllocateRepaymentCommand.Allocation(
                    cmd.loanServicingCaseId(), repaymentId, null, null, List.of(), List.of()));
        }
        BigDecimal remainingPayment = nz(repayment.getPaymentAmount());
        BigDecimal interest = nz(current.getInterestOutstanding());
        BigDecimal fees = nz(current.getFeesOutstanding());
        BigDecimal principal = nz(current.getPrincipalOutstanding());
        BigDecimal toInterest = remainingPayment.min(interest);
        remainingPayment = remainingPayment.subtract(toInterest);
        BigDecimal toFees = remainingPayment.min(fees);
        remainingPayment = remainingPayment.subtract(toFees);
        BigDecimal toPrincipal = remainingPayment.min(principal);
        BigDecimal newInterest = interest.subtract(toInterest);
        BigDecimal newFees = fees.subtract(toFees);
        BigDecimal newPrincipal = principal.subtract(toPrincipal);

        LoanBalanceDTO next = new LoanBalanceDTO();
        next.setLoanServicingCaseId(cmd.loanServicingCaseId());
        next.setPrincipalOutstanding(newPrincipal);
        next.setInterestOutstanding(newInterest);
        next.setFeesOutstanding(newFees);
        next.setTotalOutstanding(newPrincipal.add(newInterest).add(newFees));
        next.setBalanceDate(repayment.getPaymentDate());
        next.setIsCurrent(true);

        return balanceApi.createBalance(cmd.loanServicingCaseId(), next, key())
                .flatMap(created -> balanceApi.getBalanceById(cmd.loanServicingCaseId(), current.getLoanBalanceId(), key())
                        .flatMap(previous -> {
                            previous.setIsCurrent(false);
                            return balanceApi.updateBalance(
                                    cmd.loanServicingCaseId(), previous.getLoanBalanceId(), previous, key());
                        })
                        .then(rebuildIfRequested(cmd, newPrincipal, repayment.getPaymentDate()))
                        .map(schedules -> new AllocateRepaymentCommand.Allocation(
                                cmd.loanServicingCaseId(),
                                repaymentId,
                                created.getLoanBalanceId(),
                                current.getLoanBalanceId(),
                                schedules.removed(),
                                schedules.created()))
                        .onErrorResume(error -> balanceApi.deleteBalance(
                                        cmd.loanServicingCaseId(), created.getLoanBalanceId(), key())
                                .then(repaymentRecordApi.deleteRepaymentRecord(cmd.loanServicingCaseId(), repaymentId, key()))
                                .then(Mono.error(error))));
    }

    private Mono<ScheduleChange> rebuildIfRequested(AllocateRepaymentCommand cmd, BigDecimal remainingPrincipal, LocalDate paymentDate) {
        if (!cmd.rebuildSchedule() || remainingPrincipal.signum() <= 0) {
            return Mono.just(new ScheduleChange(List.of(), List.of()));
        }
        return Mono.zip(
                servicingCaseApi.getServicingCaseById(cmd.loanServicingCaseId(), key()),
                scheduleApi.findAllRepaymentSchedules(cmd.loanServicingCaseId(), new FilterRequestLoanRepaymentScheduleDTO(), null)
        ).flatMap(tuple -> {
            List<LoanRepaymentScheduleDTO> unpaid = new ArrayList<>();
            if (tuple.getT2().getContent() != null) {
                for (LoanRepaymentScheduleDTO row : tuple.getT2().getContent()) {
                    if (!Boolean.TRUE.equals(row.getIsPaid())) {
                        unpaid.add(row);
                    }
                }
            }
            if (unpaid.isEmpty()) {
                return Mono.just(new ScheduleChange(List.of(), List.of()));
            }
            return Flux.fromIterable(unpaid)
                    .concatMap(row -> scheduleApi.deleteRepaymentSchedule(cmd.loanServicingCaseId(), row.getLoanRepaymentScheduleId(), key())
                            .thenReturn(row))
                    .collectList()
                    .flatMap(removed -> createReplacement(cmd.loanServicingCaseId(), tuple.getT1(), remainingPrincipal, removed, paymentDate));
        });
    }

    private Mono<ScheduleChange> createReplacement(
            UUID caseId,
            LoanServicingCaseDTO loan,
            BigDecimal remainingPrincipal,
            List<LoanRepaymentScheduleDTO> removed,
            LocalDate paymentDate) {
        String frequency = loan.getPaymentFrequency() == null ? "MONTHLY" : loan.getPaymentFrequency().getValue();
        String method = loan.getAmortizationMethod() == null ? "EQUAL_INSTALLMENT" : loan.getAmortizationMethod().getValue();
        LocalDate firstDue = removed.stream()
                .map(LoanRepaymentScheduleDTO::getDueDate)
                .filter(date -> date != null && !date.isBefore(paymentDate))
                .min(LocalDate::compareTo)
                .orElseGet(() -> AmortizationScheduleCalculator.addPeriod(paymentDate, frequency));
        var installments = AmortizationScheduleCalculator.generate(
                remainingPrincipal,
                loan.getInterestRate() == null ? BigDecimal.ZERO : loan.getInterestRate(),
                removed.size(),
                frequency,
                method,
                firstDue);
        List<UUID> created = new ArrayList<>();
        return Flux.fromIterable(installments)
                .concatMap(installment -> {
                    LoanRepaymentScheduleDTO row = new LoanRepaymentScheduleDTO();
                    row.setLoanServicingCaseId(caseId);
                    row.setInstallmentNumber(installment.number());
                    row.setDueDate(installment.dueDate());
                    row.setPrincipalDue(installment.principalDue());
                    row.setInterestDue(installment.interestDue());
                    row.setFeeDue(installment.feeDue());
                    row.setTotalDue(installment.totalDue());
                    row.setIsPaid(false);
                    return scheduleApi.createRepaymentSchedule(caseId, row, key())
                            .doOnNext(saved -> created.add(saved.getLoanRepaymentScheduleId()));
                })
                .then(Mono.just(new ScheduleChange(removed, List.copyOf(created))))
                .onErrorResume(error -> Flux.fromIterable(created)
                        .concatMap(id -> scheduleApi.deleteRepaymentSchedule(caseId, id, key()).onErrorResume(ignored -> Mono.empty()))
                        .then(Mono.error(error)));
    }

    private static LoanBalanceDTO currentBalance(List<LoanBalanceDTO> balances) {
        if (balances == null || balances.isEmpty()) {
            return null;
        }
        for (LoanBalanceDTO balance : balances) {
            if (Boolean.TRUE.equals(balance.getIsCurrent())) {
                return balance;
            }
        }
        return balances.get(0);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }

    private record ScheduleChange(List<LoanRepaymentScheduleDTO> removed, List<UUID> created) {
    }
}
