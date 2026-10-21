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

import io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.CalendarTerms;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.MemoryLoanCalendar;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.GetAmortizationCalendarQuery;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GetAmortizationCalendarHandlerTest {

    @Test
    void readsTheCurrentCalendarWithRemainingBalance() {
        UUID caseId = UUID.randomUUID();
        MemoryLoanCalendar calendar = new MemoryLoanCalendar();
        calendar.store(caseId, new CalendarTerms(
                new BigDecimal("1200.00"),
                new BigDecimal("12.00"),
                12,
                "MONTHLY",
                "EQUAL_INSTALLMENT",
                LocalDate.of(2026, 2, 1),
                null)).block();
        GetAmortizationCalendarHandler handler = new GetAmortizationCalendarHandler(calendar);

        var rows = handler.doHandle(GetAmortizationCalendarQuery.builder().loanCaseId(caseId).build()).block();

        assertThat(rows).hasSize(12);
        assertThat(rows.get(0).installmentNumber()).isEqualTo(1);
        assertThat(rows.get(0).dueDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(rows.get(0).principal()).isNotNull();
        assertThat(rows.get(0).interest()).isEqualByComparingTo("12.00");
        assertThat(rows.get(11).remainingBalance()).isEqualByComparingTo("0.00");
    }
}
