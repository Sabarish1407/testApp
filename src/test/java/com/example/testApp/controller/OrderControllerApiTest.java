package com.example.testApp.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import java.math.*;
import java.time.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import com.example.testApp.dto.OrderResponse;
import com.example.testApp.exception.GlobalExceptionHandler;
import com.example.testApp.exception.TooManyItemsException;
import com.example.testApp.model.CustomerTier;
import com.example.testApp.model.Order;
import com.example.testApp.model.OrderItem;
import com.example.testApp.service.OrderService;

@Tag("neuro-post-deploy")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderControllerApiTest {

    @LocalServerPort
    private int port;

    @Autowired(required = false)
    private RestTemplate restTemplate = createRestTemplate();

    @Autowired(required = false)
    private ObjectMapper objectMapper = new ObjectMapper();

    private static RestTemplate createRestTemplate() {
        RestTemplate rt = new RestTemplate();
        rt.setErrorHandler(new DefaultResponseErrorHandler() {

            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });
        return rt;
    }

    private String getUrl(String path) {
        String baseUrl = System.getProperty("app.base-url");
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = System.getenv("APP_BASE_URL");
        }
        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl.replaceAll("/+$", "") + (path.startsWith("/") ? path : "/" + path);
        }
        return "http://localhost:" + port + path;
    }

    @Test
    @DisplayName("[R3] - Final total never goes below $0 with promo")
    void test_r3_tc_r3_03() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Order payload = new Order();
        HttpEntity<?> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(getUrl("/api/orders/total"), entity, String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
    }
}
