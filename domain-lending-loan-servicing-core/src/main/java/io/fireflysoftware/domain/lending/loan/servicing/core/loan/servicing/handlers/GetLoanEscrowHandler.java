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

import io.fireflysoftware.core.lending.servicing.sdk.api.LoanEscrowApi;
import io.fireflysoftware.core.lending.servicing.sdk.model.FilterRequestLoanEscrowDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.PaginationResponse;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanEscrowQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.QueryHandlerComponent;
import org.fireflyframework.cqrs.query.QueryHandler;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@QueryHandlerComponent
public class GetLoanEscrowHandler extends QueryHandler<GetLoanEscrowQuery, PaginationResponse> {

    private final LoanEscrowApi loanEscrowApi;

    @Override
    protected Mono<PaginationResponse> doHandle(GetLoanEscrowQuery query) {
        log.debug("Listing escrows for loan case: loanCaseId={}", query.getLoanCaseId());
        return loanEscrowApi.findAllEscrows(query.getLoanCaseId(), new FilterRequestLoanEscrowDTO(), null);
    }
}
