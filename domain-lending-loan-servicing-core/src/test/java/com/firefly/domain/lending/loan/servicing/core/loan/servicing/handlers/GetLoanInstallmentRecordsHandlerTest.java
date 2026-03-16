package com.firefly.domain.lending.loan.servicing.core.loan.servicing.handlers;

import com.firefly.core.lending.servicing.sdk.api.LoanInstallmentRecordApi;
import com.firefly.core.lending.servicing.sdk.model.PaginationResponseLoanInstallmentRecordDTO;
import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanInstallmentRecordsQuery;
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
class GetLoanInstallmentRecordsHandlerTest {

    @Mock
    private LoanInstallmentRecordApi loanInstallmentRecordApi;

    private GetLoanInstallmentRecordsHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetLoanInstallmentRecordsHandler(loanInstallmentRecordApi);
    }

    @Test
    void shouldReturnInstallmentRecords() {
        UUID caseId = UUID.randomUUID();
        PaginationResponseLoanInstallmentRecordDTO response = new PaginationResponseLoanInstallmentRecordDTO();

        when(loanInstallmentRecordApi.findAllInstallmentRecords(eq(caseId), any(), isNull()))
                .thenReturn(Mono.just(response));

        StepVerifier.create(handler.doHandle(GetLoanInstallmentRecordsQuery.builder().loanCaseId(caseId).build()))
                .expectNext(response)
                .verifyComplete();

        verify(loanInstallmentRecordApi).findAllInstallmentRecords(eq(caseId), any(), isNull());
    }
}
