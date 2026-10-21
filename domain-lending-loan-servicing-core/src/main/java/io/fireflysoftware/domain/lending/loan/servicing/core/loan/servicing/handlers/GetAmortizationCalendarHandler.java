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

import io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.LoanCalendarPort;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.ScheduleState;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.StoredInstallment;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.GetAmortizationCalendarQuery;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.AmortizationCalendarEntry;
import org.fireflyframework.cqrs.annotations.QueryHandlerComponent;
import org.fireflyframework.cqrs.query.QueryHandler;
import reactor.core.publisher.Mono;

import java.util.List;

@QueryHandlerComponent
public class GetAmortizationCalendarHandler extends QueryHandler<GetAmortizationCalendarQuery, List<AmortizationCalendarEntry>> {

    private final LoanCalendarPort calendars;

    public GetAmortizationCalendarHandler(LoanCalendarPort calendars) {
        this.calendars = calendars;
    }

    @Override
    protected Mono<List<AmortizationCalendarEntry>> doHandle(GetAmortizationCalendarQuery query) {
        return calendars.read(query.getLoanCaseId())
                .map(rows -> rows.stream()
                        .filter(row -> row.status() == ScheduleState.CURRENT)
                        .map(GetAmortizationCalendarHandler::entry)
                        .toList());
    }

    private static AmortizationCalendarEntry entry(StoredInstallment row) {
        return new AmortizationCalendarEntry(
                row.number(),
                row.dueDate(),
                row.principalDue(),
                row.interestDue(),
                row.remainingBalance());
    }
}
