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


package io.fireflysoftware.domain.lending.loan.servicing.web.controller;

import io.fireflysoftware.domain.lending.loan.servicing.core.syndicate.AllocateSyndicateRepaymentCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.syndicate.AllocateSyndicateRepaymentSaga;
import io.fireflysoftware.domain.lending.loan.servicing.core.syndicate.BorrowerFacilityView;
import io.fireflysoftware.domain.lending.loan.servicing.core.syndicate.SyndicatePaymentReceipt;
import io.fireflysoftware.domain.lending.loan.servicing.core.syndicate.SyndicateRepaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Borrower read of a syndicated facility, and the repayment that allocates it.
 * Participant administration stays on the servicing core. The finance terminal calls
 * {@code SyndicatedFacilityApi} on core-lending-loan-servicing.
 */
@RestController
@RequestMapping("/api/v1/loans/{loanId}/syndicated-facility")
@Tag(name = "SyndicatedFacility", description = "Borrower view of a syndicated facility and its repayment")
public class SyndicatedFacilityController {

    private final SyndicateRepaymentService repayments;
    private final AllocateSyndicateRepaymentSaga saga;

    public SyndicatedFacilityController(SyndicateRepaymentService repayments, AllocateSyndicateRepaymentSaga saga) {
        this.repayments = repayments;
        this.saga = saga;
    }

    @GetMapping
    @Operation(operationId = "viewSyndicatedFacility", summary = "Borrower view of the syndicated facility")
    public Mono<ResponseEntity<BorrowerFacilityView>> view(@PathVariable UUID loanId) {
        return repayments.viewForBorrower(loanId).map(ResponseEntity::ok);
    }

    @PostMapping("/repayments")
    @Operation(operationId = "repaySyndicatedFacility", summary = "Allocate a repayment and submit it through payments")
    public Mono<ResponseEntity<SyndicatePaymentReceipt>> repay(@PathVariable UUID loanId,
                                                               @RequestBody AllocateSyndicateRepaymentCommand command) {
        AllocateSyndicateRepaymentCommand scoped = new AllocateSyndicateRepaymentCommand(loanId, command.facilityId(),
                command.repaymentId(), command.amount(), command.currency(), command.idempotencyKey());
        return saga.allocateAndPay(scoped).map(ResponseEntity::ok);
    }
}
