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


package io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance;

import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.schedule.AmortizationScheduleCalculator;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * In-memory calendar used by handler tests. It keeps superseded rows the way the servicing core does.
 */
public final class MemoryLoanCalendar implements LoanCalendarPort {

    private final List<StoredInstallment> rows = new ArrayList<>();

    @Override
    public Mono<List<StoredInstallment>> store(UUID caseId, CalendarTerms terms) {
        List<StoredInstallment> stored = new ArrayList<>();
        for (AmortizationScheduleCalculator.Installment row : AmortizationScheduleCalculator.generate(
                terms.principal(),
                terms.annualRatePercent(),
                terms.term(),
                terms.paymentFrequency(),
                terms.amortizationMethod(),
                terms.firstDueDate(),
                terms.installmentAmount())) {
            StoredInstallment saved = new StoredInstallment(
                    UUID.randomUUID(),
                    caseId,
                    row.number(),
                    row.dueDate(),
                    row.principalDue(),
                    row.interestDue(),
                    row.remainingBalance(),
                    false,
                    ScheduleState.CURRENT);
            rows.add(saved);
            stored.add(saved);
        }
        return Mono.just(List.copyOf(stored));
    }

    @Override
    public Mono<List<StoredInstallment>> read(UUID caseId) {
        return Mono.just(rows.stream()
                .filter(row -> caseId.equals(row.loanServicingCaseId()) && row.status() == ScheduleState.CURRENT)
                .toList());
    }

    @Override
    public Mono<ScheduleRevision> replace(UUID caseId, CalendarTerms terms) {
        List<StoredInstallment> previous = rows.stream()
                .filter(row -> caseId.equals(row.loanServicingCaseId())
                        && row.status() == ScheduleState.CURRENT
                        && !row.paid())
                .toList();
        List<StoredInstallment> kept = new ArrayList<>();
        for (StoredInstallment row : previous) {
            StoredInstallment superseded = new StoredInstallment(
                    row.id(), row.loanServicingCaseId(), row.number(), row.dueDate(),
                    row.principalDue(), row.interestDue(), row.remainingBalance(), row.paid(), ScheduleState.SUPERSEDED);
            rows.set(rows.indexOf(row), superseded);
            kept.add(superseded);
        }
        return store(caseId, terms).map(current -> new ScheduleRevision(caseId, List.copyOf(kept), current));
    }

    @Override
    public Mono<List<StoredInstallment>> restore(ScheduleRevision revision) {
        rows.removeIf(row -> revision.current().stream().anyMatch(current -> current.id().equals(row.id())));
        for (StoredInstallment previous : revision.previous()) {
            rows.removeIf(row -> row.id().equals(previous.id()));
            rows.add(new StoredInstallment(
                    previous.id(), previous.loanServicingCaseId(), previous.number(), previous.dueDate(),
                    previous.principalDue(), previous.interestDue(), previous.remainingBalance(),
                    previous.paid(), ScheduleState.CURRENT));
        }
        return read(revision.loanServicingCaseId());
    }
}
