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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.fireflysoftware.core.lending.servicing.sdk.invoker.ApiClient;
import io.fireflysoftware.domain.lending.loan.servicing.infra.LoanServicingProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Live calendar adapter. Calls the loan servicing core through its SDK client.
 */
@Component
public class SdkLoanCalendarAdapter implements LoanCalendarPort {

    private final WebClient webClient;
    private final String basePath;

    @Autowired
    public SdkLoanCalendarAdapter(LoanServicingProperties properties) {
        String base = strip(properties.getBasePath());
        ApiClient client = new ApiClient();
        client.setBasePath(base);
        this.webClient = client.getWebClient();
        this.basePath = base;
    }

    SdkLoanCalendarAdapter(WebClient webClient, String basePath) {
        this.webClient = webClient;
        this.basePath = strip(basePath);
    }

    @Override
    public Mono<List<StoredInstallment>> store(UUID caseId, CalendarTerms terms) {
        return webClient.post()
                .uri(basePath + "/api/v1/loan-servicing-cases/{caseId}/repayment-schedules/calendar", caseId)
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(CalendarBody.from(terms))
                .retrieve()
                .bodyToMono(CalendarRow[].class)
                .map(SdkLoanCalendarAdapter::rows);
    }

    @Override
    public Mono<List<StoredInstallment>> read(UUID caseId) {
        return webClient.get()
                .uri(basePath + "/api/v1/loan-servicing-cases/{caseId}/repayment-schedules/calendar", caseId)
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .retrieve()
                .bodyToMono(CalendarRow[].class)
                .map(SdkLoanCalendarAdapter::rows);
    }

    @Override
    public Mono<ScheduleRevision> replace(UUID caseId, CalendarTerms terms) {
        return webClient.post()
                .uri(basePath + "/api/v1/loan-servicing-cases/{caseId}/repayment-schedules/calendar/replacements", caseId)
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(CalendarBody.from(terms))
                .retrieve()
                .bodyToMono(RevisionBody.class)
                .map(body -> new ScheduleRevision(body.loanServicingCaseId, rows(body.previous), rows(body.current)));
    }

    @Override
    public Mono<List<StoredInstallment>> restore(ScheduleRevision revision) {
        RestoreBody body = new RestoreBody(
                revision.previous().stream().map(StoredInstallment::id).toList(),
                revision.current().stream().map(StoredInstallment::id).toList());
        return webClient.post()
                .uri(basePath + "/api/v1/loan-servicing-cases/{caseId}/repayment-schedules/calendar/restore",
                        revision.loanServicingCaseId())
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(CalendarRow[].class)
                .map(SdkLoanCalendarAdapter::rows);
    }

    private static List<StoredInstallment> rows(CalendarRow[] rows) {
        if (rows == null) {
            return List.of();
        }
        return rows(List.of(rows));
    }

    private static List<StoredInstallment> rows(List<CalendarRow> rows) {
        if (rows == null) {
            return List.of();
        }
        return rows.stream().map(CalendarRow::toStored).toList();
    }

    private static String strip(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:8082";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private record CalendarBody(
            BigDecimal principal,
            BigDecimal annualRatePercent,
            Integer term,
            String paymentFrequency,
            String amortizationMethod,
            LocalDate firstDueDate,
            BigDecimal installmentAmount
    ) {
        static CalendarBody from(CalendarTerms terms) {
            return new CalendarBody(
                    terms.principal(),
                    terms.annualRatePercent(),
                    terms.term(),
                    terms.paymentFrequency(),
                    terms.amortizationMethod(),
                    terms.firstDueDate(),
                    terms.installmentAmount());
        }
    }

    private record RestoreBody(List<UUID> previousScheduleIds, List<UUID> replacementScheduleIds) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class RevisionBody {
        public UUID loanServicingCaseId;
        public List<CalendarRow> previous;
        public List<CalendarRow> current;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class CalendarRow {
        public UUID loanRepaymentScheduleId;
        public UUID loanServicingCaseId;
        public Integer installmentNumber;
        public LocalDate dueDate;
        public BigDecimal principalDue;
        public BigDecimal interestDue;
        public BigDecimal remainingBalance;
        public Boolean isPaid;
        public String scheduleStatus;

        StoredInstallment toStored() {
            ScheduleState state = "SUPERSEDED".equals(scheduleStatus) ? ScheduleState.SUPERSEDED : ScheduleState.CURRENT;
            return new StoredInstallment(
                    loanRepaymentScheduleId,
                    loanServicingCaseId,
                    installmentNumber == null ? 0 : installmentNumber,
                    dueDate,
                    principalDue,
                    interestDue,
                    remainingBalance,
                    Boolean.TRUE.equals(isPaid),
                    state);
        }
    }
}
