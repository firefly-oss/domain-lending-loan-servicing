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

import io.fireflysoftware.core.lending.servicing.sdk.api.LoanRepaymentScheduleApi;
import io.fireflysoftware.core.lending.servicing.sdk.invoker.ApiClient;
import io.fireflysoftware.core.lending.servicing.sdk.model.LoanRepaymentScheduleDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.RestoreScheduleRequest;
import io.fireflysoftware.core.lending.servicing.sdk.model.ScheduleRevisionDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.StoreAmortizationCalendarRequest;
import io.fireflysoftware.domain.lending.loan.servicing.infra.LoanServicingProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Live calendar adapter. Stores and reads the amortization calendar through
 * {@link LoanRepaymentScheduleApi}.
 */
@Component
public class SdkLoanCalendarAdapter implements LoanCalendarPort {

    private final LoanRepaymentScheduleApi schedules;

    @Autowired
    public SdkLoanCalendarAdapter(LoanServicingProperties properties) {
        ApiClient client = new ApiClient();
        client.setBasePath(strip(properties.getBasePath()));
        this(new LoanRepaymentScheduleApi(client));
    }

    SdkLoanCalendarAdapter(LoanRepaymentScheduleApi schedules) {
        this.schedules = schedules;
    }

    @Override
    public Mono<List<StoredInstallment>> store(UUID caseId, CalendarTerms terms) {
        return schedules.storeCalendar(caseId, request(terms), key())
                .collectList()
                .map(SdkLoanCalendarAdapter::rows);
    }

    @Override
    public Mono<List<StoredInstallment>> read(UUID caseId) {
        return schedules.readCalendar(caseId, false, key())
                .collectList()
                .map(SdkLoanCalendarAdapter::rows);
    }

    @Override
    public Mono<ScheduleRevision> replace(UUID caseId, CalendarTerms terms) {
        return schedules.replaceCalendar(caseId, request(terms), key())
                .map(SdkLoanCalendarAdapter::revision);
    }

    @Override
    public Mono<List<StoredInstallment>> restore(ScheduleRevision revision) {
        RestoreScheduleRequest body = new RestoreScheduleRequest()
                .previousScheduleIds(revision.previous().stream().map(StoredInstallment::id).toList())
                .replacementScheduleIds(revision.current().stream().map(StoredInstallment::id).toList());
        return schedules.restoreCalendar(revision.loanServicingCaseId(), body, key())
                .collectList()
                .map(SdkLoanCalendarAdapter::rows);
    }

    private static ScheduleRevision revision(ScheduleRevisionDTO body) {
        return new ScheduleRevision(
                body.getLoanServicingCaseId(),
                rows(body.getPrevious()),
                rows(body.getCurrent()));
    }

    private static List<StoredInstallment> rows(List<LoanRepaymentScheduleDTO> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream().filter(row -> row != null).map(SdkLoanCalendarAdapter::toStored).toList();
    }

    private static StoredInstallment toStored(LoanRepaymentScheduleDTO row) {
        ScheduleState state = row.getScheduleStatus() == LoanRepaymentScheduleDTO.ScheduleStatusEnum.SUPERSEDED
                ? ScheduleState.SUPERSEDED
                : ScheduleState.CURRENT;
        return new StoredInstallment(
                row.getLoanRepaymentScheduleId(),
                row.getLoanServicingCaseId(),
                row.getInstallmentNumber() == null ? 0 : row.getInstallmentNumber(),
                row.getDueDate(),
                row.getPrincipalDue(),
                row.getInterestDue(),
                row.getRemainingBalance(),
                Boolean.TRUE.equals(row.getIsPaid()),
                state);
    }

    private static StoreAmortizationCalendarRequest request(CalendarTerms terms) {
        return new StoreAmortizationCalendarRequest()
                .principal(terms.principal())
                .annualRatePercent(terms.annualRatePercent())
                .term(terms.term())
                .paymentFrequency(terms.paymentFrequency())
                .amortizationMethod(terms.amortizationMethod())
                .firstDueDate(terms.firstDueDate())
                .installmentAmount(terms.installmentAmount());
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }

    private static String strip(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:8082";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
