package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.core.lending.servicing.sdk.api.LoanRateChangeApi;
import com.firefly.core.lending.servicing.sdk.model.PaginationResponseLoanRateChangeDTO;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanRateChangesQuery;
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
class GetLoanRateChangesHandlerTest {

    @Mock
    private LoanRateChangeApi loanRateChangeApi;

    private GetLoanRateChangesHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetLoanRateChangesHandler(loanRateChangeApi);
    }

    @Test
    void shouldReturnRateChanges() {
        UUID caseId = UUID.randomUUID();
        PaginationResponseLoanRateChangeDTO response = new PaginationResponseLoanRateChangeDTO();

        when(loanRateChangeApi.findAllRateChanges(eq(caseId), any(), isNull()))
                .thenReturn(Mono.just(response));

        StepVerifier.create(handler.doHandle(GetLoanRateChangesQuery.builder().loanCaseId(caseId).build()))
                .expectNext(response)
                .verifyComplete();

        verify(loanRateChangeApi).findAllRateChanges(eq(caseId), any(), isNull());
    }
}
