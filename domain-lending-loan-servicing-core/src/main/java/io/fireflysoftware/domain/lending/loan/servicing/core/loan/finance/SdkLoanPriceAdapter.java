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

import io.fireflysoftware.domain.core.pricing.engine.sdk.api.PricingEngineProductsApi;
import io.fireflysoftware.domain.core.pricing.engine.sdk.api.PricingEngineSimulationApi;
import io.fireflysoftware.domain.core.pricing.engine.sdk.invoker.ApiClient;
import io.fireflysoftware.domain.core.pricing.engine.sdk.model.CalculateSimulationCommand;
import io.fireflysoftware.domain.core.pricing.engine.sdk.model.ProductPricingDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Live price adapter. Passes principal, term, and product id to the pricing engine SDK.
 */
@Component
@ConditionalOnProperty(name = "firefly.lending.price.adapter", havingValue = "engine")
public class SdkLoanPriceAdapter implements LoanPricePort {

    private final PricingEngineProductsApi products;
    private final PricingEngineSimulationApi simulations;

    @Autowired
    public SdkLoanPriceAdapter(@Value("${firefly.lending.price.engine-base-url:http://localhost:18080}") String baseUrl) {
        ApiClient client = new ApiClient();
        client.setBasePath(strip(baseUrl));
        this(new PricingEngineProductsApi(client), new PricingEngineSimulationApi(client));
    }

    SdkLoanPriceAdapter(PricingEngineProductsApi products, PricingEngineSimulationApi simulations) {
        this.products = products;
        this.simulations = simulations;
    }

    @Override
    public Mono<LoanPrice> price(BigDecimal principal, int term, UUID productId) {
        if (principal == null || principal.signum() <= 0 || term < 1 || productId == null) {
            return Mono.error(new IllegalArgumentException("Principal, term, and product id are required"));
        }
        return products.getEngineProductPricing(productId, null)
                .flatMap(pricing -> simulations.calculateSimulation(command(principal, term, productId, pricing), null))
                .map(result -> {
                    String currency = result.getCurrency();
                    if (currency == null || !currency.matches("[A-Z]{3}")) {
                        throw new IllegalArgumentException("Currency must be a 3-letter code");
                    }
                    if (result.getTin() == null || result.getMonthlyPayment() == null) {
                        throw new IllegalStateException("Pricing engine did not return a rate and installment");
                    }
                    return new LoanPrice(result.getTin(), result.getMonthlyPayment(), currency);
                });
    }

    private static CalculateSimulationCommand command(
            BigDecimal principal, int term, UUID productId, ProductPricingDTO pricing) {
        if (pricing.getProductType() == null || pricing.getProductType().isBlank()) {
            throw new IllegalStateException("Product pricing configuration has no product type");
        }
        CalculateSimulationCommand command = new CalculateSimulationCommand()
                .productId(productId)
                .productType(pricing.getProductType())
                .requestedAmount(principal)
                .term(term);
        if (pricing.getCurrency() != null) {
            command.currency(pricing.getCurrency());
        }
        return command;
    }

    private static String strip(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:18080";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
