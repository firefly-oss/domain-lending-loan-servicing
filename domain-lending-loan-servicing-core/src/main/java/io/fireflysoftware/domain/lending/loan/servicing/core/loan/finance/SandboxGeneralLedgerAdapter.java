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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default general-ledger adapter. One journal per business date, and it does not open a socket.
 */
@Component
@ConditionalOnProperty(name = "firefly.lending.accounting.adapter", havingValue = "sandbox", matchIfMissing = true)
public class SandboxGeneralLedgerAdapter implements GeneralLedgerPort {

    private final ConcurrentHashMap<LocalDate, InterestPosting> journals = new ConcurrentHashMap<>();

    @Override
    public Mono<InterestPosting> postInterest(LocalDate businessDate, BigDecimal amount, String currency) {
        if (businessDate == null) {
            return Mono.error(new IllegalArgumentException("Business date is required"));
        }
        if (amount == null || amount.signum() <= 0) {
            return Mono.error(new IllegalArgumentException("Interest amount must be positive"));
        }
        if (currency == null || !currency.matches("[A-Z]{3}")) {
            return Mono.error(new IllegalArgumentException("Currency must be a 3-letter code"));
        }
        InterestPosting existing = journals.get(businessDate);
        if (existing != null) {
            return Mono.just(existing);
        }
        UUID id = UUID.nameUUIDFromBytes(("LOAN-INTEREST-" + businessDate + ":" + currency)
                .getBytes(StandardCharsets.UTF_8));
        InterestPosting created = new InterestPosting(id, businessDate, amount.setScale(2, java.math.RoundingMode.HALF_UP), currency);
        InterestPosting raced = journals.putIfAbsent(businessDate, created);
        return Mono.just(raced == null ? created : raced);
    }

    @Override
    public Mono<InterestPosting> findInterest(LocalDate businessDate) {
        InterestPosting journal = journals.get(businessDate);
        return journal == null ? Mono.empty() : Mono.just(journal);
    }
}
