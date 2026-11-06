package com.example.testApp.service;

import com.example.testApp.dto.OrderResponse;
import com.example.testApp.model.CustomerTier;
import com.example.testApp.model.Order;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DiscountService {

    private static final BigDecimal PROMO_MINIMUM_THRESHOLD = new BigDecimal("100.00");

    private final PromoCodeValidator promoCodeValidator;

    public DiscountService(PromoCodeValidator promoCodeValidator) {
        this.promoCodeValidator = promoCodeValidator;
    }

    public OrderResponse applyDiscounts(Order order, BigDecimal subtotal) {
        CustomerTier tier = order.getCustomerTier() != null ? order.getCustomerTier() : CustomerTier.STANDARD;

        BigDecimal discountRate = switch (tier) {
            case GOLD -> new BigDecimal("0.10");
            case SILVER -> new BigDecimal("0.50");
            case STANDARD -> BigDecimal.ZERO;
        };

        BigDecimal discountAmount = subtotal.multiply(discountRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountedTotal = subtotal.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);

        BigDecimal promoAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        String promoCode = order.getPromoCode();

        if (promoCode != null && promoCodeValidator.isValid(promoCode) && !order.isPromoApplied()) {
            if (discountedTotal.compareTo(PROMO_MINIMUM_THRESHOLD) > 0) {
                BigDecimal discount = promoCodeValidator.getPromoDiscount(promoCode);
                if (discountedTotal.compareTo(discount) < 0) {
                    promoAmount = discountedTotal;
                } else {
                    promoAmount = discount.setScale(2, RoundingMode.HALF_UP);
                }
                order.setPromoApplied(true);
            }
        }

        BigDecimal finalTotal = discountedTotal.subtract(promoAmount).setScale(2, RoundingMode.HALF_UP);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return new OrderResponse(
                subtotal.setScale(2, RoundingMode.HALF_UP),
                discountAmount,
                promoAmount,
                finalTotal
        );
    }
}
