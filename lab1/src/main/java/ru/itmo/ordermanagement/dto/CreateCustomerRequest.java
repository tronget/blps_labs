package ru.itmo.ordermanagement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCustomerRequest {
    @NotBlank(message = "Name is required")
    private String name;
    private String email;
    private String phone;
}
