package com.orapay.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Embeddable
@Getter
@EqualsAndHashCode
@ToString
public class MonetaryAmount implements Serializable, Comparable<MonetaryAmount> {

    @Column(name = "amount", nullable = false)
    private final long amount; // Stored in minor units (e.g. cents)

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    private final Currency currency;

    // Default constructor for JPA
    protected MonetaryAmount() {
        this.amount = 0L;
        this.currency = Currency.USD;
    }

    public MonetaryAmount(long amount, Currency currency) {
        this.amount = amount;
        this.currency = Objects.requireNonNull(currency, "Currency must not be null");
    }

    public static MonetaryAmount of(long minorUnits, Currency currency) {
        return new MonetaryAmount(minorUnits, currency);
    }

    public static MonetaryAmount zero(Currency currency) {
        return new MonetaryAmount(0L, currency);
    }

    public static MonetaryAmount fromMajorUnits(BigDecimal majorUnits, Currency currency) {
        Objects.requireNonNull(majorUnits, "Major units amount must not be null");
        Objects.requireNonNull(currency, "Currency must not be null");
        long minorUnits = majorUnits.setScale(currency.getDefaultFractionDigits(), RoundingMode.UNNECESSARY)
                .movePointRight(currency.getDefaultFractionDigits())
                .longValueExact();
        return new MonetaryAmount(minorUnits, currency);
    }

    public MonetaryAmount add(MonetaryAmount other) {
        checkSameCurrency(other);
        return new MonetaryAmount(Math.addExact(this.amount, other.amount), this.currency);
    }

    public MonetaryAmount subtract(MonetaryAmount other) {
        checkSameCurrency(other);
        return new MonetaryAmount(Math.subtractExact(this.amount, other.amount), this.currency);
    }

    public MonetaryAmount multiply(long multiplier) {
        return new MonetaryAmount(Math.multiplyExact(this.amount, multiplier), this.currency);
    }

    public boolean isPositive() {
        return this.amount > 0;
    }

    public boolean isNegative() {
        return this.amount < 0;
    }

    public boolean isZero() {
        return this.amount == 0;
    }

    public boolean isGreaterThan(MonetaryAmount other) {
        checkSameCurrency(other);
        return this.amount > other.amount;
    }

    public boolean isLessThan(MonetaryAmount other) {
        checkSameCurrency(other);
        return this.amount < other.amount;
    }

    public BigDecimal toMajorUnits() {
        return BigDecimal.valueOf(this.amount)
                .movePointLeft(this.currency.getDefaultFractionDigits());
    }

    private void checkSameCurrency(MonetaryAmount other) {
        Objects.requireNonNull(other, "MonetaryAmount to compare/operate must not be null");
        if (this.currency != other.currency) {
            throw new IllegalArgumentException(
                    String.format("Currency mismatch: %s vs %s", this.currency, other.currency)
            );
        }
    }

    @Override
    public int compareTo(MonetaryAmount other) {
        checkSameCurrency(other);
        return Long.compare(this.amount, other.amount);
    }
}
