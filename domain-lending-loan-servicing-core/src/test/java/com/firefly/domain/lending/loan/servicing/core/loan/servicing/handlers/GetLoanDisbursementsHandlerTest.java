package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.core.lending.servicing.sdk.api.LoanDisbursementApi;
import com.firefly.core.lending.servicing.sdk.model.PaginationResponseLoanDisbursementDTO;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanDisbursementsQuery;
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
class GetLoanDisbursementsHandlerTest {

    @Mock
    private LoanDisbursementApi loanDisbursementApi;

    private GetLoanDisbursementsHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetLoanDisbursementsHandler(loanDisbursementApi);
    }

    @Test
    void shouldReturnDisbursements() {
        UUID caseId = UUID.randomUUID();
        PaginationResponseLoanDisbursementDTO response = new PaginationResponseLoanDisbursementDTO();

        when(loanDisbursementApi.findAllDisbursements(eq(caseId), any(), isNull()))
                .thenReturn(Mono.just(response));

        StepVerifier.create(handler.doHandle(GetLoanDisbursementsQuery.builder().loanCaseId(caseId).build()))
                .expectNext(response)
                .verifyComplete();

        verify(loanDisbursementApi).findAllDisbursements(eq(caseId), any(), isNull());
    }
}
