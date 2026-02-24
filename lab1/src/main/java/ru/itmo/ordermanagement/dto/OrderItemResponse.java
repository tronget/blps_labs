package ru.itmo.ordermanagement.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemResponse {
    private Long id;
    private String productName;
    private Integer quantity;
    private BigDecimal price;
}
