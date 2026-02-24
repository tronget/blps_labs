package ru.itmo.ordermanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.ordermanagement.dto.NotificationResponse;
import ru.itmo.ordermanagement.model.enums.RecipientType;
import ru.itmo.ordermanagement.service.NotificationService;

import java.util.List;

/**
 * REST API для работы с уведомлениями.
 * Уведомления генерируются автоматически при смене статуса заказа.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Уведомления участникам бизнес-процесса")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/{recipientType}/{recipientId}")
    @Operation(summary = "Получить все уведомления получателя",
            description = "recipientType: CUSTOMER, SELLER, COURIER")
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @PathVariable RecipientType recipientType,
            @PathVariable Long recipientId) {
        return ResponseEntity.ok(notificationService.getNotifications(recipientType, recipientId));
    }

    @GetMapping("/{recipientType}/{recipientId}/unread")
    @Operation(summary = "Получить непрочитанные уведомления получателя")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(
            @PathVariable RecipientType recipientType,
            @PathVariable Long recipientId) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(recipientType, recipientId));
    }

    @PostMapping("/{notificationId}/read")
    @Operation(summary = "Отметить уведомление как прочитанное")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }
}
