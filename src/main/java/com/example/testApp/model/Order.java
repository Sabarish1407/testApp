package com.example.testApp.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

import java.util.List;

public class Order {

    private String id;

    @NotBlank(message = "Customer ID must not be blank")
    private String customerId;

    @NotEmpty(message = "Order items must not be empty")
    private List<@Valid OrderItem> items;

    @NotNull(message = "Customer tier must not be null")
    private CustomerTier customerTier;

    private String promoCode;

    private boolean promoApplied;

    public Order() {
    }

    public Order(String id, String customerId, List<OrderItem> items, CustomerTier customerTier) {
        this.id = id;
        this.customerId = customerId;
        this.items = items;
        this.customerTier = customerTier;
    }

    public Order(String id, String customerId, List<OrderItem> items, CustomerTier customerTier, String promoCode) {
        this.id = id;
        this.customerId = customerId;
        this.items = items;
        this.customerTier = customerTier;
        this.promoCode = promoCode;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public CustomerTier getCustomerTier() {
        return customerTier;
    }

    public void setCustomerTier(CustomerTier customerTier) {
        this.customerTier = customerTier;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }

    public boolean isPromoApplied() {
        return promoApplied;
    }

    public void setPromoApplied(boolean promoApplied) {
        this.promoApplied = promoApplied;
    }
}
