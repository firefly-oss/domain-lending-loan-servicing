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
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.ScheduleRevision;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.StoredInstallment;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.ReplaceRemainingScheduleCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.schedule.AmortizationScheduleCalculator;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.UUID;

/**
 * Replaces the unpaid schedule on the same servicing case and keeps the previous rows.
 */
@CommandHandlerComponent
public class ReplaceRemainingScheduleHandler extends CommandHandler<ReplaceRemainingScheduleCommand, ScheduleRevision> {

    private final LoanServicingCaseApi cases;
    private final LoanPricePort prices;
    private final LoanCalendarPort calendars;

    public ReplaceRemainingScheduleHandler(LoanServicingCaseApi cases, LoanPricePort prices, LoanCalendarPort calendars) {
        this.cases = cases;
        this.prices = prices;
        this.calendars = calendars;
    }

    @Override
    protected Mono<ScheduleRevision> doHandle(ReplaceRemainingScheduleCommand cmd) {
        return cases.getServicingCaseById(cmd.loanServicingCaseId(), key())
                .switchIfEmpty(Mono.error(new IllegalStateException("Loan servicing case was not found")))
                .flatMap(loan -> {
                    if (loan.getPrincipalAmount() == null || loan.getLoanTerm() == null || loan.getProductId() == null) {
                        return Mono.error(new IllegalStateException("Principal, term, and product id are required"));
                    }
                    return calendars.read(cmd.loanServicingCaseId())
                            .flatMap(current -> prices.price(loan.getPrincipalAmount(), loan.getLoanTerm(), loan.getProductId())
                                    .flatMap(price -> {
                                        loan.setInterestRate(price.annualRatePercent());
                                        String frequency = frequency(loan);
                                        LocalDate firstDue = current.stream()
                                                .filter(row -> !row.paid())
                                                .map(StoredInstallment::dueDate)
                                                .min(Comparator.naturalOrder())
                                                .orElseGet(() -> AmortizationScheduleCalculator.addPeriod(LocalDate.now(), frequency));
                                        CalendarTerms terms = new CalendarTerms(
                                                loan.getPrincipalAmount(),
                                                price.annualRatePercent(),
                                                loan.getLoanTerm(),
                                                frequency,
                                                loan.getAmortizationMethod() == null
                                                        ? "EQUAL_INSTALLMENT"
                                                        : loan.getAmortizationMethod().getValue(),
                                                firstDue,
                                                price.installmentAmount());
                                        return cases.updateServicingCase(cmd.loanServicingCaseId(), loan, key())
                                                .then(calendars.replace(cmd.loanServicingCaseId(), terms));
                                    }));
                });
    }

    private static String frequency(LoanServicingCaseDTO loan) {
        return loan.getPaymentFrequency() == null ? "MONTHLY" : loan.getPaymentFrequency().getValue();
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }
}
