/*
 * Copyright 2025 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.handlers;

import io.fireflysoftware.core.lending.servicing.sdk.api.LoanAccrualApi;
import io.fireflysoftware.core.lending.servicing.sdk.model.PaginationResponseLoanAccrualDTO;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.GetLoanAccrualsQuery;
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
