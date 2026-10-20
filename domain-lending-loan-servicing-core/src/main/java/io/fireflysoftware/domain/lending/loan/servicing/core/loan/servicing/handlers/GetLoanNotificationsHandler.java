package io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.handlers;

import io.fireflysoftware.core.lending.servicing.sdk.api.LoanNotificationApi;
import io.fireflysoftware.core.lending.servicing.sdk.model.FilterRequestLoanNotificationDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.PaginationResponse;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanNotificationsQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.QueryHandlerComponent;
import org.fireflyframework.cqrs.query.QueryHandler;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@QueryHandlerComponent
public class GetLoanNotificationsHandler extends QueryHandler<GetLoanNotificationsQuery, PaginationResponse> {

    private final LoanNotificationApi loanNotificationApi;

    @Override
    protected Mono<PaginationResponse> doHandle(GetLoanNotificationsQuery query) {
        log.debug("Listing notifications for loan case: loanCaseId={}", query.getLoanCaseId());
        return loanNotificationApi.findAllNotifications(query.getLoanCaseId(), new FilterRequestLoanNotificationDTO(), null);
    }
}
