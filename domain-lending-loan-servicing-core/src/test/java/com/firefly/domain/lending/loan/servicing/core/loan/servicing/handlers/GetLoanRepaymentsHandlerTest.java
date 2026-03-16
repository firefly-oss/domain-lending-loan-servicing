package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.core.lending.servicing.sdk.api.LoanRepaymentRecordApi;
import com.firefly.core.lending.servicing.sdk.model.PaginationResponseLoanRepaymentRecordDTO;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanRepaymentsQuery;
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
class GetLoanRepaymentsHandlerTest {

    @Mock
    private LoanRepaymentRecordApi loanRepaymentRecordApi;

    private GetLoanRepaymentsHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetLoanRepaymentsHandler(loanRepaymentRecordApi);
    }

    @Test
    void shouldReturnRepaymentRecords() {
        UUID caseId = UUID.randomUUID();
        PaginationResponseLoanRepaymentRecordDTO response = new PaginationResponseLoanRepaymentRecordDTO();

        when(loanRepaymentRecordApi.findAllRepaymentRecords(eq(caseId), any(), isNull()))
                .thenReturn(Mono.just(response));

        StepVerifier.create(handler.doHandle(GetLoanRepaymentsQuery.builder().loanCaseId(caseId).build()))
                .expectNext(response)
                .verifyComplete();

        verify(loanRepaymentRecordApi).findAllRepaymentRecords(eq(caseId), any(), isNull());
    }
}
