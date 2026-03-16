package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.core.lending.servicing.sdk.api.LoanRestructuringApi;
import com.firefly.core.lending.servicing.sdk.model.PaginationResponseLoanRestructuringDTO;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanRestructuringsQuery;
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
class GetLoanRestructuringsHandlerTest {

    @Mock
    private LoanRestructuringApi loanRestructuringApi;

    private GetLoanRestructuringsHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetLoanRestructuringsHandler(loanRestructuringApi);
    }

    @Test
    void shouldReturnRestructurings() {
        UUID caseId = UUID.randomUUID();
        PaginationResponseLoanRestructuringDTO response = new PaginationResponseLoanRestructuringDTO();

        when(loanRestructuringApi.findAllRestructurings(eq(caseId), any(), isNull()))
                .thenReturn(Mono.just(response));

        StepVerifier.create(handler.doHandle(GetLoanRestructuringsQuery.builder().loanCaseId(caseId).build()))
                .expectNext(response)
                .verifyComplete();

        verify(loanRestructuringApi).findAllRestructurings(eq(caseId), any(), isNull());
    }
}
