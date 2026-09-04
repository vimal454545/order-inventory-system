package com.example.orderinventory.service;

import com.example.orderinventory.dto.product.ProductCreateRequest;
import com.example.orderinventory.dto.product.ProductResponse;
import com.example.orderinventory.entity.Product;
import com.example.orderinventory.exception.ResourceNotFoundException;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(10L)
                .name("Gaming Laptop")
                .category("Computers")
                .price(new BigDecimal("1200.00"))
                .availableQuantity(15)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Product creation successfully saves active product")
    void testCreateProduct() {
        ProductCreateRequest request = ProductCreateRequest.builder()
                .name("Gaming Laptop")
                .category("Computers")
                .price(new BigDecimal("1200.00"))
                .availableQuantity(15)
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.createProduct(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getActive()).isTrue();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Product deactivation soft deletes product by setting active=false")
    void testDeactivateProduct() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.deactivateProduct(10L);

        assertThat(response.getActive()).isFalse();
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Deactivating non-existent product throws ResourceNotFoundException")
    void testDeactivateProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deactivateProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with id: 99");
    }
}
