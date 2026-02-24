package ru.itmo.ordermanagement.dto;

import lombok.Builder;
import lombok.Data;
import ru.itmo.ordermanagement.model.enums.RecipientType;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private RecipientType recipientType;
    private Long recipientId;
    private Long orderId;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
