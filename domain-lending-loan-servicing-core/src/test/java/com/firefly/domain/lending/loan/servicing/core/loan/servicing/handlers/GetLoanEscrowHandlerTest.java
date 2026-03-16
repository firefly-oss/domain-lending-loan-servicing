package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.core.lending.servicing.sdk.api.LoanEscrowApi;
import com.firefly.core.lending.servicing.sdk.model.PaginationResponse;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanEscrowQuery;
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
class GetLoanEscrowHandlerTest {

    @Mock
    private LoanEscrowApi loanEscrowApi;

    private GetLoanEscrowHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetLoanEscrowHandler(loanEscrowApi);
    }

    @Test
    void shouldReturnEscrows() {
        UUID caseId = UUID.randomUUID();
        PaginationResponse response = new PaginationResponse();

        when(loanEscrowApi.findAllEscrows(eq(caseId), any(), isNull()))
                .thenReturn(Mono.just(response));

        StepVerifier.create(handler.doHandle(GetLoanEscrowQuery.builder().loanCaseId(caseId).build()))
                .expectNext(response)
                .verifyComplete();

        verify(loanEscrowApi).findAllEscrows(eq(caseId), any(), isNull());
    }
}
