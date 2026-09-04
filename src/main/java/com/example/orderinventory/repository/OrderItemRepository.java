package com.example.orderinventory.repository;

import com.example.orderinventory.dto.report.ProductReportResponse;
import com.example.orderinventory.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Query("SELECT new com.example.orderinventory.dto.report.ProductReportResponse(" +
           "p.id, p.name, SUM(i.quantity), SUM(i.totalPrice)) " +
           "FROM OrderItem i JOIN i.product p JOIN i.order o " +
           "WHERE o.status != com.example.orderinventory.entity.OrderStatus.CANCELLED " +
           "GROUP BY p.id, p.name " +
           "ORDER BY SUM(i.quantity) DESC")
    List<ProductReportResponse> getProductSalesReports();
}
