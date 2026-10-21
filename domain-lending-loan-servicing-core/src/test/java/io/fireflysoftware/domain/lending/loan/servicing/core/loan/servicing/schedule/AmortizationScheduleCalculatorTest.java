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


package io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.schedule;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AmortizationScheduleCalculatorTest {

    @Test
    void levelPaymentPrincipalSumsToTheOriginalAmount() {
        var rows = AmortizationScheduleCalculator.generate(
                new BigDecimal("1200.00"),
                new BigDecimal("12.00"),
                12,
                "MONTHLY",
                "EQUAL_INSTALLMENT",
                LocalDate.of(2026, 2, 1));

        assertThat(rows).hasSize(12);
        assertThat(rows.get(0).dueDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(rows.get(11).dueDate()).isEqualTo(LocalDate.of(2027, 1, 1));
        BigDecimal principal = rows.stream()
                .map(AmortizationScheduleCalculator.Installment::principalDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(principal).isEqualByComparingTo("1200.00");
        assertThat(rows.get(0).interestDue()).isEqualByComparingTo("12.00");
    }

    @Test
    void interestOnlyKeepsPrincipalOnTheFinalInstallment() {
        var rows = AmortizationScheduleCalculator.generate(
                new BigDecimal("1000.00"),
                new BigDecimal("12.00"),
                3,
                "MONTHLY",
                "INTEREST_ONLY",
                LocalDate.of(2026, 3, 15));

        assertThat(rows.get(0).principalDue()).isEqualByComparingTo("0.00");
        assertThat(rows.get(2).principalDue()).isEqualByComparingTo("1000.00");
        assertThat(rows.get(1).interestDue()).isEqualByComparingTo("10.00");
    }
}
