package com.example.orderinventory.service;

import com.example.orderinventory.dto.report.CustomerReportResponse;
import com.example.orderinventory.dto.report.ProductReportResponse;
import com.example.orderinventory.dto.report.TopProductResponse;
import com.example.orderinventory.entity.Customer;
import com.example.orderinventory.entity.Order;
import com.example.orderinventory.entity.OrderStatus;
import com.example.orderinventory.repository.CustomerRepository;
import com.example.orderinventory.repository.OrderItemRepository;
import com.example.orderinventory.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private ReportService reportService;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(1L)
                .name("Jane Smith")
                .email("jane@example.com")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Customer report accurately aggregates total orders, total spent, and average order value excluding cancelled orders")
    void testGetCustomerReport() {
        Order order1 = Order.builder().id(101L).customer(customer).status(OrderStatus.CREATED).totalAmount(new BigDecimal("100.00")).build();
        Order order2 = Order.builder().id(102L).customer(customer).status(OrderStatus.COMPLETED).totalAmount(new BigDecimal("200.00")).build();
        Order order3 = Order.builder().id(103L).customer(customer).status(OrderStatus.CANCELLED).totalAmount(new BigDecimal("150.00")).build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(orderRepository.findByCustomerId(1L)).thenReturn(List.of(order1, order2, order3));

        CustomerReportResponse report = reportService.getCustomerReport(1L);

        assertThat(report.getCustomerId()).isEqualTo(1L);
        assertThat(report.getCustomerName()).isEqualTo("Jane Smith");
        assertThat(report.getNumberOfOrders()).isEqualTo(2); // excluding order3 (CANCELLED)
        assertThat(report.getTotalAmountSpent()).isEqualByComparingTo(new BigDecimal("300.00")); // 100 + 200
        assertThat(report.getAverageOrderValue()).isEqualByComparingTo(new BigDecimal("150.00")); // 300 / 2
    }

    @Test
    @DisplayName("Top products report respects limit parameter")
    void testGetTopProductsWithLimit() {
        ProductReportResponse p1 = ProductReportResponse.builder().productId(1L).productName("Mouse").quantitySold(50L).totalRevenue(new BigDecimal("1250.00")).build();
        ProductReportResponse p2 = ProductReportResponse.builder().productId(2L).productName("Keyboard").quantitySold(30L).totalRevenue(new BigDecimal("2250.00")).build();
        ProductReportResponse p3 = ProductReportResponse.builder().productId(3L).productName("Monitor").quantitySold(10L).totalRevenue(new BigDecimal("3000.00")).build();

        when(orderItemRepository.getProductSalesReports()).thenReturn(List.of(p1, p2, p3));

        List<TopProductResponse> topProducts = reportService.getTopProducts(2);

        assertThat(topProducts).hasSize(2);
        assertThat(topProducts.get(0).getProductName()).isEqualTo("Mouse");
        assertThat(topProducts.get(1).getProductName()).isEqualTo("Keyboard");
    }

    @Test
    @DisplayName("Top products with invalid limit throws IllegalArgumentException")
    void testGetTopProductsInvalidLimit() {
        assertThatThrownBy(() -> reportService.getTopProducts(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Limit parameter must be greater than zero");
    }
}
