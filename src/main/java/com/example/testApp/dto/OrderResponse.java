package com.example.testApp.dto;

import java.math.BigDecimal;

public class OrderResponse {

    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal promoAmount;
    private BigDecimal total;

    public OrderResponse() {
    }

    public OrderResponse(BigDecimal total) {
        this.total = total;
    }

    public OrderResponse(BigDecimal subtotal, BigDecimal discountAmount, BigDecimal promoAmount, BigDecimal total) {
        this.subtotal = subtotal;
        this.discountAmount = discountAmount;
        this.promoAmount = promoAmount;
        this.total = total;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getPromoAmount() {
        return promoAmount;
    }

    public void setPromoAmount(BigDecimal promoAmount) {
        this.promoAmount = promoAmount;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}
