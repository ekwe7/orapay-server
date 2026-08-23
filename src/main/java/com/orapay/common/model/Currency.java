package com.orapay.common.model;

public enum Currency {
    USD(2),
    EUR(2),
    GBP(2),
    NGN(2);

    private final int defaultFractionDigits;

    Currency(int defaultFractionDigits) {
        this.defaultFractionDigits = defaultFractionDigits;
    }

    public int getDefaultFractionDigits() {
        return defaultFractionDigits;
    }
}
