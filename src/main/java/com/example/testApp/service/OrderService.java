package com.example.testApp.service;

import com.example.testApp.dto.OrderResponse;
import com.example.testApp.exception.TooManyItemsException;
import com.example.testApp.model.CustomerTier;
import com.example.testApp.model.Order;
import com.example.testApp.model.OrderItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;

@Service
public class OrderService {

    private static final int MAX_DISTINCT_SKUS = 50;
    private static final String VALID_PROMO_CODE = "SAVE20";
    private static final BigDecimal PROMO_DISCOUNT = new BigDecimal("20.00");
    private static final BigDecimal PROMO_MIN_THRESHOLD = new BigDecimal("100.00");

    public BigDecimal calculateTotal(Order order) {
        return calculateOrderResponse(order).getTotal();
    }

    public OrderResponse calculateOrderResponse(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order must not be null");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order items must not be empty");
        }

        // AC-R6: reject orders with more than 50 distinct SKUs
        Set<String> distinctSkus = new HashSet<>();
        for (OrderItem item : order.getItems()) {
            if (item == null) {
                throw new IllegalArgumentException("Order item must not be null");
            }
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Unit price must not be negative");
            }
            if (item.getQuantity() < 1) {
                throw new IllegalArgumentException("Quantity must be at least 1");
            }
            if (item.getSku() != null) {
                distinctSkus.add(item.getSku());
            }
        }

        if (distinctSkus.size() > MAX_DISTINCT_SKUS) {
            throw new TooManyItemsException(
                    "Order contains " + distinctSkus.size() + " distinct items, which exceeds the maximum of " + MAX_DISTINCT_SKUS);
        }

        // Compute subtotal
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            BigDecimal itemTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(itemTotal);
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

        // AC-R1 / AC-R2: apply tier discount
        BigDecimal tierDiscountRate = getTierDiscountRate(order.getCustomerTier());
        BigDecimal discountAmount = subtotal.multiply(tierDiscountRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal afterTierDiscount = subtotal.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);

        // AC-R3 / AC-R4: apply promo code
        BigDecimal promoAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        String promoCode = order.getPromoCode();
        if (promoCode != null && !promoCode.isBlank() && VALID_PROMO_CODE.equals(promoCode.trim())) {
            if (afterTierDiscount.compareTo(PROMO_MIN_THRESHOLD) >= 0) {
                promoAmount = PROMO_DISCOUNT.setScale(2, RoundingMode.HALF_UP);
            }
        }

        // AC-R3: final total cannot go below 0.00
        BigDecimal total = subtotal.subtract(discountAmount).subtract(promoAmount).setScale(2, RoundingMode.HALF_UP);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return new OrderResponse(subtotal, discountAmount, promoAmount, total);
    }

    private BigDecimal getTierDiscountRate(CustomerTier tier) {
        if (tier == null) {
            return BigDecimal.ZERO;
        }
        switch (tier) {
            case GOLD:
                return new BigDecimal("0.10");
            case SILVER:
                return new BigDecimal("0.05");
            case STANDARD:
            default:
                return BigDecimal.ZERO;
        }
    }
}
