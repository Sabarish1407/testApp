package com.example.testApp.controller;

import com.example.testApp.dto.OrderResponse;
import com.example.testApp.model.Order;
import com.example.testApp.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/total")
    public ResponseEntity<OrderResponse> calculateTotal(@Valid @RequestBody Order order) {
        OrderResponse response = orderService.calculatePricing(order);
        return ResponseEntity.ok(response);
    }
}
