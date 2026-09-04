package com.example.orderinventory.controller;

import com.example.orderinventory.dto.customer.CustomerCreateRequest;
import com.example.orderinventory.dto.customer.CustomerResponse;
import com.example.orderinventory.dto.customer.CustomerUpdateRequest;
import com.example.orderinventory.dto.order.OrderResponse;
import com.example.orderinventory.service.CustomerService;
import com.example.orderinventory.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "APIs for managing customer accounts and viewing customer order history")
public class CustomerController {

    private final CustomerService customerService;
    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create customer", description = "Registers a new customer with mandatory name and unique email")
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerCreateRequest request) {
        CustomerResponse response = customerService.createCustomer(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update customer", description = "Updates an existing customer's details")
    public ResponseEntity<CustomerResponse> updateCustomer(@PathVariable Long id, @Valid @RequestBody CustomerUpdateRequest request) {
        CustomerResponse response = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID", description = "Retrieves customer information by ID")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id) {
        CustomerResponse response = customerService.getCustomerById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List all customers", description = "Retrieves all registered customers")
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        List<CustomerResponse> response = customerService.getAllCustomers();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/orders")
    @Operation(summary = "Get orders belonging to a customer", description = "Retrieves all orders placed by the specified customer")
    public ResponseEntity<List<OrderResponse>> getCustomerOrders(@PathVariable Long id) {
        List<OrderResponse> response = orderService.getOrdersByCustomerId(id);
        return ResponseEntity.ok(response);
    }
}
