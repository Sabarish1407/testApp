package com.example.testApp.controller;

import com.example.testApp.model.CustomerTier;
import com.example.testApp.model.Order;
import com.example.testApp.model.OrderItem;
import com.example.testApp.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    @DisplayName("POST /api/orders/total should return 200 and total for valid order")
    void calculateTotal_withValidOrder_returnsOkAndTotal() throws Exception {
        OrderItem item = new OrderItem("SKU-1", new BigDecimal("25.00"), 2);
        Order order = new Order("ord-1", "cust-1", List.of(item), CustomerTier.STANDARD);

        when(orderService.calculateTotal(any(Order.class))).thenReturn(new BigDecimal("50.00"));

        mockMvc.perform(post("/api/orders/total")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(50.00));
    }

    @Test
    @DisplayName("POST /api/orders/total should return 400 Bad Request when customerId is blank")
    void calculateTotal_withBlankCustomerId_returnsBadRequest() throws Exception {
        OrderItem item = new OrderItem("SKU-1", new BigDecimal("25.00"), 2);
        Order order = new Order("ord-2", "", List.of(item), CustomerTier.STANDARD);

        mockMvc.perform(post("/api/orders/total")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/orders/total should return 400 Bad Request when items list is empty")
    void calculateTotal_withEmptyItems_returnsBadRequest() throws Exception {
        Order order = new Order("ord-3", "cust-3", List.of(), CustomerTier.STANDARD);

        mockMvc.perform(post("/api/orders/total")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("POST /api/orders/total should return 400 Bad Request when item quantity is less than 1")
    void calculateTotal_withInvalidItemQuantity_returnsBadRequest() throws Exception {
        OrderItem item = new OrderItem("SKU-1", new BigDecimal("25.00"), 0);
        Order order = new Order("ord-4", "cust-4", List.of(item), CustomerTier.STANDARD);

        mockMvc.perform(post("/api/orders/total")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("POST /api/orders/total should return 400 Bad Request when service throws IllegalArgumentException")
    void calculateTotal_whenServiceThrowsIllegalArgumentException_returnsBadRequest() throws Exception {
        OrderItem item = new OrderItem("SKU-1", new BigDecimal("25.00"), 1);
        Order order = new Order("ord-5", "cust-5", List.of(item), CustomerTier.STANDARD);

        when(orderService.calculateTotal(any(Order.class)))
                .thenThrow(new IllegalArgumentException("Unit price must not be negative"));

        mockMvc.perform(post("/api/orders/total")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Unit price must not be negative"));
    }
}
