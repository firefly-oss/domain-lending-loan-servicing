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

import io.fireflysoftware.core.banking.ledger.sdk.api.TransactionLegsApi;
import io.fireflysoftware.core.banking.ledger.sdk.api.TransactionsApi;
import io.fireflysoftware.core.banking.ledger.sdk.invoker.ApiClient;
import io.fireflysoftware.core.banking.ledger.sdk.model.TransactionDTO;
import io.fireflysoftware.core.banking.ledger.sdk.model.TransactionLegDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Live ledger adapter. Posts the cash movement through core-banking-ledger-sdk.
 */
@Component
@ConditionalOnProperty(name = "firefly.lending.ledger.adapter", havingValue = "core")
public class SdkLedgerAdapter implements LedgerPort {

    private final TransactionsApi transactions;
    private final TransactionLegsApi legs;

    @Autowired
    public SdkLedgerAdapter(@Value("${firefly.lending.ledger.core-base-url:http://localhost:8080}") String baseUrl) {
        ApiClient client = new ApiClient();
        client.setBasePath(strip(baseUrl));
        this(new TransactionsApi(client), new TransactionLegsApi(client));
    }

    SdkLedgerAdapter(TransactionsApi transactions, TransactionLegsApi legs) {
        this.transactions = transactions;
        this.legs = legs;
    }

    @Override
    public Mono<UUID> post(CashMovement movement) {
        if (movement == null || movement.amount() == null || movement.amount().signum() <= 0
                || movement.currency() == null || !movement.currency().matches("[A-Z]{3}")
                || movement.reference() == null || movement.debitAccountId() == null || movement.creditAccountId() == null) {
            return Mono.error(new IllegalArgumentException("A balanced ledger movement requires amount, currency, reference, and accounts"));
        }
        return transactions.findByExternalReference(movement.reference(), movement.reference())
                .filter(existing -> existing.getTransactionStatus() == TransactionDTO.TransactionStatusEnum.POSTED
                        && existing.getTransactionId() != null)
                .map(TransactionDTO::getTransactionId)
                .switchIfEmpty(Mono.defer(() -> create(movement)))
                .onErrorResume(SdkLedgerAdapter::missing, error -> create(movement));
    }

    @Override
    public Mono<Void> reverse(UUID postingId) {
        if (postingId == null) {
            return Mono.empty();
        }
        return transactions.updateTransactionStatus(postingId, "REVERSED", "loan-repayment-reverse", postingId.toString())
                .then()
                .onErrorResume(SdkLedgerAdapter::missing, error -> Mono.empty());
    }

    private Mono<UUID> create(CashMovement movement) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        TransactionDTO request = new TransactionDTO()
                .externalReference(movement.reference())
                .requestId(movement.reference())
                .transactionDate(now)
                .valueDate(now)
                .bookingDate(now)
                .transactionType(TransactionDTO.TransactionTypeEnum.TRANSFER)
                .transactionStatus(TransactionDTO.TransactionStatusEnum.POSTED)
                .totalAmount(movement.amount())
                .currency(movement.currency())
                .description("Loan repayment")
                .accountId(movement.debitAccountId());
        return transactions.createTransaction(request, movement.reference())
                .flatMap(created -> {
                    if (created.getTransactionId() == null) {
                        return Mono.error(new IllegalStateException("Ledger response did not include a transaction id"));
                    }
                    return legs.createTransactionLeg(created.getTransactionId(),
                                    leg(created.getTransactionId(), movement.debitAccountId(), "DEBIT", movement, now),
                                    movement.reference())
                            .then(legs.createTransactionLeg(created.getTransactionId(),
                                    leg(created.getTransactionId(), movement.creditAccountId(), "CREDIT", movement, now),
                                    movement.reference()))
                            .thenReturn(created.getTransactionId());
                });
    }

    private static TransactionLegDTO leg(
            UUID transactionId, UUID accountId, String side, CashMovement movement, LocalDateTime valueDate) {
        return new TransactionLegDTO()
                .transactionId(transactionId)
                .accountId(accountId)
                .legType(side)
                .amount(movement.amount())
                .currency(movement.currency())
                .valueDate(valueDate);
    }

    private static boolean missing(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof WebClientResponseException http
                    && http.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String strip(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:8080";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
