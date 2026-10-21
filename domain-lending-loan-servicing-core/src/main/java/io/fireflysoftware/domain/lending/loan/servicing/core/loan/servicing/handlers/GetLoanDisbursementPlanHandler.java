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

import io.fireflysoftware.core.lending.servicing.sdk.api.LoanDisbursementPlanApi;
import io.fireflysoftware.core.lending.servicing.sdk.model.FilterRequestLoanDisbursementPlanDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.PaginationResponse;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanDisbursementPlanQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.QueryHandlerComponent;
import org.fireflyframework.cqrs.query.QueryHandler;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@QueryHandlerComponent
public class GetLoanDisbursementPlanHandler extends QueryHandler<GetLoanDisbursementPlanQuery, PaginationResponse> {

    private final LoanDisbursementPlanApi loanDisbursementPlanApi;

    @Override
    protected Mono<PaginationResponse> doHandle(GetLoanDisbursementPlanQuery query) {
        log.debug("Listing disbursement plans for loan case: loanCaseId={}", query.getLoanCaseId());
        return loanDisbursementPlanApi.findAllDisbursementPlans(query.getLoanCaseId(), new FilterRequestLoanDisbursementPlanDTO(), null);
    }
}
