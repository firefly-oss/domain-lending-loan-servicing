package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.core.lending.servicing.sdk.api.LoanServicingEventApi;
import com.firefly.core.lending.servicing.sdk.model.PaginationResponseLoanServicingEventDTO;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanEventsQuery;
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
class GetLoanEventsHandlerTest {

    @Mock
    private LoanServicingEventApi loanServicingEventApi;

    private GetLoanEventsHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetLoanEventsHandler(loanServicingEventApi);
    }

    @Test
    void shouldReturnServicingEvents() {
        UUID caseId = UUID.randomUUID();
        PaginationResponseLoanServicingEventDTO response = new PaginationResponseLoanServicingEventDTO();

        when(loanServicingEventApi.findAllServicingEvents(eq(caseId), any(), isNull()))
                .thenReturn(Mono.just(response));

        StepVerifier.create(handler.doHandle(GetLoanEventsQuery.builder().loanCaseId(caseId).build()))
                .expectNext(response)
                .verifyComplete();

        verify(loanServicingEventApi).findAllServicingEvents(eq(caseId), any(), isNull());
    }
}
