package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.core.lending.servicing.sdk.api.LoanRepaymentScheduleApi;
import com.firefly.core.lending.servicing.sdk.model.FilterRequestLoanRepaymentScheduleDTO;
import com.firefly.core.lending.servicing.sdk.model.PaginationResponseLoanRepaymentScheduleDTO;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanScheduleQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.QueryHandlerComponent;
import org.fireflyframework.cqrs.query.QueryHandler;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@QueryHandlerComponent
public class GetLoanScheduleHandler extends QueryHandler<GetLoanScheduleQuery, PaginationResponseLoanRepaymentScheduleDTO> {

    private final LoanRepaymentScheduleApi loanRepaymentScheduleApi;

    @Override
    protected Mono<PaginationResponseLoanRepaymentScheduleDTO> doHandle(GetLoanScheduleQuery query) {
        log.debug("Listing repayment schedules for loan case: loanCaseId={}", query.getLoanCaseId());
        return loanRepaymentScheduleApi.findAllRepaymentSchedules(query.getLoanCaseId(), new FilterRequestLoanRepaymentScheduleDTO(), null);
    }
}
