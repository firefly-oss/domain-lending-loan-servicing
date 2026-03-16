package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.core.lending.servicing.sdk.api.LoanEscrowApi;
import com.firefly.core.lending.servicing.sdk.model.FilterRequestLoanEscrowDTO;
import com.firefly.core.lending.servicing.sdk.model.PaginationResponse;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanEscrowQuery;
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
