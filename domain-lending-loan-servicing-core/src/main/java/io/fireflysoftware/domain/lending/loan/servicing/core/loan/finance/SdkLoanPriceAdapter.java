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

import io.fireflysoftware.core.product.sdk.api.ProductApi;
import io.fireflysoftware.core.product.sdk.invoker.ApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Live price adapter. Confirms the product through core-common-product-mgmt-sdk,
 * then prices from the loan product brackets. It does not call a domain service.
 * core-common-pricing-mgmt stays empty and is not used.
 */
@Component
@ConditionalOnProperty(name = "firefly.lending.price.adapter", havingValue = "core")
public class SdkLoanPriceAdapter implements LoanPricePort {

    private final ProductApi products;

    @Autowired
    public SdkLoanPriceAdapter(@Value("${firefly.lending.price.product-base-url:http://localhost:8080}") String baseUrl) {
        ApiClient client = new ApiClient();
        client.setBasePath(strip(baseUrl));
        this(new ProductApi(client));
    }

    SdkLoanPriceAdapter(ProductApi products) {
        this.products = products;
    }

    @Override
    public Mono<LoanPrice> price(BigDecimal principal, int term, UUID productId) {
        if (principal == null || principal.signum() <= 0 || term < 1 || productId == null) {
            return Mono.error(new IllegalArgumentException("Principal, term, and product id are required"));
        }
        return products.getProductById(productId, null)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Product was not found")))
                .map(product -> ProductInterestCatalog.price(principal, term,
                        product.getProductId() == null ? productId : product.getProductId()));
    }

    private static String strip(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:8080";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
