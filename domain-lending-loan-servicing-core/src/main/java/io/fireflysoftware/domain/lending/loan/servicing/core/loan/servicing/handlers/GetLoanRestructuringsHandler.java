package io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.handlers;

import io.fireflysoftware.core.lending.servicing.sdk.api.LoanRestructuringApi;
import io.fireflysoftware.core.lending.servicing.sdk.model.FilterRequestLoanRestructuringDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.PaginationResponseLoanRestructuringDTO;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanRestructuringsQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.QueryHandlerComponent;
import org.fireflyframework.cqrs.query.QueryHandler;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@QueryHandlerComponent
public class GetLoanRestructuringsHandler extends QueryHandler<GetLoanRestructuringsQuery, PaginationResponseLoanRestructuringDTO> {

    private final LoanRestructuringApi loanRestructuringApi;

    @Override
    protected Mono<PaginationResponseLoanRestructuringDTO> doHandle(GetLoanRestructuringsQuery query) {
        log.debug("Listing restructurings for loan case: loanCaseId={}", query.getLoanCaseId());
        return loanRestructuringApi.findAllRestructurings(query.getLoanCaseId(), new FilterRequestLoanRestructuringDTO(), null);
    }
}
