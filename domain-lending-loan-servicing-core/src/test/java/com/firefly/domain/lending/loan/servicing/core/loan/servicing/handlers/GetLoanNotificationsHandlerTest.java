package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.core.lending.servicing.sdk.api.LoanNotificationApi;
import com.firefly.core.lending.servicing.sdk.model.PaginationResponse;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanNotificationsQuery;
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
class GetLoanNotificationsHandlerTest {

    @Mock
    private LoanNotificationApi loanNotificationApi;

    private GetLoanNotificationsHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetLoanNotificationsHandler(loanNotificationApi);
    }

    @Test
    void shouldReturnNotifications() {
        UUID caseId = UUID.randomUUID();
        PaginationResponse response = new PaginationResponse();

        when(loanNotificationApi.findAllNotifications(eq(caseId), any(), isNull()))
                .thenReturn(Mono.just(response));

        StepVerifier.create(handler.doHandle(GetLoanNotificationsQuery.builder().loanCaseId(caseId).build()))
                .expectNext(response)
                .verifyComplete();

        verify(loanNotificationApi).findAllNotifications(eq(caseId), any(), isNull());
    }
}
