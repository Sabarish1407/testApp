# Order Pricing Module

A Spring Boot application providing an order-pricing calculation service and REST API.

## Requirements

- Java 21
- Maven Wrapper (included)

## Tech Stack

- Java 21
- Spring Boot 4.1.2
- Spring Web MVC
- Spring Validation (Jakarta Bean Validation)
- JUnit 5, AssertJ, Mockito

## Domain Model & Features

- **Order**: Represents a customer order containing order ID, customer ID, tier (`STANDARD`, `SILVER`, `GOLD`), and a list of order items.
- **OrderItem**: Represents a line item with SKU, unit price (`BigDecimal`), and quantity (`int` >= 1).
- **OrderService**: Calculates total pricing by summing line item prices (`unitPrice * quantity`). Validates non-null order, non-empty items, non-negative unit prices, and quantities >= 1.
- **OrderController**: Exposes `POST /api/orders/total` accepting an order payload in JSON and returning the calculated total.

## Running Tests

Run unit and integration tests using Maven Wrapper:

```bash
# On Linux/macOS
./mvnw test

# On Windows
.\mvnw.cmd test
```

To compile tests only:

```bash
# On Linux/macOS
./mvnw test-compile

# On Windows
.\mvnw.cmd test-compile
```
