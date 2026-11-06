package com.example.testApp.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PromoCodeValidator {

    private static final String SAVE20 = "SAVE20";
    private static final BigDecimal SAVE20_DISCOUNT = new BigDecimal("20.00");

    public boolean isValid(String promoCode) {
        return promoCode != null && SAVE20.equalsIgnoreCase(promoCode.trim());
    }

    public BigDecimal getPromoDiscount(String promoCode) {
        if (isValid(promoCode)) {
            return SAVE20_DISCOUNT;
        }
        return BigDecimal.ZERO;
    }
}
