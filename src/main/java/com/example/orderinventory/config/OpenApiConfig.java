package com.example.orderinventory.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mini Order & Inventory Management System API")
                        .version("1.0.0")
                        .description("Production-quality RESTful backend for managing products, customers, orders, inventory, and analytics reports.")
                        .contact(new Contact()
                                .name("Backend Engineering Team")
                                .email("engineering@example.com")));
    }
}
