package com.example.orderinventory.controller;

import com.example.orderinventory.dto.report.CustomerReportResponse;
import com.example.orderinventory.dto.report.ProductReportResponse;
import com.example.orderinventory.dto.report.TopProductResponse;
import com.example.orderinventory.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Reporting & Analytics", description = "APIs for customer spending reports and product sales analytics")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/customers/{customerId}")
    @Operation(summary = "Get customer spending report", description = "Calculates total orders, total amount spent, and average order value for a given customer")
    public ResponseEntity<CustomerReportResponse> getCustomerReport(@PathVariable Long customerId) {
        CustomerReportResponse response = reportService.getCustomerReport(customerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/products")
    @Operation(summary = "Get product sales report", description = "Returns products sorted by quantity sold descending with total revenue generated")
    public ResponseEntity<List<ProductReportResponse>> getProductReport() {
        List<ProductReportResponse> response = reportService.getProductReports();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/top-products")
    @Operation(summary = "Get top N selling products", description = "Returns top N products based on total quantity sold (default limit=5)")
    public ResponseEntity<List<TopProductResponse>> getTopProducts(@RequestParam(defaultValue = "5") int limit) {
        List<TopProductResponse> response = reportService.getTopProducts(limit);
        return ResponseEntity.ok(response);
    }
}
