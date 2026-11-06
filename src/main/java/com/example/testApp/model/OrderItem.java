package com.example.testApp.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Objects;

public class OrderItem {

    @NotBlank(message = "SKU must not be blank")
    private String sku;

    @NotNull(message = "Unit price must not be null")
    @DecimalMin(value = "0.00", message = "Unit price must not be negative")
    private BigDecimal unitPrice;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    public OrderItem() {
    }

    public OrderItem(String sku, BigDecimal unitPrice, int quantity) {
        this.sku = sku;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        return quantity == orderItem.quantity &&
                Objects.equals(sku, orderItem.sku) &&
                Objects.equals(unitPrice, orderItem.unitPrice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku, unitPrice, quantity);
    }
}
