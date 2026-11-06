package com.example.testApp.service;

import com.example.testApp.model.Order;
import com.example.testApp.model.OrderItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class OrderService {

    public BigDecimal calculateTotal(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order must not be null");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order items must not be empty");
        }

        BigDecimal total = BigDecimal.ZERO;
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
            BigDecimal itemTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(itemTotal);
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
