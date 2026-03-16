package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.core.lending.servicing.sdk.api.LoanRepaymentScheduleApi;
import com.firefly.core.lending.servicing.sdk.model.PaginationResponseLoanRepaymentScheduleDTO;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanScheduleQuery;
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
class GetLoanScheduleHandlerTest {

    @Mock
    private LoanRepaymentScheduleApi loanRepaymentScheduleApi;

    private GetLoanScheduleHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetLoanScheduleHandler(loanRepaymentScheduleApi);
    }

    @Test
    void shouldReturnRepaymentSchedules() {
        UUID caseId = UUID.randomUUID();
        PaginationResponseLoanRepaymentScheduleDTO response = new PaginationResponseLoanRepaymentScheduleDTO();

        when(loanRepaymentScheduleApi.findAllRepaymentSchedules(eq(caseId), any(), isNull()))
                .thenReturn(Mono.just(response));

        StepVerifier.create(handler.doHandle(GetLoanScheduleQuery.builder().loanCaseId(caseId).build()))
                .expectNext(response)
                .verifyComplete();

        verify(loanRepaymentScheduleApi).findAllRepaymentSchedules(eq(caseId), any(), isNull());
    }
}
