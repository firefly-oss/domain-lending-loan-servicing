package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.core.lending.servicing.sdk.api.LoanBalanceApi;
import com.firefly.core.lending.servicing.sdk.model.PaginationResponseLoanBalanceDTO;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanBalancesQuery;
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
class GetLoanBalancesHandlerTest {

    @Mock
    private LoanBalanceApi loanBalanceApi;

    private GetLoanBalancesHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetLoanBalancesHandler(loanBalanceApi);
    }

    @Test
    void shouldReturnBalances() {
        UUID caseId = UUID.randomUUID();
        PaginationResponseLoanBalanceDTO response = new PaginationResponseLoanBalanceDTO();

        when(loanBalanceApi.findAllBalances(eq(caseId), any(), isNull()))
                .thenReturn(Mono.just(response));

        StepVerifier.create(handler.doHandle(GetLoanBalancesQuery.builder().loanCaseId(caseId).build()))
                .expectNext(response)
                .verifyComplete();

        verify(loanBalanceApi).findAllBalances(eq(caseId), any(), isNull());
    }
}
