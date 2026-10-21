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

import io.fireflysoftware.core.lending.servicing.sdk.api.LoanRepaymentScheduleApi;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanServicingCaseApi;
import io.fireflysoftware.core.lending.servicing.sdk.model.LoanRepaymentScheduleDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.LoanServicingCaseDTO;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.GenerateAmortizationScheduleCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.schedule.AmortizationScheduleCalculator;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@CommandHandlerComponent
public class GenerateAmortizationScheduleHandler extends CommandHandler<GenerateAmortizationScheduleCommand, List<UUID>> {

    private final LoanServicingCaseApi loanServicingCaseApi;
    private final LoanRepaymentScheduleApi loanRepaymentScheduleApi;

    public GenerateAmortizationScheduleHandler(
            LoanServicingCaseApi loanServicingCaseApi,
            LoanRepaymentScheduleApi loanRepaymentScheduleApi) {
        this.loanServicingCaseApi = loanServicingCaseApi;
        this.loanRepaymentScheduleApi = loanRepaymentScheduleApi;
    }

    @Override
    protected Mono<List<UUID>> doHandle(GenerateAmortizationScheduleCommand cmd) {
        return loanServicingCaseApi.getServicingCaseById(cmd.loanServicingCaseId(), UUID.randomUUID().toString())
                .flatMap(loan -> createRows(cmd, loan));
    }

    private Mono<List<UUID>> createRows(GenerateAmortizationScheduleCommand cmd, LoanServicingCaseDTO loan) {
        if (loan.getLoanTerm() == null || loan.getPrincipalAmount() == null || loan.getInterestRate() == null) {
            return Mono.error(new IllegalStateException("Loan term, principal, and interest rate are required"));
        }
        String frequency = loan.getPaymentFrequency() == null ? "MONTHLY" : loan.getPaymentFrequency().getValue();
        String method = loan.getAmortizationMethod() == null ? "EQUAL_INSTALLMENT" : loan.getAmortizationMethod().getValue();
        LocalDate firstDue = cmd.firstDueDate();
        if (firstDue == null) {
            LocalDate origin = loan.getOriginationDate() == null ? LocalDate.now() : loan.getOriginationDate();
            firstDue = AmortizationScheduleCalculator.addPeriod(origin, frequency);
        }
        var installments = AmortizationScheduleCalculator.generate(
                loan.getPrincipalAmount(),
                loan.getInterestRate(),
                loan.getLoanTerm(),
                frequency,
                method,
                firstDue);
        List<UUID> created = new ArrayList<>();
        return Flux.fromIterable(installments)
                .concatMap(installment -> {
                    LoanRepaymentScheduleDTO row = new LoanRepaymentScheduleDTO();
                    row.setLoanServicingCaseId(cmd.loanServicingCaseId());
                    row.setInstallmentNumber(installment.number());
                    row.setDueDate(installment.dueDate());
                    row.setPrincipalDue(installment.principalDue());
                    row.setInterestDue(installment.interestDue());
                    row.setFeeDue(installment.feeDue());
                    row.setTotalDue(installment.totalDue());
                    row.setIsPaid(false);
                    return loanRepaymentScheduleApi.createRepaymentSchedule(
                                    cmd.loanServicingCaseId(), row, UUID.randomUUID().toString())
                            .map(saved -> {
                                created.add(saved.getLoanRepaymentScheduleId());
                                return saved.getLoanRepaymentScheduleId();
                            });
                })
                .collectList()
                .onErrorResume(error -> Flux.fromIterable(created)
                        .concatMap(id -> loanRepaymentScheduleApi.deleteRepaymentSchedule(
                                cmd.loanServicingCaseId(), id, UUID.randomUUID().toString()).onErrorResume(ignored -> Mono.empty()))
                        .then(Mono.error(error)));
    }
}
