package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.core.lending.servicing.sdk.api.LoanDisbursementApi;
import com.firefly.core.lending.servicing.sdk.model.FilterRequestLoanDisbursementDTO;
import com.firefly.core.lending.servicing.sdk.model.PaginationResponseLoanDisbursementDTO;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanDisbursementsQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.QueryHandlerComponent;
import org.fireflyframework.cqrs.query.QueryHandler;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@QueryHandlerComponent
public class GetLoanDisbursementsHandler extends QueryHandler<GetLoanDisbursementsQuery, PaginationResponseLoanDisbursementDTO> {

    private final LoanDisbursementApi loanDisbursementApi;

    @Override
    protected Mono<PaginationResponseLoanDisbursementDTO> doHandle(GetLoanDisbursementsQuery query) {
        log.debug("Listing disbursements for loan case: loanCaseId={}", query.getLoanCaseId());
        return loanDisbursementApi.findAllDisbursements(query.getLoanCaseId(), new FilterRequestLoanDisbursementDTO(), null);
    }
}
