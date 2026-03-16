package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.core.lending.servicing.sdk.api.LoanInstallmentPlanApi;
import com.firefly.core.lending.servicing.sdk.model.FilterRequestLoanInstallmentPlanDTO;
import com.firefly.core.lending.servicing.sdk.model.PaginationResponseLoanInstallmentPlanDTO;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanInstallmentPlanQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.QueryHandlerComponent;
import org.fireflyframework.cqrs.query.QueryHandler;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@QueryHandlerComponent
public class GetLoanInstallmentPlanHandler extends QueryHandler<GetLoanInstallmentPlanQuery, PaginationResponseLoanInstallmentPlanDTO> {

    private final LoanInstallmentPlanApi loanInstallmentPlanApi;

    @Override
    protected Mono<PaginationResponseLoanInstallmentPlanDTO> doHandle(GetLoanInstallmentPlanQuery query) {
        log.debug("Listing installment plans for loan case: loanCaseId={}", query.getLoanCaseId());
        return loanInstallmentPlanApi.findAllInstallmentPlans(query.getLoanCaseId(), new FilterRequestLoanInstallmentPlanDTO(), null);
    }
}
