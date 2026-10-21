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

import io.fireflysoftware.core.lending.servicing.sdk.api.LoanAccrualApi;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanRepaymentScheduleApi;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanServicingCaseApi;
import io.fireflysoftware.core.lending.servicing.sdk.model.FilterRequestLoanAccrualDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.FilterRequestLoanRepaymentScheduleDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.FilterRequestLoanServicingCaseDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.LoanAccrualDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.LoanRepaymentScheduleDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.LoanServicingCaseDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.PaginationRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Accrues interest for open loans and posts one general-ledger journal per business date.
 * A second run for that date does not accrue again and does not post again.
 */
@Service
public class LoanEndOfDayService {

    private static final Set<LoanServicingCaseDTO.ServicingStatusEnum> OPEN = EnumSet.of(
            LoanServicingCaseDTO.ServicingStatusEnum.ACTIVE,
            LoanServicingCaseDTO.ServicingStatusEnum.GRACE_PERIOD,
            LoanServicingCaseDTO.ServicingStatusEnum.DELINQUENT,
            LoanServicingCaseDTO.ServicingStatusEnum.DEFAULT,
            LoanServicingCaseDTO.ServicingStatusEnum.FORBEARANCE,
            LoanServicingCaseDTO.ServicingStatusEnum.RESTRUCTURED,
            LoanServicingCaseDTO.ServicingStatusEnum.BANKRUPTCY,
            LoanServicingCaseDTO.ServicingStatusEnum.FORECLOSURE);

    private final LoanServicingCaseApi cases;
    private final LoanAccrualApi accruals;
    private final LoanRepaymentScheduleApi schedules;
    private final LoanPricePort prices;
    private final GeneralLedgerPort ledger;

    public LoanEndOfDayService(
            LoanServicingCaseApi cases,
            LoanAccrualApi accruals,
            LoanRepaymentScheduleApi schedules,
            LoanPricePort prices,
            GeneralLedgerPort ledger) {
        this.cases = cases;
        this.accruals = accruals;
        this.schedules = schedules;
        this.prices = prices;
        this.ledger = ledger;
    }

    public Mono<EndOfDayResult> run(LocalDate businessDate) {
        if (businessDate == null) {
            return Mono.error(new IllegalArgumentException("Business date is required"));
        }
        return openLoans()
                .flatMap(loans -> Flux.fromIterable(loans)
                        .concatMap(loan -> accrue(loan, businessDate))
                        .collectList()
                        .flatMap(rows -> post(businessDate, rows)));
    }

    private Mono<List<LoanServicingCaseDTO>> openLoans() {
        return page(0, new ArrayList<>())
                .map(rows -> rows.stream().filter(loan -> OPEN.contains(loan.getServicingStatus())).toList());
    }

    private Mono<List<LoanServicingCaseDTO>> page(int number, List<LoanServicingCaseDTO> accumulated) {
        FilterRequestLoanServicingCaseDTO filter = new FilterRequestLoanServicingCaseDTO()
                .pagination(new PaginationRequest().pageNumber(number).pageSize(200));
        return cases.findAllServicingCases(filter, null)
                .flatMap(response -> {
                    if (response != null && response.getContent() != null) {
                        accumulated.addAll(response.getContent());
                    }
                    int pages = response == null || response.getTotalPages() == null ? 1 : response.getTotalPages();
                    if (number + 1 >= pages) {
                        return Mono.just(accumulated);
                    }
                    return page(number + 1, accumulated);
                });
    }

    private Mono<Accrued> accrue(LoanServicingCaseDTO loan, LocalDate businessDate) {
        if (loan.getInterestRate() == null || loan.getProductId() == null
                || loan.getPrincipalAmount() == null || loan.getLoanTerm() == null) {
            return Mono.empty();
        }
        return accruals.findAllAccruals(loan.getLoanServicingCaseId(), new FilterRequestLoanAccrualDTO()
                        .pagination(new PaginationRequest().pageNumber(0).pageSize(200)), null)
                .flatMap(page -> {
                    if (page != null && page.getContent() != null) {
                        for (LoanAccrualDTO row : page.getContent()) {
                            if (row.getAccrualType() == LoanAccrualDTO.AccrualTypeEnum.INTEREST
                                    && businessDate.equals(row.getAccrualDate())) {
                                return currency(loan).map(code -> new Accrued(row.getAccrualAmount(), code));
                            }
                        }
                    }
                    return createAccrual(loan, businessDate);
                });
    }

    private Mono<Accrued> createAccrual(LoanServicingCaseDTO loan, LocalDate businessDate) {
        return outstanding(loan)
                .flatMap(principal -> currency(loan).flatMap(code -> {
                    BigDecimal interest = dailyInterest(principal, loan.getInterestRate(), loan.getDayCountConvention(), businessDate);
                    if (interest.signum() <= 0) {
                        return Mono.empty();
                    }
                    LoanAccrualDTO accrual = new LoanAccrualDTO()
                            .loanServicingCaseId(loan.getLoanServicingCaseId())
                            .accrualAmount(interest)
                            .accrualType(LoanAccrualDTO.AccrualTypeEnum.INTEREST)
                            .accrualDate(businessDate)
                            .note("End of day interest");
                    String key = "eod-" + loan.getLoanServicingCaseId() + "-" + businessDate;
                    return accruals.createAccrual(loan.getLoanServicingCaseId(), accrual, key)
                            .thenReturn(new Accrued(interest, code));
                }));
    }

    private Mono<BigDecimal> outstanding(LoanServicingCaseDTO loan) {
        return schedules.findAllRepaymentSchedules(loan.getLoanServicingCaseId(),
                        new FilterRequestLoanRepaymentScheduleDTO()
                                .pagination(new PaginationRequest().pageNumber(0).pageSize(500)), null)
                .map(page -> {
                    if (page == null || page.getContent() == null || page.getContent().isEmpty()) {
                        return loan.getPrincipalAmount();
                    }
                    BigDecimal unpaid = BigDecimal.ZERO;
                    boolean any = false;
                    for (LoanRepaymentScheduleDTO row : page.getContent()) {
                        if (!Boolean.TRUE.equals(row.getIsPaid()) && row.getPrincipalDue() != null) {
                            unpaid = unpaid.add(row.getPrincipalDue());
                            any = true;
                        }
                    }
                    return any ? unpaid : BigDecimal.ZERO;
                });
    }

    private Mono<String> currency(LoanServicingCaseDTO loan) {
        return prices.price(loan.getPrincipalAmount(), loan.getLoanTerm(), loan.getProductId())
                .map(LoanPrice::currency);
    }

    private Mono<EndOfDayResult> post(LocalDate businessDate, List<Accrued> rows) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (Accrued row : rows) {
            totals.merge(row.currency, row.amount, BigDecimal::add);
        }
        if (totals.isEmpty()) {
            return Mono.just(new EndOfDayResult(businessDate, List.of()));
        }
        return Flux.fromIterable(totals.entrySet())
                .concatMap(entry -> ledger.findInterest(businessDate)
                        .filter(journal -> entry.getKey().equals(journal.currency()))
                        .switchIfEmpty(Mono.defer(() -> ledger.postInterest(businessDate, entry.getValue(), entry.getKey()))))
                .collectList()
                .map(journals -> new EndOfDayResult(businessDate, journals));
    }

    static BigDecimal dailyInterest(
            BigDecimal principal,
            BigDecimal annualRatePercent,
            LoanServicingCaseDTO.DayCountConventionEnum convention,
            LocalDate businessDate) {
        if (principal == null || principal.signum() <= 0 || annualRatePercent == null || annualRatePercent.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        int basis = switch (convention == null ? LoanServicingCaseDTO.DayCountConventionEnum.ACTUAL_365 : convention) {
            case ACTUAL_360, THIRTY_360 -> 360;
            case ACTUAL_ACTUAL -> businessDate.lengthOfYear();
            case ACTUAL_365 -> 365;
        };
        return principal.multiply(annualRatePercent)
                .divide(BigDecimal.valueOf(100L * basis), 2, RoundingMode.HALF_UP);
    }

    private record Accrued(BigDecimal amount, String currency) {
    }
}
