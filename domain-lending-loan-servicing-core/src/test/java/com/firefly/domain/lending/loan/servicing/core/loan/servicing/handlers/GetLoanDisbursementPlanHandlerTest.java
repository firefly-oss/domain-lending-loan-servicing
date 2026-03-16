package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.core.lending.servicing.sdk.api.LoanDisbursementPlanApi;
import com.firefly.core.lending.servicing.sdk.model.PaginationResponse;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanDisbursementPlanQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetLoanDisbursementPlanHandlerTest {

    @Mock
    private LoanDisbursementPlanApi loanDisbursementPlanApi;

    private GetLoanDisbursementPlanHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetLoanDisbursementPlanHandler(loanDisbursementPlanApi);
    }

    @Test
    void shouldReturnDisbursementPlans() {
        UUID caseId = UUID.randomUUID();
        PaginationResponse response = new PaginationResponse();

        when(loanDisbursementPlanApi.findAllDisbursementPlans(eq(caseId), any(), isNull()))
                .thenReturn(Mono.just(response));

        StepVerifier.create(handler.doHandle(GetLoanDisbursementPlanQuery.builder().loanCaseId(caseId).build()))
                .expectNext(response)
                .verifyComplete();

        verify(loanDisbursementPlanApi).findAllDisbursementPlans(eq(caseId), any(), isNull());
    }
}
