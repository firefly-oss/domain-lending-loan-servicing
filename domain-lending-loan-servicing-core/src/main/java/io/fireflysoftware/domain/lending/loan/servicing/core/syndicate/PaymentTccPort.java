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

import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Try, confirm, and cancel for a repayment. The default adapter is the sandbox.
 * It does not post the ledger and it does not open a socket.
 * An installer replaces it with the payments domain SDK when that SDK is published.
 */
public interface PaymentTccPort {

    Mono<UUID> tryPayment(BigDecimal amount, String currency, String idempotencyKey);

    Mono<UUID> confirm(UUID paymentId);

    Mono<Void> cancel(UUID paymentId);
}
