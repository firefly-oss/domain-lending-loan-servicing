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

import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Records the participant allocation and the payment. Compensation cancels the payment
 * and deletes the allocation. The ledger is not posted here.
 */
@Saga(name = "AllocateSyndicateRepaymentSaga")
@Service
public class AllocateSyndicateRepaymentSaga {

    private final SyndicateRepaymentService repayments;

    public AllocateSyndicateRepaymentSaga(SyndicateRepaymentService repayments) {
        this.repayments = repayments;
    }

    @SagaStep(id = "allocateAndPay", compensate = "compensatePayment")
    @StepEvent(type = "syndicatedRepayment.allocated")
    public Mono<SyndicatePaymentReceipt> allocateAndPay(AllocateSyndicateRepaymentCommand command) {
        return repayments.allocateAndPay(command.caseId(), command.facilityId(), command.repaymentId(),
                command.amount(), command.currency(), command.idempotencyKey());
    }

    public Mono<Void> compensatePayment(SyndicatePaymentReceipt receipt) {
        return repayments.compensate(receipt);
    }
}
