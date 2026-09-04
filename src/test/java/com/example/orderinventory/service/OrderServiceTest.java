package com.example.orderinventory.service;

import com.example.orderinventory.dto.order.CreateOrderRequest;
import com.example.orderinventory.dto.order.OrderItemRequest;
import com.example.orderinventory.dto.order.OrderResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    private Customer sampleCustomer;
    private Product productA;
    private Product productB;

    @BeforeEach
    void setUp() {
        sampleCustomer = Customer.builder()
                .id(101L)
                .name("John Doe")
                .email("john@example.com")
                .phone("1234567890")
                .createdAt(LocalDateTime.now())
                .build();

        productA = Product.builder()
                .id(1L)
                .name("Wireless Mouse")
                .category("Electronics")
                .price(new BigDecimal("25.00"))
                .availableQuantity(10)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        productB = Product.builder()
                .id(2L)
                .name("Mechanical Keyboard")
                .category("Electronics")
                .price(new BigDecimal("75.00"))
                .availableQuantity(5)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Test 1: Successful order creation with total, unit price preservation, stock reduction & status CREATED")
    void test1_SuccessfulOrderCreation() {
        // Arrange
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId(101L)
                .items(List.of(
                        OrderItemRequest.builder().productId(1L).quantity(2).build(),
                        OrderItemRequest.builder().productId(2L).quantity(1).build()
                ))
                .build();

        when(customerRepository.findById(101L)).thenReturn(Optional.of(sampleCustomer));
        when(productRepository.findById(1L)).thenReturn(Optional.of(productA));
        when(productRepository.findById(2L)).thenReturn(Optional.of(productB));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order orderToSave = invocation.getArgument(0);
            orderToSave.setId(1001L);
            long itemIdCounter = 1L;
            for (OrderItem item : orderToSave.getItems()) {
                item.setId(itemIdCounter++);
            }
            return orderToSave;
        });

        // Act
        OrderResponse response = orderService.createOrder(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1001L);
        assertThat(response.getCustomerId()).isEqualTo(101L);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.CREATED);

        // Verify total amount: (2 * 25.00) + (1 * 75.00) = 125.00
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("125.00"));

        // Verify items
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).getUnitPrice()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(response.getItems().get(0).getTotalPrice()).isEqualByComparingTo(new BigDecimal("50.00"));

        // Verify inventory reduction
        assertThat(productA.getAvailableQuantity()).isEqualTo(8); // 10 - 2
        assertThat(productB.getAvailableQuantity()).isEqualTo(4); // 5 - 1
        verify(productRepository, times(2)).save(any(Product.class));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Test 2: Insufficient inventory fails order creation without modifying stock or saving order")
    void test2_InsufficientInventoryFailsOrder() {
        // Arrange: Product A has 10, Product B has 0 stock
        productB.setAvailableQuantity(0);

        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId(101L)
                .items(List.of(
                        OrderItemRequest.builder().productId(1L).quantity(2).build(),
                        OrderItemRequest.builder().productId(2L).quantity(1).build()
                ))
                .build();

        when(customerRepository.findById(101L)).thenReturn(Optional.of(sampleCustomer));
        when(productRepository.findById(1L)).thenReturn(Optional.of(productA));
        when(productRepository.findById(2L)).thenReturn(Optional.of(productB));

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("Insufficient inventory for product 'Mechanical Keyboard'");

        // Verify stock of Product A remains 10 (no partial stock reduction)
        assertThat(productA.getAvailableQuantity()).isEqualTo(10);
        assertThat(productB.getAvailableQuantity()).isEqualTo(0);

        // Verify no order was saved and no products were saved
        verify(orderRepository, never()).save(any(Order.class));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Test 3: Invalid customer fails order creation with 404 exception and no inventory modification")
    void test3_InvalidCustomerFailsOrder() {
        // Arrange
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId(999L)
                .items(List.of(
                        OrderItemRequest.builder().productId(1L).quantity(2).build()
                ))
                .build();

        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found with id: 999");

        // Verify inventory untouched
        assertThat(productA.getAvailableQuantity()).isEqualTo(10);
        verify(productRepository, never()).save(any(Product.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Test 4: Order cancellation changes status CREATED to CANCELLED and restores inventory")
    void test4_OrderCancellationRestoresInventory() {
        // Arrange
        productA.setAvailableQuantity(8); // reduced previously by 2

        Order order = Order.builder()
                .id(500L)
                .customer(sampleCustomer)
                .orderDate(LocalDateTime.now())
                .totalAmount(new BigDecimal("50.00"))
                .status(OrderStatus.CREATED)
                .items(new ArrayList<>())
                .build();

        OrderItem item = OrderItem.builder()
                .id(10L)
                .order(order)
                .product(productA)
                .quantity(2)
                .unitPrice(new BigDecimal("25.00"))
                .totalPrice(new BigDecimal("50.00"))
                .build();
        order.addItem(item);

        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderResponse response = orderService.cancelOrder(500L);

        // Assert
        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(productA.getAvailableQuantity()).isEqualTo(10); // restored from 8 to 10
        verify(productRepository).save(productA);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("Test 5: Cancellation of COMPLETED order fails and leaves status and inventory unchanged")
    void test5_CancelCompletedOrderFails() {
        // Arrange
        Order completedOrder = Order.builder()
                .id(600L)
                .customer(sampleCustomer)
                .orderDate(LocalDateTime.now())
                .totalAmount(new BigDecimal("50.00"))
                .status(OrderStatus.COMPLETED)
                .items(new ArrayList<>())
                .build();

        OrderItem item = OrderItem.builder()
                .id(11L)
                .order(completedOrder)
                .product(productA)
                .quantity(2)
                .unitPrice(new BigDecimal("25.00"))
                .totalPrice(new BigDecimal("50.00"))
                .build();
        completedOrder.addItem(item);

        when(orderRepository.findById(600L)).thenReturn(Optional.of(completedOrder));

        // Act & Assert
        assertThatThrownBy(() -> orderService.cancelOrder(600L))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Completed orders cannot be cancelled");

        assertThat(completedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(productA.getAvailableQuantity()).isEqualTo(10);
        verify(productRepository, never()).save(any(Product.class));
    }
}
