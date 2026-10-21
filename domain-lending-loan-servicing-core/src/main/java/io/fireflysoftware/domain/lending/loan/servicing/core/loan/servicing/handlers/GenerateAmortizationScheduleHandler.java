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

import io.fireflysoftware.core.lending.servicing.sdk.api.LoanServicingCaseApi;
import io.fireflysoftware.core.lending.servicing.sdk.model.LoanServicingCaseDTO;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.CalendarTerms;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.LoanCalendarPort;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.LoanPricePort;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.StoredInstallment;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.GenerateAmortizationScheduleCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.schedule.AmortizationScheduleCalculator;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@CommandHandlerComponent
public class GenerateAmortizationScheduleHandler extends CommandHandler<GenerateAmortizationScheduleCommand, List<UUID>> {

    private final LoanServicingCaseApi loanServicingCaseApi;
    private final LoanPricePort prices;
    private final LoanCalendarPort calendars;

    public GenerateAmortizationScheduleHandler(
            LoanServicingCaseApi loanServicingCaseApi,
            LoanPricePort prices,
            LoanCalendarPort calendars) {
        this.loanServicingCaseApi = loanServicingCaseApi;
        this.prices = prices;
        this.calendars = calendars;
    }

    @Override
    protected Mono<List<UUID>> doHandle(GenerateAmortizationScheduleCommand cmd) {
        return loanServicingCaseApi.getServicingCaseById(cmd.loanServicingCaseId(), key())
                .flatMap(loan -> priceAndStore(cmd, loan));
    }

    private Mono<List<UUID>> priceAndStore(GenerateAmortizationScheduleCommand cmd, LoanServicingCaseDTO loan) {
        if (loan.getLoanTerm() == null || loan.getPrincipalAmount() == null || loan.getProductId() == null) {
            return Mono.error(new IllegalStateException("Loan term, principal, and product id are required"));
        }
        return prices.price(loan.getPrincipalAmount(), loan.getLoanTerm(), loan.getProductId())
                .flatMap(price -> {
                    loan.setInterestRate(price.annualRatePercent());
                    String frequency = loan.getPaymentFrequency() == null ? "MONTHLY" : loan.getPaymentFrequency().getValue();
                    String method = loan.getAmortizationMethod() == null
                            ? "EQUAL_INSTALLMENT"
                            : loan.getAmortizationMethod().getValue();
                    LocalDate firstDue = cmd.firstDueDate();
                    if (firstDue == null) {
                        LocalDate origin = loan.getOriginationDate() == null ? LocalDate.now() : loan.getOriginationDate();
                        firstDue = AmortizationScheduleCalculator.addPeriod(origin, frequency);
                    }
                    CalendarTerms terms = new CalendarTerms(
                            loan.getPrincipalAmount(),
                            price.annualRatePercent(),
                            loan.getLoanTerm(),
                            frequency,
                            method,
                            firstDue,
                            price.installmentAmount());
                    return loanServicingCaseApi.updateServicingCase(cmd.loanServicingCaseId(), loan, key())
                            .then(calendars.store(cmd.loanServicingCaseId(), terms))
                            .map(rows -> rows.stream().map(StoredInstallment::id).toList());
                });
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }
}
