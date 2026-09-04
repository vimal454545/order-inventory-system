package com.example.orderinventory.controller;

import com.example.orderinventory.dto.order.CreateOrderRequest;
import com.example.orderinventory.dto.order.OrderResponse;
import com.example.orderinventory.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "APIs for order creation, retrieval, listing, and cancellation")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create an order", description = "Atomically validates customer, active status, and inventory before creating an order and updating product stock.")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order details", description = "Retrieves complete details of an order including line items and status")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List orders with pagination", description = "Returns paginated orders sorted by order date descending")
    public ResponseEntity<Page<OrderResponse>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<OrderResponse> response = orderService.getOrders(pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order", description = "Cancels a CREATED order and restores inventory atomically. Prevents cancelling COMPLETED or already CANCELLED orders.")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id) {
        OrderResponse response = orderService.cancelOrder(id);
        return ResponseEntity.ok(response);
    }
}
