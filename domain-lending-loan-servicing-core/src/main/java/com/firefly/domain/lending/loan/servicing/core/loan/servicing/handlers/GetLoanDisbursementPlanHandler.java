package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.core.lending.servicing.sdk.api.LoanDisbursementPlanApi;
import com.firefly.core.lending.servicing.sdk.model.FilterRequestLoanDisbursementPlanDTO;
import com.firefly.core.lending.servicing.sdk.model.PaginationResponse;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanDisbursementPlanQuery;
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
