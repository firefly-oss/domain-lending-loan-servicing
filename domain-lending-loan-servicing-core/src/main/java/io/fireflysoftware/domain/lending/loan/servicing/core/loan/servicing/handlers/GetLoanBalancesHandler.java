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

import io.fireflysoftware.core.lending.servicing.sdk.api.LoanBalanceApi;
import io.fireflysoftware.core.lending.servicing.sdk.model.FilterRequestLoanBalanceDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.PaginationResponseLoanBalanceDTO;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanBalancesQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.QueryHandlerComponent;
import org.fireflyframework.cqrs.query.QueryHandler;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@QueryHandlerComponent
public class GetLoanBalancesHandler extends QueryHandler<GetLoanBalancesQuery, PaginationResponseLoanBalanceDTO> {

    private final LoanBalanceApi loanBalanceApi;

    @Override
    protected Mono<PaginationResponseLoanBalanceDTO> doHandle(GetLoanBalancesQuery query) {
        log.debug("Listing balances for loan case: loanCaseId={}", query.getLoanCaseId());
        return loanBalanceApi.findAllBalances(query.getLoanCaseId(), new FilterRequestLoanBalanceDTO(), null);
    }
}
