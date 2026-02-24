package ru.itmo.ordermanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.itmo.ordermanagement.model.entity.Notification;
import ru.itmo.ordermanagement.model.enums.RecipientType;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(
            RecipientType recipientType, Long recipientId);

    List<Notification> findByRecipientTypeAndRecipientIdAndIsReadFalseOrderByCreatedAtDesc(
            RecipientType recipientType, Long recipientId);
}
