package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.core.lending.servicing.sdk.api.LoanAccrualApi;
import com.firefly.core.lending.servicing.sdk.model.PaginationResponseLoanAccrualDTO;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanAccrualsQuery;
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
class GetLoanAccrualsHandlerTest {

    @Mock
    private LoanAccrualApi loanAccrualApi;

    private GetLoanAccrualsHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetLoanAccrualsHandler(loanAccrualApi);
    }

    @Test
    void shouldReturnAccruals() {
        UUID caseId = UUID.randomUUID();
        PaginationResponseLoanAccrualDTO response = new PaginationResponseLoanAccrualDTO();

        when(loanAccrualApi.findAllAccruals(eq(caseId), any(), isNull()))
                .thenReturn(Mono.just(response));

        StepVerifier.create(handler.doHandle(GetLoanAccrualsQuery.builder().loanCaseId(caseId).build()))
                .expectNext(response)
                .verifyComplete();

        verify(loanAccrualApi).findAllAccruals(eq(caseId), any(), isNull());
    }
}
