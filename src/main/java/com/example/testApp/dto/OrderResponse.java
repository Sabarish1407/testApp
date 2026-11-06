package com.example.testApp.dto;

import java.math.BigDecimal;

public class OrderResponse {

    private BigDecimal total;

    public OrderResponse() {
    }

    public OrderResponse(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}
