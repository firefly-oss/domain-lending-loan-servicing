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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Live general-ledger adapter. Asks domain accounting to post the journal.
 * That domain calls core-common-accounting-gl-sdk.
 */
@Component
@ConditionalOnProperty(name = "firefly.lending.accounting.adapter", havingValue = "domain")
public class AccountingDomainLedgerAdapter implements GeneralLedgerPort {

    private final WebClient webClient;
    private final String basePath;

    @Autowired
    public AccountingDomainLedgerAdapter(
            @Value("${firefly.lending.accounting.domain-base-url:http://localhost:18122}") String baseUrl) {
        this(WebClient.builder().build(), strip(baseUrl));
    }

    AccountingDomainLedgerAdapter(WebClient webClient, String basePath) {
        this.webClient = webClient;
        this.basePath = basePath;
    }

    @Override
    public Mono<InterestPosting> postInterest(LocalDate businessDate, BigDecimal amount, String currency) {
        JournalBody body = new JournalBody(businessDate, amount, currency, "LOAN-INTEREST-" + businessDate);
        return webClient.post()
                .uri(basePath + "/api/v1/journals")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(InterestPosting.class);
    }

    @Override
    public Mono<InterestPosting> findInterest(LocalDate businessDate) {
        return webClient.get()
                .uri(basePath + "/api/v1/journals/{businessDate}", businessDate)
                .retrieve()
                .bodyToMono(InterestPosting.class);
    }

    private static String strip(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:18122";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private record JournalBody(LocalDate businessDate, BigDecimal amount, String currency, String reference) {
    }
}
