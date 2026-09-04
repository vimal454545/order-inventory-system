package com.example.orderinventory.controller;

import com.example.orderinventory.dto.order.CreateOrderRequest;
import com.example.orderinventory.dto.order.OrderItemRequest;
import com.example.orderinventory.dto.order.OrderResponse;
import com.example.orderinventory.entity.OrderStatus;
import com.example.orderinventory.exception.InsufficientInventoryException;
import com.example.orderinventory.exception.InvalidOrderException;
import com.example.orderinventory.exception.ResourceNotFoundException;
import com.example.orderinventory.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    @DisplayName("POST /orders creates order and returns HTTP 201 CREATED")
    void testCreateOrderSuccess() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId(101L)
                .items(List.of(OrderItemRequest.builder().productId(1L).quantity(2).build()))
                .build();

        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .customerId(101L)
                .customerName("John Doe")
                .orderDate(LocalDateTime.now())
                .totalAmount(new BigDecimal("50.00"))
                .status(OrderStatus.CREATED)
                .build();

        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalAmount").value(50.00));
    }

    @Test
    @DisplayName("POST /orders with non-existent customer returns HTTP 404 NOT FOUND with structured ErrorResponse")
    void testCreateOrderInvalidCustomerReturns404() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId(999L)
                .items(List.of(OrderItemRequest.builder().productId(1L).quantity(2).build()))
                .build();

        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenThrow(new ResourceNotFoundException("Customer not found with id: 999"));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Customer not found with id: 999"));
    }

    @Test
    @DisplayName("POST /orders with insufficient inventory returns HTTP 400 BAD REQUEST")
    void testCreateOrderInsufficientInventoryReturns400() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId(101L)
                .items(List.of(OrderItemRequest.builder().productId(1L).quantity(100).build()))
                .build();

        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenThrow(new InsufficientInventoryException("Insufficient inventory for product 'Wireless Mouse'"));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_INVENTORY"))
                .andExpect(jsonPath("$.message").value("Insufficient inventory for product 'Wireless Mouse'"));
    }

    @Test
    @DisplayName("PUT /orders/{id}/cancel cancelling COMPLETED order returns HTTP 400 BAD REQUEST")
    void testCancelCompletedOrderReturns400() throws Exception {
        when(orderService.cancelOrder(1L))
                .thenThrow(new InvalidOrderException("Completed orders cannot be cancelled"));

        mockMvc.perform(put("/orders/1/cancel"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INVALID_ORDER"))
                .andExpect(jsonPath("$.message").value("Completed orders cannot be cancelled"));
    }
}
