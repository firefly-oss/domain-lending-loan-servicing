package io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.handlers;

import io.fireflysoftware.core.lending.servicing.sdk.api.LoanAccrualApi;
import io.fireflysoftware.core.lending.servicing.sdk.model.FilterRequestLoanAccrualDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.PaginationResponseLoanAccrualDTO;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanAccrualsQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.QueryHandlerComponent;
import org.fireflyframework.cqrs.query.QueryHandler;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@QueryHandlerComponent
public class GetLoanAccrualsHandler extends QueryHandler<GetLoanAccrualsQuery, PaginationResponseLoanAccrualDTO> {

    private final LoanAccrualApi loanAccrualApi;

    @Override
    protected Mono<PaginationResponseLoanAccrualDTO> doHandle(GetLoanAccrualsQuery query) {
        log.debug("Listing accruals for loan case: loanCaseId={}", query.getLoanCaseId());
        return loanAccrualApi.findAllAccruals(query.getLoanCaseId(), new FilterRequestLoanAccrualDTO(), null);
    }
}
