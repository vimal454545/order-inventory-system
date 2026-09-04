package com.example.orderinventory.controller;

import com.example.orderinventory.dto.product.ProductCreateRequest;
import com.example.orderinventory.dto.product.ProductResponse;
import com.example.orderinventory.dto.product.ProductUpdateRequest;
import com.example.orderinventory.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "APIs for managing catalog products and inventory status")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "Add a new product", description = "Creates a new active product in the catalog")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        ProductResponse response = productService.createProduct(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing product", description = "Updates product details such as name, category, price, and stock quantity")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieves details of a single product by its unique ID")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List all products", description = "Retrieves all products. Set activeOnly=true to filter active products only.")
    public ResponseEntity<List<ProductResponse>> getAllProducts(@RequestParam(required = false, defaultValue = "true") Boolean activeOnly) {
        List<ProductResponse> response = productService.getAllProducts(activeOnly);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Search active products", description = "Searches active products by name or category matching the search query")
    public ResponseEntity<List<ProductResponse>> searchProducts(@RequestParam(required = false) String query) {
        List<ProductResponse> response = productService.searchProducts(query);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate product", description = "Soft deletes a product by setting active=false. Used when product cannot be hard deleted due to order history.")
    public ResponseEntity<ProductResponse> deactivateProduct(@PathVariable Long id) {
        ProductResponse response = productService.deactivateProduct(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate product via DELETE endpoint", description = "Deactivates product (active=false) to preserve order history.")
    public ResponseEntity<ProductResponse> deleteProduct(@PathVariable Long id) {
        ProductResponse response = productService.deactivateProduct(id);
        return ResponseEntity.ok(response);
    }
}
