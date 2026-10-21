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

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Product pricing configuration already stored for the demo lending products:
 * PRICING / interest_rate_brackets and LIMITS / amount_term.
 */
public final class ProductInterestCatalog {

    public static final UUID PERSONAL_LOAN = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    public static final UUID LEASING = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private static final MathContext MATH = MathContext.DECIMAL64;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWELVE = new BigDecimal("12");

    private static final List<Product> PRODUCTS = List.of(
            new Product(PERSONAL_LOAN, "PERSONAL_LOAN", "EUR",
                    new BigDecimal("1000"), new BigDecimal("60000"), 12, 96,
                    List.of(new Bracket(new BigDecimal("1000"), new BigDecimal("60000"), new BigDecimal("7.99")))),
            new Product(LEASING, "LEASING", "EUR",
                    new BigDecimal("5000"), new BigDecimal("500000"), 12, 84,
                    List.of(
                            new Bracket(new BigDecimal("5000"), new BigDecimal("25000"), new BigDecimal("6.90")),
                            new Bracket(new BigDecimal("25001"), new BigDecimal("100000"), new BigDecimal("5.90")),
                            new Bracket(new BigDecimal("100001"), new BigDecimal("250000"), new BigDecimal("5.50")),
                            new Bracket(new BigDecimal("250001"), new BigDecimal("500000"), new BigDecimal("5.20")))));

    private ProductInterestCatalog() {
    }

    public static LoanPrice price(BigDecimal principal, int term, UUID productId) {
        if (principal == null || principal.signum() <= 0) {
            throw new IllegalArgumentException("Principal amount is required");
        }
        if (term < 1) {
            throw new IllegalArgumentException("Loan term is required");
        }
        if (productId == null) {
            throw new IllegalArgumentException("Product id is required");
        }
        Product product = PRODUCTS.stream()
                .filter(row -> row.id.equals(productId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Product pricing configuration was not found"));
        if (principal.compareTo(product.minAmount) < 0 || principal.compareTo(product.maxAmount) > 0) {
            throw new IllegalArgumentException("Principal is outside the product amount limits");
        }
        if (term < product.minTerm || term > product.maxTerm) {
            throw new IllegalArgumentException("Term is outside the product term limits");
        }
        Bracket bracket = product.brackets.stream()
                .filter(row -> row.minAmount != null && principal.compareTo(row.minAmount) >= 0)
                .max(Comparator.comparing(row -> row.minAmount))
                .orElseThrow(() -> new IllegalArgumentException("Principal is below every interest-rate bracket"));
        BigDecimal monthlyRate = bracket.tin.divide(HUNDRED, MATH).divide(TWELVE, MATH);
        BigDecimal installment = monthlyPayment(principal, monthlyRate, term).setScale(2, RoundingMode.HALF_EVEN);
        return new LoanPrice(bracket.tin, installment, product.currency);
    }

    private static BigDecimal monthlyPayment(BigDecimal principal, BigDecimal monthlyRate, int term) {
        if (monthlyRate.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(term), MATH);
        }
        BigDecimal growth = BigDecimal.ONE.add(monthlyRate, MATH).pow(term, MATH);
        BigDecimal numerator = monthlyRate.multiply(growth, MATH);
        BigDecimal denominator = growth.subtract(BigDecimal.ONE, MATH);
        return principal.multiply(numerator, MATH).divide(denominator, MATH);
    }

    private record Product(
            UUID id,
            String type,
            String currency,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            int minTerm,
            int maxTerm,
            List<Bracket> brackets) {
    }

    private record Bracket(BigDecimal minAmount, BigDecimal maxAmount, BigDecimal tin) {
    }
}
