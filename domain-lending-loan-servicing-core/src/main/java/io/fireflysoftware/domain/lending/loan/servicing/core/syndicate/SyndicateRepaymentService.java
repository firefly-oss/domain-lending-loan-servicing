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
import io.fireflysoftware.core.lending.servicing.sdk.model.AllocateFacilityRepaymentRequest;
import io.fireflysoftware.core.lending.servicing.sdk.model.AttachFacilityPaymentRequest;
import io.fireflysoftware.core.lending.servicing.sdk.model.SyndicatedFacilityDTO;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Allocates a syndicated repayment through the servicing core SDK, then submits the money
 * through {@link PaymentTccPort}. Compensation cancels the payment and deletes the allocation.
 */
@Service
public class SyndicateRepaymentService {

    private final SyndicatedFacilityApi facilities;
    private final PaymentTccPort payments;

    public SyndicateRepaymentService(SyndicatedFacilityApi facilities, PaymentTccPort payments) {
        this.facilities = facilities;
        this.payments = payments;
    }

    public Mono<BorrowerFacilityView> viewForBorrower(UUID loanId) {
        return facilities.listSyndicatedFacilities(loanId, UUID.randomUUID().toString())
                .next()
                .map(this::borrowerView);
    }

    public Mono<SyndicatePaymentReceipt> allocateAndPay(UUID caseId, UUID facilityId, UUID repaymentId,
                                                        BigDecimal amount, String currency, String idempotencyKey) {
        AllocateFacilityRepaymentRequest request = new AllocateFacilityRepaymentRequest()
                .loanRepaymentRecordId(repaymentId)
                .paymentAmount(amount)
                .currency(currency);
        String key = idempotencyKey == null ? UUID.randomUUID().toString() : idempotencyKey;
        return facilities.allocate(caseId, facilityId, request, key)
                .flatMap(allocation -> payments.tryPayment(allocation.getPaymentAmount(), allocation.getCurrency(), key)
                        .flatMap(paymentId -> payments.confirm(paymentId)
                                .flatMap(confirmed -> {
                                    AttachFacilityPaymentRequest attach = new AttachFacilityPaymentRequest().paymentId(confirmed);
                                    return facilities.attachPayment(caseId, facilityId, allocation.getAllocationId(), attach, key)
                                            .thenReturn(new SyndicatePaymentReceipt(caseId, facilityId, allocation.getAllocationId(),
                                                    confirmed, allocation.getPaymentAmount(), allocation.getCurrency()));
                                })
                                .onErrorResume(error -> payments.cancel(paymentId).then(Mono.error(error))))
                        .onErrorResume(error -> facilities.deleteAllocation(caseId, facilityId, allocation.getAllocationId(), key)
                                .then(Mono.error(error))));
    }

    public Mono<Void> compensate(SyndicatePaymentReceipt receipt) {
        if (receipt == null) {
            return Mono.empty();
        }
        return payments.cancel(receipt.paymentId())
                .then(facilities.deleteAllocation(receipt.caseId(), receipt.facilityId(), receipt.allocationId(),
                        UUID.randomUUID().toString()));
    }

    private BorrowerFacilityView borrowerView(SyndicatedFacilityDTO facility) {
        String status = facility.getStatus() == null ? null : facility.getStatus().getValue();
        return new BorrowerFacilityView(facility.getFacilityId(), facility.getLoanServicingCaseId(),
                facility.getCurrency(), facility.getCommitmentAmount(), status);
    }
}
