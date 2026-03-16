package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.core.lending.servicing.sdk.api.LoanInstallmentRecordApi;
import com.firefly.core.lending.servicing.sdk.model.FilterRequestLoanInstallmentRecordDTO;
import com.firefly.core.lending.servicing.sdk.model.PaginationResponseLoanInstallmentRecordDTO;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanInstallmentRecordsQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.QueryHandlerComponent;
import org.fireflyframework.cqrs.query.QueryHandler;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@QueryHandlerComponent
public class GetLoanInstallmentRecordsHandler extends QueryHandler<GetLoanInstallmentRecordsQuery, PaginationResponseLoanInstallmentRecordDTO> {

    private final LoanInstallmentRecordApi loanInstallmentRecordApi;

    @Override
    protected Mono<PaginationResponseLoanInstallmentRecordDTO> doHandle(GetLoanInstallmentRecordsQuery query) {
        log.debug("Listing installment records for loan case: loanCaseId={}", query.getLoanCaseId());
        return loanInstallmentRecordApi.findAllInstallmentRecords(query.getLoanCaseId(), new FilterRequestLoanInstallmentRecordDTO(), null);
    }
}
