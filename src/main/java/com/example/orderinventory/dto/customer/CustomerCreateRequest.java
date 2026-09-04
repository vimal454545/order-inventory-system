package com.example.orderinventory.dto.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCreateRequest {

    @NotBlank(message = "Customer name is mandatory")
    private String name;

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email format must be valid")
    private String email;

    private String phone;
}
