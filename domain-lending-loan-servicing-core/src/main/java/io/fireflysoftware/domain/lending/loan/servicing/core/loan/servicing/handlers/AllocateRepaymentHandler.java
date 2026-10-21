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
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.CashMovement;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.LedgerPort;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.LoanPricePort;
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
    private final LedgerPort ledger;
    private final LoanPricePort prices;

    private static final UUID CASH_ACCOUNT = UUID.fromString("00000000-0000-4000-8000-000000000021");
    private static final UUID RECEIVABLE_ACCOUNT = UUID.fromString("00000000-0000-4000-8000-000000000022");

    public AllocateRepaymentHandler(
            LoanRepaymentRecordApi repaymentRecordApi,
            LoanBalanceApi balanceApi,
            LoanRepaymentScheduleApi scheduleApi,
            LoanServicingCaseApi servicingCaseApi,
            LedgerPort ledger,
            LoanPricePort prices) {
        this.repaymentRecordApi = repaymentRecordApi;
        this.balanceApi = balanceApi;
        this.scheduleApi = scheduleApi;
        this.servicingCaseApi = servicingCaseApi;
        this.ledger = ledger;
        this.prices = prices;
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
                            .flatMap(saved -> markInstallmentPaid(cmd, repayment)
                                    .onErrorResume(error -> deleteRepayment(cmd.loanServicingCaseId(), saved.getLoanRepaymentRecordId())
                                            .then(Mono.error(error)))
                                    .flatMap(paid -> postCash(cmd, repayment)
                                            .onErrorResume(error -> restoreInstallment(cmd.loanServicingCaseId(), paid)
                                                    .then(deleteRepayment(cmd.loanServicingCaseId(), saved.getLoanRepaymentRecordId()))
                                                    .then(Mono.error(error)))
                                            .flatMap(postingId -> applyBalance(
                                                    cmd, saved.getLoanRepaymentRecordId(), current, repayment, postingId, paid)
                                                    .onErrorResume(error -> ledger.reverse(postingId)
                                                            .then(restoreInstallment(cmd.loanServicingCaseId(), paid))
                                                            .then(deleteRepayment(cmd.loanServicingCaseId(), saved.getLoanRepaymentRecordId()))
                                                            .then(Mono.error(error))))));
                });
    }

    private Mono<LoanRepaymentScheduleDTO> markInstallmentPaid(AllocateRepaymentCommand cmd, LoanRepaymentRecordDTO repayment) {
        return scheduleApi.findAllRepaymentSchedules(cmd.loanServicingCaseId(), new FilterRequestLoanRepaymentScheduleDTO(), null)
                .flatMap(page -> {
                    LoanRepaymentScheduleDTO due = earliestUnpaid(page == null ? null : page.getContent());
                    if (due == null) {
                        return Mono.just(new LoanRepaymentScheduleDTO());
                    }
                    LoanRepaymentScheduleDTO before = copySchedule(due);
                    due.setIsPaid(true);
                    due.setPaidDate(repayment.getPaymentDate());
                    due.setPaidAmount(repayment.getPaymentAmount());
                    return scheduleApi.updateRepaymentSchedule(
                                    cmd.loanServicingCaseId(), due.getLoanRepaymentScheduleId(), due, key())
                            .thenReturn(before);
                });
    }

    private Mono<UUID> postCash(AllocateRepaymentCommand cmd, LoanRepaymentRecordDTO repayment) {
        return servicingCaseApi.getServicingCaseById(cmd.loanServicingCaseId(), key())
                .flatMap(loan -> {
                    if (loan.getProductId() == null || loan.getPrincipalAmount() == null || loan.getLoanTerm() == null) {
                        return Mono.error(new IllegalStateException("Principal, term, and product id are required"));
                    }
                    return prices.price(loan.getPrincipalAmount(), loan.getLoanTerm(), loan.getProductId());
                })
                .flatMap(price -> ledger.post(new CashMovement(
                        repayment.getPaymentAmount(),
                        price.currency(),
                        repayment.getTransactionId().toString(),
                        CASH_ACCOUNT,
                        RECEIVABLE_ACCOUNT)));
    }

    private Mono<Void> deleteRepayment(UUID caseId, UUID repaymentId) {
        if (repaymentId == null) {
            return Mono.empty();
        }
        return repaymentRecordApi.deleteRepaymentRecord(caseId, repaymentId, key()).onErrorResume(ignored -> Mono.empty());
    }

    private Mono<Void> restoreInstallment(UUID caseId, LoanRepaymentScheduleDTO before) {
        if (before == null || before.getLoanRepaymentScheduleId() == null) {
            return Mono.empty();
        }
        return scheduleApi.updateRepaymentSchedule(caseId, before.getLoanRepaymentScheduleId(), before, key()).then();
    }

    private Mono<AllocateRepaymentCommand.Allocation> applyBalance(
            AllocateRepaymentCommand cmd,
            UUID repaymentId,
            LoanBalanceDTO current,
            LoanRepaymentRecordDTO repayment,
            UUID ledgerPostingId,
            LoanRepaymentScheduleDTO paidInstallment) {
        LoanRepaymentScheduleDTO paid = paidInstallment != null && paidInstallment.getLoanRepaymentScheduleId() != null
                ? paidInstallment
                : null;
        if (current == null) {
            return Mono.just(new AllocateRepaymentCommand.Allocation(
                    cmd.loanServicingCaseId(), repaymentId, null, null, List.of(), List.of(), ledgerPostingId, paid));
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
                                schedules.created(),
                                ledgerPostingId,
                                paid))
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

    private static LoanRepaymentScheduleDTO earliestUnpaid(List<LoanRepaymentScheduleDTO> rows) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        LoanRepaymentScheduleDTO earliest = null;
        for (LoanRepaymentScheduleDTO row : rows) {
            if (Boolean.TRUE.equals(row.getIsPaid()) || row.getDueDate() == null) {
                continue;
            }
            if (earliest == null || row.getDueDate().isBefore(earliest.getDueDate())) {
                earliest = row;
            }
        }
        return earliest;
    }

    private static LoanRepaymentScheduleDTO copySchedule(LoanRepaymentScheduleDTO source) {
        LoanRepaymentScheduleDTO copy = new LoanRepaymentScheduleDTO(source.getLoanRepaymentScheduleId());
        copy.setLoanServicingCaseId(source.getLoanServicingCaseId());
        copy.setInstallmentNumber(source.getInstallmentNumber());
        copy.setDueDate(source.getDueDate());
        copy.setPrincipalDue(source.getPrincipalDue());
        copy.setInterestDue(source.getInterestDue());
        copy.setFeeDue(source.getFeeDue());
        copy.setTotalDue(source.getTotalDue());
        copy.setIsPaid(source.getIsPaid());
        copy.setPaidDate(source.getPaidDate());
        copy.setPaidAmount(source.getPaidAmount());
        return copy;
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
