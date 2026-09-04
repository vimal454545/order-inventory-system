package com.example.orderinventory.service;

import com.example.orderinventory.dto.customer.CustomerCreateRequest;
import com.example.orderinventory.dto.customer.CustomerResponse;
import com.example.orderinventory.entity.Customer;
import com.example.orderinventory.exception.DuplicateResourceException;
import com.example.orderinventory.repository.CustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    @DisplayName("Test 6: Duplicate customer email cannot be created and throws DuplicateResourceException")
    void test6_DuplicateCustomerEmailFails() {
        // Arrange
        CustomerCreateRequest request = CustomerCreateRequest.builder()
                .name("Alice Smith")
                .email("duplicate@example.com")
                .phone("9876543210")
                .build();

        when(customerRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Customer already exists with email: duplicate@example.com");

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    @DisplayName("Successful customer creation returns CustomerResponse")
    void testSuccessfulCustomerCreation() {
        // Arrange
        CustomerCreateRequest request = CustomerCreateRequest.builder()
                .name("Bob Builder")
                .email("bob@example.com")
                .phone("1122334455")
                .build();

        Customer savedCustomer = Customer.builder()
                .id(1L)
                .name("Bob Builder")
                .email("bob@example.com")
                .phone("1122334455")
                .createdAt(LocalDateTime.now())
                .build();

        when(customerRepository.existsByEmail("bob@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);

        // Act
        CustomerResponse response = customerService.createCustomer(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("bob@example.com");
        verify(customerRepository).save(any(Customer.class));
    }
}
