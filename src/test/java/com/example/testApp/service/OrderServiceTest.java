package com.example.testApp.service;

import com.example.testApp.model.CustomerTier;
import com.example.testApp.model.Order;
import com.example.testApp.model.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderServiceTest {

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService();
    }

    @Test
    @DisplayName("calculateTotal should return correct total for a single valid item")
    void calculateTotal_withSingleItem_returnsCorrectTotal() {
        OrderItem item = new OrderItem("SKU-1", new BigDecimal("19.99"), 2);
        Order order = new Order("ord-1", "cust-1", List.of(item), CustomerTier.STANDARD);

        BigDecimal total = orderService.calculateTotal(order);

        assertThat(total).isEqualByComparingTo("39.98");
    }

    @Test
    @DisplayName("calculateTotal should return sum of unit price times quantity across multiple items")
    void calculateTotal_withMultipleItems_returnsCorrectSum() {
        OrderItem item1 = new OrderItem("SKU-1", new BigDecimal("10.50"), 2);
        OrderItem item2 = new OrderItem("SKU-2", new BigDecimal("5.25"), 4);
        Order order = new Order("ord-2", "cust-2", List.of(item1, item2), CustomerTier.STANDARD);

        BigDecimal total = orderService.calculateTotal(order);

        assertThat(total).isEqualByComparingTo("42.00");
    }

    @Test
    @DisplayName("calculateTotal should throw IllegalArgumentException when order is null")
    void calculateTotal_withNullOrder_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> orderService.calculateTotal(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Order must not be null");
    }

    @Test
    @DisplayName("calculateTotal should throw IllegalArgumentException when items list is null")
    void calculateTotal_withNullItemsList_throwsIllegalArgumentException() {
        Order order = new Order("ord-3", "cust-3", null, CustomerTier.STANDARD);

        assertThatThrownBy(() -> orderService.calculateTotal(order))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Order items must not be empty");
    }

    @Test
    @DisplayName("calculateTotal should throw IllegalArgumentException when items list is empty")
    void calculateTotal_withEmptyItemsList_throwsIllegalArgumentException() {
        Order order = new Order("ord-4", "cust-4", List.of(), CustomerTier.STANDARD);

        assertThatThrownBy(() -> orderService.calculateTotal(order))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Order items must not be empty");
    }

    @Test
    @DisplayName("calculateTotal should throw IllegalArgumentException when an order item is null")
    void calculateTotal_withNullOrderItem_throwsIllegalArgumentException() {
        List<OrderItem> items = new ArrayList<>();
        items.add(null);
        Order order = new Order("ord-5", "cust-5", items, CustomerTier.STANDARD);

        assertThatThrownBy(() -> orderService.calculateTotal(order))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Order item must not be null");
    }

    @Test
    @DisplayName("calculateTotal should throw IllegalArgumentException when item unit price is negative")
    void calculateTotal_withNegativeUnitPrice_throwsIllegalArgumentException() {
        OrderItem item = new OrderItem("SKU-NEG", new BigDecimal("-5.00"), 1);
        Order order = new Order("ord-6", "cust-6", List.of(item), CustomerTier.SILVER);

        assertThatThrownBy(() -> orderService.calculateTotal(order))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unit price must not be negative");
    }

    @Test
    @DisplayName("calculateTotal should throw IllegalArgumentException when item quantity is zero")
    void calculateTotal_withZeroQuantity_throwsIllegalArgumentException() {
        OrderItem item = new OrderItem("SKU-ZERO", new BigDecimal("10.00"), 0);
        Order order = new Order("ord-7", "cust-7", List.of(item), CustomerTier.STANDARD);

        assertThatThrownBy(() -> orderService.calculateTotal(order))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Quantity must be at least 1");
    }

    @Test
    @DisplayName("calculateTotal should throw IllegalArgumentException when item quantity is negative")
    void calculateTotal_withNegativeQuantity_throwsIllegalArgumentException() {
        OrderItem item = new OrderItem("SKU-SUB", new BigDecimal("10.00"), -3);
        Order order = new Order("ord-8", "cust-8", List.of(item), CustomerTier.STANDARD);

        assertThatThrownBy(() -> orderService.calculateTotal(order))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Quantity must be at least 1");
    }
}
