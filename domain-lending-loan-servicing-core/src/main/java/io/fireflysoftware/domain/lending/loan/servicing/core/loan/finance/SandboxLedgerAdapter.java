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


package io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default ledger adapter. Keeps the posting in memory and does not open a socket.
 */
@Component
@ConditionalOnProperty(name = "firefly.lending.ledger.adapter", havingValue = "sandbox", matchIfMissing = true)
public class SandboxLedgerAdapter implements LedgerPort {

    private final ConcurrentHashMap<String, UUID> posted = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> references = new ConcurrentHashMap<>();

    @Override
    public Mono<UUID> post(CashMovement movement) {
        String error = invalid(movement);
        if (error != null) {
            return Mono.error(new IllegalArgumentException(error));
        }
        UUID postingId = posted.computeIfAbsent(movement.reference(), reference ->
                UUID.nameUUIDFromBytes((reference + ":" + movement.amount().toPlainString() + ":" + movement.currency())
                        .getBytes(StandardCharsets.UTF_8)));
        references.put(postingId, movement.reference());
        return Mono.just(postingId);
    }

    @Override
    public Mono<Void> reverse(UUID postingId) {
        if (postingId != null) {
            String reference = references.remove(postingId);
            if (reference != null) {
                posted.remove(reference);
            }
        }
        return Mono.empty();
    }

    private static String invalid(CashMovement movement) {
        if (movement == null || movement.amount() == null || movement.amount().signum() <= 0) {
            return "Ledger amount must be positive";
        }
        if (movement.currency() == null || !movement.currency().matches("[A-Z]{3}")) {
            return "Currency must be a 3-letter code";
        }
        if (movement.reference() == null || movement.reference().isBlank()) {
            return "Ledger reference is required";
        }
        if (movement.debitAccountId() == null || movement.creditAccountId() == null) {
            return "Ledger accounts are required";
        }
        if (movement.debitAccountId().equals(movement.creditAccountId())) {
            return "Ledger entry must use two accounts";
        }
        return null;
    }
}
