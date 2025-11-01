package org.eSante.notification.repository;


import org.eSante.notification.domain.Notification;
import org.eSante.notification.domain.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId, Pageable pageable);
    Page<Notification> findByRecipientIdAndStatusOrderByCreatedAtDesc(String recipientId, NotificationStatus status, Pageable pageable);
    Optional<Notification> findByCorrelationId(String correlationId);
}

