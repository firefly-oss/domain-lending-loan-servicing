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

import io.fireflysoftware.core.accounting.invoker.ApiClient;
import io.fireflysoftware.core.accounting.sdk.api.GlAccountsApi;
import io.fireflysoftware.core.accounting.sdk.api.JournalBatchesApi;
import io.fireflysoftware.core.accounting.sdk.api.JournalEntriesApi;
import io.fireflysoftware.core.accounting.sdk.api.JournalLinesApi;
import io.fireflysoftware.core.accounting.sdk.model.JournalBatchDTO;
import io.fireflysoftware.core.accounting.sdk.model.JournalEntryDTO;
import io.fireflysoftware.core.accounting.sdk.model.JournalLineDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Live general-ledger adapter. Posts interest through core-common-accounting-gl-sdk.
 * It does not call the accounting domain.
 */
@Component
@ConditionalOnProperty(name = "firefly.lending.accounting.adapter", havingValue = "core")
public class AccountingDomainLedgerAdapter implements GeneralLedgerPort {

    private static final UUID DEFAULT_RECEIVABLE = UUID.fromString("00000000-0000-4000-8000-000000000011");
    private static final UUID DEFAULT_INCOME = UUID.fromString("00000000-0000-4000-8000-000000000012");

    private final GlAccountsApi accounts;
    private final JournalBatchesApi batches;
    private final JournalEntriesApi entries;
    private final JournalLinesApi lines;
    private final UUID receivableAccountId;
    private final UUID incomeAccountId;

    @Autowired
    public AccountingDomainLedgerAdapter(
            @Value("${firefly.lending.accounting.gl-base-url:http://localhost:8080}") String baseUrl,
            @Value("${firefly.lending.accounting.interest-receivable-account:00000000-0000-4000-8000-000000000011}") UUID receivableAccountId,
            @Value("${firefly.lending.accounting.interest-income-account:00000000-0000-4000-8000-000000000012}") UUID incomeAccountId) {
        ApiClient client = new ApiClient();
        client.setBasePath(strip(baseUrl));
        this.accounts = new GlAccountsApi(client);
        this.batches = new JournalBatchesApi(client);
        this.entries = new JournalEntriesApi(client);
        this.lines = new JournalLinesApi(client);
        this.receivableAccountId = receivableAccountId;
        this.incomeAccountId = incomeAccountId;
    }

    @Override
    public Mono<InterestPosting> postInterest(LocalDate businessDate, BigDecimal amount, String currency) {
        if (businessDate == null || amount == null || amount.signum() <= 0 || currency == null || !currency.matches("[A-Z]{3}")) {
            return Mono.error(new IllegalArgumentException("Business date, a positive amount, and a 3-letter currency are required"));
        }
        String reference = "LOAN-INTEREST-" + businessDate;
        BigDecimal posted = amount.setScale(2, RoundingMode.HALF_UP);
        return find(businessDate, reference, posted, currency)
                .switchIfEmpty(Mono.defer(() -> create(businessDate, posted, currency, reference)));
    }

    @Override
    public Mono<InterestPosting> findInterest(LocalDate businessDate) {
        if (businessDate == null) {
            return Mono.empty();
        }
        return find(businessDate, "LOAN-INTEREST-" + businessDate, null, null);
    }

    private Mono<InterestPosting> find(LocalDate businessDate, String reference, BigDecimal amount, String currency) {
        return batches.search(0, 100, null, null, null, null, reference)
                .flatMap(page -> {
                    if (page == null || page.getContent() == null) {
                        return Mono.empty();
                    }
                    return Flux.fromIterable(page.getContent())
                            .filter(batch -> reference.equals(batch.getReference()) || businessDate.equals(batch.getBatchDate()))
                            .next()
                            .map(batch -> new InterestPosting(
                                    batch.getId(),
                                    batch.getBatchDate() == null ? businessDate : batch.getBatchDate(),
                                    amount,
                                    currency));
                });
    }

    private Mono<InterestPosting> create(LocalDate businessDate, BigDecimal amount, String currency, String reference) {
        JournalBatchDTO batch = new JournalBatchDTO()
                .reference(reference)
                .status(JournalBatchDTO.StatusEnum.POSTED)
                .batchDate(businessDate)
                .remarks("Loan interest accrual");
        return accounts.getById3(receivableAccountId, reference)
                .then(accounts.getById3(incomeAccountId, reference))
                .then(batches.create(batch, reference))
                .flatMap(created -> {
                    JournalEntryDTO entry = new JournalEntryDTO()
                            .transactionId(reference)
                            .sourceModule("LENDING")
                            .totalAmount(amount)
                            .description("Loan interest accrued")
                            .entryDate(businessDate.atStartOfDay())
                            .posted(true);
                    return entries.create1(created.getId(), entry, reference)
                            .flatMap(saved -> lines.create2(created.getId(), saved.getId(),
                                            line(receivableAccountId, amount, currency, JournalLineDTO.DrCrEnum.DR, "Interest receivable"),
                                            reference)
                                    .then(lines.create2(created.getId(), saved.getId(),
                                            line(incomeAccountId, amount, currency, JournalLineDTO.DrCrEnum.CR, "Interest income"),
                                            reference)))
                            .thenReturn(new InterestPosting(created.getId(), businessDate, amount, currency));
                });
    }

    private static JournalLineDTO line(
            UUID accountId, BigDecimal amount, String currency, JournalLineDTO.DrCrEnum side, String memo) {
        return new JournalLineDTO()
                .accountId(accountId)
                .amount(amount)
                .drCr(side)
                .fxCurrency(currency)
                .memo(memo);
    }

    private static String strip(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:8080";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
