package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.core.lending.servicing.sdk.api.LoanRebateApi;
import com.firefly.core.lending.servicing.sdk.model.FilterRequestLoanRebateDTO;
import com.firefly.core.lending.servicing.sdk.model.PaginationResponse;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanRebatesQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.QueryHandlerComponent;
import org.fireflyframework.cqrs.query.QueryHandler;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@QueryHandlerComponent
public class GetLoanRebatesHandler extends QueryHandler<GetLoanRebatesQuery, PaginationResponse> {

    private final LoanRebateApi loanRebateApi;

    @Override
    protected Mono<PaginationResponse> doHandle(GetLoanRebatesQuery query) {
        log.debug("Listing rebates for loan case: loanCaseId={}", query.getLoanCaseId());
        return loanRebateApi.findAllRebates(query.getLoanCaseId(), new FilterRequestLoanRebateDTO(), null);
    }
}
