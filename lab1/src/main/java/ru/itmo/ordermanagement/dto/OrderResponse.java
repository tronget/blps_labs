package ru.itmo.ordermanagement.dto;

import lombok.Builder;
import lombok.Data;
import ru.itmo.ordermanagement.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO для ответа с информацией о заказе.
 */
@Data
@Builder
public class OrderResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private Long sellerId;
    private String sellerName;
    private Long courierId;
    private String courierName;
    private OrderStatus status;
    private BigDecimal totalPrice;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String cancelReason;
}
