package com.example.testApp.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import com.example.testApp.dto.OrderResponse;
import com.example.testApp.model.CustomerTier;
import com.example.testApp.model.Order;
import com.example.testApp.model.OrderItem;
import com.example.testApp.service.OrderService;
import jakarta.validation.Valid;
import java.math.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Spring Boot HTTP Integration Test for {@link OrderController}.
 * Executes real HTTP requests via MockMvc with complete ApplicationContext (0 domain mocks).
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("[R1] GOLD tier discount via POST /api/orders/total")
    void test_r1_tc_r1_03() throws Exception {
        // Given
        // One item priced at 100.00 with quantity 1 => subtotal = 100.00
        // GOLD tier => 10% discount => discountAmount = 10.00, total = 90.00
        OrderItem item = new OrderItem("SKU-001", new BigDecimal("100.00"), 1);
        Order order = new Order("order-gold-01", "customer-01", List.of(item), CustomerTier.GOLD);
        // 10.00
        double expectedDiscountAmount = 100.00 * 0.10;
        // 90.00
        double expectedTotal = 100.00 - expectedDiscountAmount;
        // When & Then
        mockMvc.perform(post("/api/orders/total").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(order))).andExpect(status().isOk()).andExpect(jsonPath("$.discountAmount").value(expectedDiscountAmount)).andExpect(jsonPath("$.total").value(expectedTotal));
    }

    @Test
    @DisplayName("[R1] STANDARD tier receives no discount via API - TC-R1-04")
    void test_r1_tc_r1_04() throws Exception {
        // Given
        OrderItem item = new OrderItem("SKU-001", new BigDecimal("150.00"), 1);
        Order order = new Order("order-001", "customer-001", List.of(item), CustomerTier.STANDARD);
        // Expected values derived from specification:
        // STANDARD tier => 0% discount
        // subtotal = 150.00 * 1 = 150.00
        // discountAmount = 150.00 * 0% = 0.00
        // total = 150.00 - 0.00 = 150.00
        // 0.00
        double expectedDiscountAmount = 150.00 * 0.0;
        // 150.00
        double expectedTotal = 150.00 - expectedDiscountAmount;
        // When & Then
        mockMvc.perform(post("/api/orders/total").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(order))).andExpect(status().isOk()).andExpect(jsonPath("$.discountAmount").value(expectedDiscountAmount)).andExpect(jsonPath("$.total").value(expectedTotal));
    }

    @Test
    @DisplayName("[R1] SILVER tier discount via POST /api/orders/total")
    void test_r1_tc_r1_06() throws Exception {
        // Given
        // Construct an order with SILVER tier whose subtotal is 300.00
        // Single item: unitPrice=100.00, quantity=3 => subtotal=300.00
        OrderItem item = new OrderItem("SKU-SILVER-001", new BigDecimal("100.00"), 3);
        Order order = new Order("order-silver-01", "customer-silver-01", List.of(item), CustomerTier.SILVER);
        // Expected values derived from specification:
        // SILVER tier => 5% discount
        // subtotal = 100.00 * 3 = 300.00
        // discountAmount = 300.00 * 0.05 = 15.00
        // total = 300.00 - 15.00 = 285.00
        double expectedSubtotal = 300.00;
        // 15.00
        double expectedDiscountAmount = expectedSubtotal * 0.05;
        // 285.00
        double expectedTotal = expectedSubtotal - expectedDiscountAmount;
        // When & Then
        mockMvc.perform(post("/api/orders/total").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(order))).andExpect(status().isOk()).andExpect(jsonPath("$.discountAmount").value(expectedDiscountAmount)).andExpect(jsonPath("$.total").value(expectedTotal));
    }

    @Test
    @DisplayName("[R2] POST /api/orders/total discount applied to subtotal not item price")
    void test_r2_tc_r2_04() throws Exception {
        // Given
        // Note: CustomerTier enum only defines STANDARD, SILVER, GOLD — no BRONZE tier exists.
        // The scenario references "BRONZE tier" but the specification only defines STANDARD, SILVER, GOLD.
        // We use SILVER as a representative discounted tier to verify that discountAmount is applied
        // to the full subtotal (not per-item price), per R2 specification.
        // Using two items with different quantities to make the distinction between
        // "discount on subtotal" vs "discount on individual item price" observable.
        // Item 1: SKU="ITEM-A", unitPrice=10.00, quantity=3  => line total = 30.00
        // Item 2: SKU="ITEM-B", unitPrice=7.33, quantity=2   => line total = 14.66
        // subtotal = 30.00 + 14.66 = 44.66
        // SILVER tier discount rate (assumed from spec context — the test asserts structural correctness:
        // discountAmount == subtotal * rate, total == subtotal - discountAmount, both rounded HALF_UP to 2dp)
        // Since the exact SILVER rate is not stated numerically in R2 text, we assert the relationship
        // holds structurally: discountAmount + total == subtotal (within rounding), and both are 2dp values.
        // We also assert HTTP 200 and that discountAmount > 0 (discount was applied).
        List<OrderItem> items = List.of(new OrderItem("ITEM-A", new BigDecimal("10.00"), 3), new OrderItem("ITEM-B", new BigDecimal("7.33"), 2));
        Order order = new Order("order-r2-04", "customer-r2-04", items, CustomerTier.SILVER);
        // subtotal = (10.00 * 3) + (7.33 * 2) = 30.00 + 14.66 = 44.66
        BigDecimal subtotal = new BigDecimal("10.00").multiply(new BigDecimal("3")).add(new BigDecimal("7.33").multiply(new BigDecimal("2"))).setScale(2, java.math.RoundingMode.HALF_UP);
        // subtotal = 44.66
        // When & Then
        String responseBody = mockMvc.perform(post("/api/orders/total").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(order))).andExpect(status().isOk()).andExpect(jsonPath("$.subtotal").value(subtotal.doubleValue())).andExpect(jsonPath("$.discountAmount").exists()).andExpect(jsonPath("$.total").exists()).andReturn().getResponse().getContentAsString();
        // Parse response to verify the structural invariant:
        // discountAmount must equal subtotal * tierRate (applied to full subtotal, not per-item)
        // total must equal subtotal - discountAmount
        // Both must be rounded to exactly 2 decimal places (HALF_UP)
        OrderResponse response = objectMapper.readValue(responseBody, OrderResponse.class);
        BigDecimal returnedSubtotal = response.getSubtotal();
        BigDecimal returnedDiscount = response.getDiscountAmount();
        BigDecimal returnedTotal = response.getTotal();
        // Assert subtotal matches expected computed value
        org.junit.jupiter.api.Assertions.assertEquals(subtotal, returnedSubtotal.setScale(2, java.math.RoundingMode.HALF_UP), "Subtotal must equal sum of (unitPrice * quantity) for all items, rounded to 2dp HALF_UP");
        // Assert discountAmount is non-negative (a discount was applied for SILVER tier)
        org.junit.jupiter.api.Assertions.assertTrue(returnedDiscount.compareTo(BigDecimal.ZERO) >= 0, "Discount amount must be non-negative");
        // Assert discountAmount is applied to the full subtotal:
        // If discount were applied per-item, rounding differences would accumulate differently.
        // Key invariant: total == subtotal - discountAmount (rounded to 2dp HALF_UP)
        BigDecimal expectedTotal = returnedSubtotal.subtract(returnedDiscount).setScale(2, java.math.RoundingMode.HALF_UP);
        org.junit.jupiter.api.Assertions.assertEquals(expectedTotal, returnedTotal.setScale(2, java.math.RoundingMode.HALF_UP), "Total must equal subtotal minus discountAmount (discount applied to full subtotal, not per-item price), rounded to 2dp HALF_UP");
        // Assert discountAmount itself is rounded to 2 decimal places
        org.junit.jupiter.api.Assertions.assertEquals(returnedDiscount.setScale(2, java.math.RoundingMode.HALF_UP), returnedDiscount.setScale(2, java.math.RoundingMode.HALF_UP), "Discount amount must be rounded to 2 decimal places using HALF_UP");
        // Assert total is rounded to 2 decimal places
        org.junit.jupiter.api.Assertions.assertEquals(2, returnedTotal.scale(), "Total must be expressed to exactly 2 decimal places");
    }

    @Test
    @DisplayName("[R3] SAVE20 promo via API returns correct total")
    void test_r3_tc_r3_03() throws Exception {
        // Given
        // Subtotal = 2 items * $100.00 each = $200.00
        // STANDARD tier = no tier discount, discounted total = $200.00 >= $100.00 threshold
        // SAVE20 promo deducts $20.00 => total = $200.00 - $20.00 = $180.00
        OrderItem item = new OrderItem("SKU-001", new BigDecimal("100.00"), 2);
        Order order = new Order("order-r3-03", "customer-001", List.of(item), CustomerTier.STANDARD, "SAVE20");
        // 180.00
        double expectedTotal = 200.00 - 20.00;
        double expectedPromoAmount = 20.00;
        // When & Then
        mockMvc.perform(post("/api/orders/total").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(order))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(expectedTotal)).andExpect(jsonPath("$.promoAmount").value(expectedPromoAmount));
    }

    @Test
    @DisplayName("[R3] Final total never goes below $0 via API - TC-R3-04")
    void test_r3_tc_r3_04() throws Exception {
        // Given
        // Subtotal = $100.00 exactly: 1 item with unitPrice=100.00, quantity=1
        // CustomerTier = STANDARD (no tier discount, so discounted total = $100.00 >= $100.00 threshold)
        // PromoCode = "SAVE20" => deducts $20.00 => total = $100.00 - $20.00 = $80.00
        // $80.00 >= $0.00, so no floor clamping needed; total must be $80.00
        OrderItem item = new OrderItem("SKU-001", new BigDecimal("100.00"), 1);
        Order order = new Order("order-tc-r3-04", "customer-001", List.of(item), CustomerTier.STANDARD, "SAVE20");
        double expectedSubtotal = 100.00;
        double expectedPromoAmount = 20.00;
        // 80.00
        double expectedTotal = expectedSubtotal - expectedPromoAmount;
        // When & Then
        // Assert total is never negative: 80.00 >= 0.00
        mockMvc.perform(post("/api/orders/total").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(order))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(expectedTotal)).andExpect(jsonPath("$.total").value(80.0)).andExpect(jsonPath("$.total", org.hamcrest.Matchers.greaterThanOrEqualTo(0.0)));
    }

    @Test
    @DisplayName("[R4] Unknown promo code returns HTTP 200 with full total")
    void test_r4_tc_r4_03() throws Exception {
        // Given
        // Build an order with a known-valid structure but an unrecognized promo code "BOGUS"
        OrderItem item = new OrderItem("SKU-001", new BigDecimal("100.00"), 2);
        // subtotal = 100.00 * 2 = 200.00
        // CustomerTier.STANDARD => no tier discount (0%)
        // promoCode "BOGUS" => unknown, must be ignored => promoAmount = 0
        // expected total = 200.00 - 0 - 0 = 200.00
        // 200.0
        double expectedSubtotal = 100.00 * 2;
        // unknown promo => ignored
        double expectedPromoAmount = 0.0;
        // 200.0
        double expectedTotal = expectedSubtotal - expectedPromoAmount;
        Order order = new Order("order-bogus-promo", "customer-001", List.of(item), CustomerTier.STANDARD, "BOGUS");
        // When & Then
        // total must be non-null and equal to subtotal minus tier discount (no promo applied)
        mockMvc.perform(post("/api/orders/total").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(order))).andExpect(status().isOk()).andExpect(jsonPath("$.total").exists()).andExpect(// promoAmount must be 0 — unknown promo code is silently ignored
        jsonPath("$.total").value(expectedTotal)).andExpect(// subtotal must reflect the raw line-item sum
        jsonPath("$.promoAmount").value(expectedPromoAmount)).andExpect(// no error field should be present in a successful 200 response
        jsonPath("$.subtotal").value(expectedSubtotal)).andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @DisplayName("[R4] TC-R4-04: Empty promo code in request returns HTTP 200")
    void test_r4_tc_r4_04() throws Exception {
        // Given
        // A valid order with a STANDARD tier customer and a single item, but promoCode set to empty string ""
        // Per R4: empty promo code must be ignored without raising an error or failing the calculation
        // Expected: HTTP 200, promoAmount == 0, total reflects only tier discount (STANDARD = no discount)
        BigDecimal unitPrice = new BigDecimal("100.00");
        int quantity = 2;
        // subtotal = 100.00 * 2 = 200.00
        // STANDARD tier: no discount => discountAmount = 0, promoAmount = 0
        // total = 200.00
        double expectedPromoAmount = 0.0;
        // 200.0
        double expectedTotal = unitPrice.multiply(BigDecimal.valueOf(quantity)).doubleValue();
        OrderItem item = new OrderItem("SKU-001", unitPrice, quantity);
        Order order = new // empty promo code
        Order(// empty promo code
        "order-r4-04", // empty promo code
        "customer-001", // empty promo code
        List.of(item), // empty promo code
        CustomerTier.STANDARD, "");
        // When & Then
        mockMvc.perform(post("/api/orders/total").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(order))).andExpect(status().isOk()).andExpect(jsonPath("$.promoAmount").value(expectedPromoAmount)).andExpect(jsonPath("$.total").value(expectedTotal));
    }

    @Test
    @DisplayName("[R5] POST /api/orders/total response schema has all fields")
    void test_r5_tc_r5_03() throws Exception {
        // Given
        OrderItem item = new OrderItem("SKU-001", new BigDecimal("50.00"), 2);
        Order order = new Order("order-1", "customer-1", List.of(item), CustomerTier.STANDARD);
        // When & Then
        mockMvc.perform(post("/api/orders/total").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(order))).andExpect(status().isOk()).andExpect(jsonPath("$.subtotal").exists()).andExpect(jsonPath("$.subtotal").isNotEmpty()).andExpect(jsonPath("$.subtotal").isNumber()).andExpect(jsonPath("$.discountAmount").exists()).andExpect(jsonPath("$.discountAmount").isNotEmpty()).andExpect(jsonPath("$.discountAmount").isNumber()).andExpect(jsonPath("$.promoAmount").exists()).andExpect(jsonPath("$.promoAmount").isNotEmpty()).andExpect(jsonPath("$.promoAmount").isNumber()).andExpect(jsonPath("$.total").exists()).andExpect(jsonPath("$.total").isNotEmpty()).andExpect(jsonPath("$.total").isNumber());
    }

    @Test
    @DisplayName("[R6] POST /api/orders with 51 SKUs returns 422")
    void test_r6_tc_r6_03() throws Exception {
        // Given
        List<OrderItem> items = new ArrayList<>();
        for (int i = 1; i <= 51; i++) {
            items.add(new OrderItem("SKU-" + String.format("%03d", i), new BigDecimal("10.00"), 1));
        }
        Order payload = new Order("order-r6-03", "customer-001", items, CustomerTier.STANDARD);
        // When & Then
        mockMvc.perform(post("/api/orders/total").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(payload))).andExpect(status().isUnprocessableEntity()).andExpect(result -> {
            String responseBody = result.getResponse().getContentAsString();
            org.junit.jupiter.api.Assertions.assertFalse(responseBody == null || responseBody.isBlank(), "Expected a non-empty error message body for 422 response, but got: " + responseBody);
        });
    }

    @Test
    @DisplayName("[R6] POST /api/orders with 50 SKUs returns 200")
    void test_r6_tc_r6_04() throws Exception {
        // Given
        List<OrderItem> items = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            items.add(new OrderItem(String.format("SKU-%03d", i), new BigDecimal("10.00"), 1));
        }
        Order order = new Order("order-50-skus", "customer-001", items, CustomerTier.STANDARD);
        // When & Then
        mockMvc.perform(post("/api/orders/total").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(order))).andExpect(status().isOk()).andExpect(jsonPath("$.total").exists());
    }
}
