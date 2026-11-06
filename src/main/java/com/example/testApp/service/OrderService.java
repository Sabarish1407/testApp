package com.example.testApp.service;

import com.example.testApp.dto.OrderResponse;
import com.example.testApp.exception.UnprocessableEntityException;
import com.example.testApp.model.Order;
import com.example.testApp.model.OrderItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

@Service
public class OrderService {

    private final DiscountService discountService;

    public OrderService() {
        this.discountService = new DiscountService(new PromoCodeValidator());
    }

    public OrderService(DiscountService discountService) {
        this.discountService = discountService;
    }

    public OrderResponse calculatePricing(Order order) {
        validateOrder(order);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            BigDecimal itemTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(itemTotal);
        }

        return discountService.applyDiscounts(order, subtotal);
    }

    public BigDecimal calculateTotal(Order order) {
        return calculatePricing(order).getTotal();
    }

    private void validateOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order must not be null");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order items must not be empty");
        }

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
        }

        long distinctItemsCount = order.getItems().stream()
                .map(OrderItem::getSku)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        if (distinctItemsCount > 50) {
            throw new UnprocessableEntityException("Order cannot contain more than 50 distinct items");
        }
    }
}
