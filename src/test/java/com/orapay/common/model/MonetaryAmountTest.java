package com.orapay.common.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MonetaryAmountTest {

    @Test
    @DisplayName("Should create MonetaryAmount from minor units")
    void shouldCreateFromMinorUnits() {
        MonetaryAmount amount = MonetaryAmount.of(1050L, Currency.USD);
        assertEquals(1050L, amount.getAmount());
        assertEquals(Currency.USD, amount.getCurrency());
        assertEquals(new BigDecimal("10.50"), amount.toMajorUnits());
    }

    @Test
    @DisplayName("Should create MonetaryAmount from major units BigDecimal")
    void shouldCreateFromMajorUnits() {
        MonetaryAmount amount = MonetaryAmount.fromMajorUnits(new BigDecimal("25.00"), Currency.EUR);
        assertEquals(2500L, amount.getAmount());
        assertEquals(Currency.EUR, amount.getCurrency());
    }

    @Test
    @DisplayName("Should perform addition and subtraction correctly")
    void shouldPerformArithmetic() {
        MonetaryAmount a1 = MonetaryAmount.of(5000L, Currency.USD);
        MonetaryAmount a2 = MonetaryAmount.of(2000L, Currency.USD);

        MonetaryAmount sum = a1.add(a2);
        assertEquals(7000L, sum.getAmount());

        MonetaryAmount diff = a1.subtract(a2);
        assertEquals(3000L, diff.getAmount());

        MonetaryAmount product = a2.multiply(3);
        assertEquals(6000L, product.getAmount());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException on currency mismatch")
    void shouldThrowOnCurrencyMismatch() {
        MonetaryAmount usd = MonetaryAmount.of(1000L, Currency.USD);
        MonetaryAmount eur = MonetaryAmount.of(1000L, Currency.EUR);

        assertThrows(IllegalArgumentException.class, () -> usd.add(eur));
        assertThrows(IllegalArgumentException.class, () -> usd.subtract(eur));
        assertThrows(IllegalArgumentException.class, () -> usd.isGreaterThan(eur));
    }

    @Test
    @DisplayName("Should correctly evaluate relational predicates")
    void shouldEvaluatePredicates() {
        MonetaryAmount pos = MonetaryAmount.of(100L, Currency.NGN);
        MonetaryAmount zero = MonetaryAmount.zero(Currency.NGN);
        MonetaryAmount neg = MonetaryAmount.of(-50L, Currency.NGN);

        assertTrue(pos.isPositive());
        assertTrue(zero.isZero());
        assertTrue(neg.isNegative());

        assertTrue(pos.isGreaterThan(zero));
        assertTrue(neg.isLessThan(zero));
    }
}
