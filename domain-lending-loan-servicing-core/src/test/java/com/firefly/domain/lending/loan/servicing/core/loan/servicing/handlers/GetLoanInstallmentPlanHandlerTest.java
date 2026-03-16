package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.core.lending.servicing.sdk.api.LoanInstallmentPlanApi;
import com.firefly.core.lending.servicing.sdk.model.PaginationResponseLoanInstallmentPlanDTO;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanInstallmentPlanQuery;
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
class GetLoanInstallmentPlanHandlerTest {

    @Mock
    private LoanInstallmentPlanApi loanInstallmentPlanApi;

    private GetLoanInstallmentPlanHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetLoanInstallmentPlanHandler(loanInstallmentPlanApi);
    }

    @Test
    void shouldReturnInstallmentPlans() {
        UUID caseId = UUID.randomUUID();
        PaginationResponseLoanInstallmentPlanDTO response = new PaginationResponseLoanInstallmentPlanDTO();

        when(loanInstallmentPlanApi.findAllInstallmentPlans(eq(caseId), any(), isNull()))
                .thenReturn(Mono.just(response));

        StepVerifier.create(handler.doHandle(GetLoanInstallmentPlanQuery.builder().loanCaseId(caseId).build()))
                .expectNext(response)
                .verifyComplete();

        verify(loanInstallmentPlanApi).findAllInstallmentPlans(eq(caseId), any(), isNull());
    }
}
