package com.example.orderinventory.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerReportResponse {

    private Long customerId;
    private String customerName;
    private Long numberOfOrders;
    private BigDecimal totalAmountSpent;
    private BigDecimal averageOrderValue;
}
