package com.example.orderinventory.service;

import com.example.orderinventory.dto.report.CustomerReportResponse;
import com.example.orderinventory.dto.report.ProductReportResponse;
import com.example.orderinventory.dto.report.TopProductResponse;
import com.example.orderinventory.entity.Customer;
import com.example.orderinventory.entity.Order;
import com.example.orderinventory.entity.OrderStatus;
import com.example.orderinventory.exception.ResourceNotFoundException;
import com.example.orderinventory.repository.CustomerRepository;
import com.example.orderinventory.repository.OrderItemRepository;
import com.example.orderinventory.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional(readOnly = true)
    public CustomerReportResponse getCustomerReport(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        List<Order> customerOrders = orderRepository.findByCustomerId(customerId);

        List<Order> validOrders = customerOrders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .collect(Collectors.toList());

        long numberOfOrders = validOrders.size();
        BigDecimal totalAmountSpent = validOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageOrderValue = BigDecimal.ZERO;
        if (numberOfOrders > 0) {
            averageOrderValue = totalAmountSpent.divide(BigDecimal.valueOf(numberOfOrders), 2, RoundingMode.HALF_UP);
        }

        return CustomerReportResponse.builder()
                .customerId(customer.getId())
                .customerName(customer.getName())
                .numberOfOrders(numberOfOrders)
                .totalAmountSpent(totalAmountSpent)
                .averageOrderValue(averageOrderValue)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ProductReportResponse> getProductReports() {
        return orderItemRepository.getProductSalesReports();
    }

    @Transactional(readOnly = true)
    public List<TopProductResponse> getTopProducts(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit parameter must be greater than zero");
        }

        List<ProductReportResponse> reports = orderItemRepository.getProductSalesReports();

        return reports.stream()
                .limit(limit)
                .map(r -> TopProductResponse.builder()
                        .productId(r.getProductId())
                        .productName(r.getProductName())
                        .quantitySold(r.getQuantitySold())
                        .totalRevenue(r.getTotalRevenue())
                        .build())
                .collect(Collectors.toList());
    }
}
