package io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.handlers;

import io.fireflysoftware.core.lending.servicing.sdk.api.LoanRateChangeApi;
import io.fireflysoftware.core.lending.servicing.sdk.model.FilterRequestLoanRateChangeDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.PaginationResponseLoanRateChangeDTO;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanRateChangesQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.QueryHandlerComponent;
import org.fireflyframework.cqrs.query.QueryHandler;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@QueryHandlerComponent
public class GetLoanRateChangesHandler extends QueryHandler<GetLoanRateChangesQuery, PaginationResponseLoanRateChangeDTO> {

    private final LoanRateChangeApi loanRateChangeApi;

    @Override
    protected Mono<PaginationResponseLoanRateChangeDTO> doHandle(GetLoanRateChangesQuery query) {
        log.debug("Listing rate changes for loan case: loanCaseId={}", query.getLoanCaseId());
        return loanRateChangeApi.findAllRateChanges(query.getLoanCaseId(), new FilterRequestLoanRateChangeDTO(), null);
    }
}
