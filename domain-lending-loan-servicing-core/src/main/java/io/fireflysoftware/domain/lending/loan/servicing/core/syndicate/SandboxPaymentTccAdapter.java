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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deterministic try, confirm, and cancel. No network.
 */
@Component
@ConditionalOnProperty(name = "firefly.lending.payments.adapter", havingValue = "sandbox", matchIfMissing = true)
public class SandboxPaymentTccAdapter implements PaymentTccPort {

    private final ConcurrentHashMap<UUID, String> pending = new ConcurrentHashMap<>();

    @Override
    public Mono<UUID> tryPayment(BigDecimal amount, String currency, String idempotencyKey) {
        if (amount == null || amount.signum() <= 0 || currency == null || currency.length() != 3) {
            return Mono.error(new IllegalArgumentException("Payment amount and currency are required"));
        }
        String key = (idempotencyKey == null ? "" : idempotencyKey) + ":" + amount.toPlainString() + ":" + currency;
        UUID paymentId = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
        pending.put(paymentId, currency);
        return Mono.just(paymentId);
    }

    @Override
    public Mono<UUID> confirm(UUID paymentId) {
        if (paymentId == null || pending.remove(paymentId) == null) {
            return Mono.error(new IllegalStateException("Payment was not tried"));
        }
        return Mono.just(paymentId);
    }

    @Override
    public Mono<Void> cancel(UUID paymentId) {
        if (paymentId != null) {
            pending.remove(paymentId);
        }
        return Mono.empty();
    }
}
