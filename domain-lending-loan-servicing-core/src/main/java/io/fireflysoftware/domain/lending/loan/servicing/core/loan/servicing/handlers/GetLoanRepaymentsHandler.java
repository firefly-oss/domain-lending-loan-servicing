package io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.handlers;

import io.fireflysoftware.core.lending.servicing.sdk.api.LoanRepaymentRecordApi;
import io.fireflysoftware.core.lending.servicing.sdk.model.FilterRequestLoanRepaymentRecordDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.PaginationResponseLoanRepaymentRecordDTO;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanRepaymentsQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.QueryHandlerComponent;
import org.fireflyframework.cqrs.query.QueryHandler;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@QueryHandlerComponent
public class GetLoanRepaymentsHandler extends QueryHandler<GetLoanRepaymentsQuery, PaginationResponseLoanRepaymentRecordDTO> {

    private final LoanRepaymentRecordApi loanRepaymentRecordApi;

    @Override
    protected Mono<PaginationResponseLoanRepaymentRecordDTO> doHandle(GetLoanRepaymentsQuery query) {
        log.debug("Listing repayment records for loan case: loanCaseId={}", query.getLoanCaseId());
        return loanRepaymentRecordApi.findAllRepaymentRecords(query.getLoanCaseId(), new FilterRequestLoanRepaymentRecordDTO(), null);
    }
}
