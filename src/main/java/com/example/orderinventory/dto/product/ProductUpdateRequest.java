package com.example.orderinventory.dto.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateRequest {

    @NotBlank(message = "Product name is mandatory")
    private String name;

    @NotBlank(message = "Category is mandatory")
    private String category;

    @NotNull(message = "Price is mandatory")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    @NotNull(message = "Available quantity is mandatory")
    @Min(value = 0, message = "Available quantity cannot be negative")
    private Integer availableQuantity;

    private Boolean active;
}
