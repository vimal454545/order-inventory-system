package com.example.orderinventory.service;

import com.example.orderinventory.dto.order.*;
import com.example.orderinventory.entity.Customer;
import com.example.orderinventory.entity.Order;
import com.example.orderinventory.entity.OrderItem;
import com.example.orderinventory.entity.OrderStatus;
import com.example.orderinventory.entity.Product;
import com.example.orderinventory.exception.InsufficientInventoryException;
import com.example.orderinventory.exception.InvalidOrderException;
import com.example.orderinventory.exception.ResourceNotFoundException;
import com.example.orderinventory.repository.CustomerRepository;
import com.example.orderinventory.repository.OrderRepository;
import com.example.orderinventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        // Step 1: Validate Customer
        if (request.getCustomerId() == null) {
            throw new InvalidOrderException("Customer ID is required");
        }
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));

        // Step 2: Validate Order Items list
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new InvalidOrderException("Order must contain at least one item");
        }

        // Aggregate quantities if duplicate product IDs appear in request
        Map<Long, Integer> requestedQuantities = new HashMap<>();
        for (OrderItemRequest itemReq : request.getItems()) {
            if (itemReq.getProductId() == null) {
                throw new InvalidOrderException("Product ID is required for each order item");
            }
            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new InvalidOrderException("Quantity must be greater than zero for product ID: " + itemReq.getProductId());
            }
            requestedQuantities.merge(itemReq.getProductId(), itemReq.getQuantity(), Integer::sum);
        }

        // Step 3 & 4: Validate EVERY product exists, is active, and has sufficient stock FIRST
        Map<Long, Product> productMap = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : requestedQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer totalRequestedQty = entry.getValue();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new InvalidOrderException("Product is inactive and cannot be ordered: " + product.getName() + " (ID: " + productId + ")");
            }

            if (product.getAvailableQuantity() < totalRequestedQty) {
                throw new InsufficientInventoryException("Insufficient inventory for product '" + product.getName() + "'. Requested: " + totalRequestedQty + ", Available: " + product.getAvailableQuantity());
            }

            productMap.put(productId, product);
        }

        // Step 5, 6 & 7: Calculate totals, preserve historical prices, construct Order & OrderItems
        Order order = Order.builder()
                .customer(customer)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.CREATED)
                .items(new ArrayList<>())
                .build();

        BigDecimal grandTotal = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : request.getItems()) {
            Product product = productMap.get(itemReq.getProductId());
            BigDecimal unitPrice = product.getPrice();
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(unitPrice)
                    .totalPrice(totalPrice)
                    .build();

            order.addItem(orderItem);
            grandTotal = grandTotal.add(totalPrice);
        }

        order.setTotalAmount(grandTotal);

        // Step 8: Reduce inventory after ALL validations pass
        for (Map.Entry<Long, Integer> entry : requestedQuantities.entrySet()) {
            Product product = productMap.get(entry.getKey());
            product.setAvailableQuantity(product.getAvailableQuantity() - entry.getValue());
            productRepository.save(product);
        }

        Order savedOrder = orderRepository.save(order);
        return mapToResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerId(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found with id: " + customerId);
        }
        return orderRepository.findByCustomerId(customerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderException("Order with id " + id + " is already CANCELLED");
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new InvalidOrderException("Completed orders cannot be cancelled");
        }

        // Status is CREATED -> set CANCELLED and restore inventory
        order.setStatus(OrderStatus.CANCELLED);

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setAvailableQuantity(product.getAvailableQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        Order savedOrder = orderRepository.save(order);
        return mapToResponse(savedOrder);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderException("Cannot change status of a cancelled order");
        }

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);
        return mapToResponse(saved);
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getTotalPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getName())
                .orderDate(order.getOrderDate())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .items(itemResponses)
                .build();
    }
}
