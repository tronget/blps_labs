package ru.itmo.ordermanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.ordermanagement.dto.NotificationResponse;
import ru.itmo.ordermanagement.model.entity.Notification;
import ru.itmo.ordermanagement.model.entity.Order;
import ru.itmo.ordermanagement.model.enums.RecipientType;
import ru.itmo.ordermanagement.repository.NotificationRepository;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification send(RecipientType recipientType, Long recipientId,
                             Order order, String message) {
        Notification notification = Notification.builder()
                .recipientType(recipientType)
                .recipientId(recipientId)
                .order(order)
                .message(message)
                .isRead(false)
                .build();
        notification = notificationRepository.save(notification);
        log.info("Notification sent to {} #{}: {}", recipientType, recipientId, message);
        return notification;
    }

    @Transactional
    public void notifyCustomerStatusChanged(Order order) {
        String message = String.format("Изменён статус заказа #%d: \"%s\"",
                order.getId(), translateStatus(order.getStatus().name()));
        send(RecipientType.CUSTOMER, order.getCustomer().getId(), order, message);
    }

    @Transactional
    public void notifySellerNewOrder(Order order) {
        String message = String.format("Новый заказ #%d от покупателя %s",
                order.getId(), order.getCustomer().getName());
        send(RecipientType.SELLER, order.getSeller().getId(), order, message);
    }

    @Transactional
    public void notifyCourierNewDelivery(Order order) {
        String message = String.format("Уведомление о новом заказе #%d. Адрес заведения: %s",
                order.getId(), order.getSeller().getAddress());
        send(RecipientType.COURIER, order.getCourier().getId(), order, message);
    }

    public List<NotificationResponse> getNotifications(RecipientType recipientType, Long recipientId) {
        return notificationRepository
                .findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(recipientType, recipientId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<NotificationResponse> getUnreadNotifications(RecipientType recipientType, Long recipientId) {
        return notificationRepository
                .findByRecipientTypeAndRecipientIdAndIsReadFalseOrderByCreatedAtDesc(recipientType, recipientId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ru.itmo.ordermanagement.exception.ResourceNotFoundException(
                        "Notification not found: " + notificationId));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .recipientType(n.getRecipientType())
                .recipientId(n.getRecipientId())
                .orderId(n.getOrder().getId())
                .message(n.getMessage())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

    private String translateStatus(String status) {
        return switch (status) {
            case "CREATED" -> "Создан";
            case "IN_PROCESSING" -> "В обработке";
            case "COOKING" -> "Готовится";
            case "ASSEMBLING" -> "В сборке";
            case "SEARCHING_COURIER" -> "Поиск курьера";
            case "AWAITING_COURIER" -> "Ожидание курьера";
            case "DELAYED" -> "Задерживается";
            case "IN_DELIVERY" -> "В доставке";
            case "CANCELLED" -> "Отменён";
            default -> status;
        };
    }
}
