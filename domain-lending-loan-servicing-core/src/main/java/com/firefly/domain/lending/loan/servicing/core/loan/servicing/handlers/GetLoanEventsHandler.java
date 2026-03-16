package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.core.lending.servicing.sdk.api.LoanServicingEventApi;
import com.firefly.core.lending.servicing.sdk.model.FilterRequestLoanServicingEventDTO;
import com.firefly.core.lending.servicing.sdk.model.PaginationResponseLoanServicingEventDTO;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanEventsQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.QueryHandlerComponent;
import org.fireflyframework.cqrs.query.QueryHandler;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@QueryHandlerComponent
public class GetLoanEventsHandler extends QueryHandler<GetLoanEventsQuery, PaginationResponseLoanServicingEventDTO> {

    private final LoanServicingEventApi loanServicingEventApi;

    @Override
    protected Mono<PaginationResponseLoanServicingEventDTO> doHandle(GetLoanEventsQuery query) {
        log.debug("Listing servicing events for loan case: loanCaseId={}", query.getLoanCaseId());
        return loanServicingEventApi.findAllServicingEvents(query.getLoanCaseId(), new FilterRequestLoanServicingEventDTO(), null);
    }
}
