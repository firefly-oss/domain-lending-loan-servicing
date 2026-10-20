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
