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


package io.fireflysoftware.domain.lending.loan.servicing.core.syndicate;

import io.fireflysoftware.core.lending.servicing.sdk.api.SyndicatedFacilityApi;
import io.fireflysoftware.core.lending.servicing.sdk.model.FacilityAllocationDTO;
import io.fireflysoftware.core.lending.servicing.sdk.model.SyndicatedFacilityDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyndicateRepaymentServiceTest {

    @Mock
    private SyndicatedFacilityApi facilities;

    @Test
    void repaymentUsesThePaymentsPortAndCompensationCancelsIt() {
        UUID caseId = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        UUID allocationId = UUID.randomUUID();
        SandboxPaymentTccAdapter payments = new SandboxPaymentTccAdapter();
        SyndicateRepaymentService service = new SyndicateRepaymentService(facilities, payments);
        FacilityAllocationDTO allocation = new FacilityAllocationDTO()
                .allocationId(allocationId)
                .facilityId(facilityId)
                .paymentAmount(new BigDecimal("1000.00"))
                .unallocatedAmount(new BigDecimal("500.00"))
                .currency("USD");
        when(facilities.allocate(eq(caseId), eq(facilityId), any(), any())).thenReturn(Mono.just(allocation));
        when(facilities.attachPayment(eq(caseId), eq(facilityId), eq(allocationId), any(), any()))
                .thenReturn(Mono.just(allocation));
        when(facilities.deleteAllocation(eq(caseId), eq(facilityId), eq(allocationId), any())).thenReturn(Mono.empty());

        SyndicatePaymentReceipt receipt = service.allocateAndPay(caseId, facilityId, UUID.randomUUID(),
                new BigDecimal("1000.00"), "USD", "repay-1").block();

        assertThat(receipt.paymentId()).isNotNull();
        assertThat(receipt.allocationId()).isEqualTo(allocationId);

        AllocateSyndicateRepaymentSaga saga = new AllocateSyndicateRepaymentSaga(service);
        saga.compensatePayment(receipt).block();
        verify(facilities).deleteAllocation(eq(caseId), eq(facilityId), eq(allocationId), any());
    }

    @Test
    void borrowerViewOmitsParticipants() {
        UUID loanId = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        SandboxPaymentTccAdapter payments = new SandboxPaymentTccAdapter();
        SyndicateRepaymentService service = new SyndicateRepaymentService(facilities, payments);
        SyndicatedFacilityDTO facility = new SyndicatedFacilityDTO()
                .facilityId(facilityId)
                .loanServicingCaseId(loanId)
                .currency("USD")
                .commitmentAmount(new BigDecimal("250000.00"))
                .status(SyndicatedFacilityDTO.StatusEnum.OPEN);
        when(facilities.listSyndicatedFacilities(eq(loanId), any())).thenReturn(Flux.just(facility));

        BorrowerFacilityView view = service.viewForBorrower(loanId).block();
        assertThat(view.facilityId()).isEqualTo(facilityId);
        assertThat(view.currency()).isEqualTo("USD");
        assertThat(view.commitmentAmount()).isEqualByComparingTo("250000.00");
        assertThat(view.status()).isEqualTo("OPEN");
        assertThat(view.getClass().getRecordComponents()).extracting(component -> component.getName())
                .doesNotContain("participants");
    }
}
