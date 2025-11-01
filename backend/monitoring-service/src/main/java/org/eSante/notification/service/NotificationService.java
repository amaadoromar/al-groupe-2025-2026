package org.eSante.notification.service;


import org.eSante.notification.api.dto.DeliveryDTO;
import org.eSante.notification.api.dto.NotificationRequest;
import org.eSante.notification.api.dto.NotificationResponse;
import org.eSante.notification.domain.*;
import org.eSante.notification.repository.NotificationDeliveryRepository;
import org.eSante.notification.repository.NotificationRepository;
import org.eSante.notification.service.channel.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final Map<String, NotificationChannel> channelMap;
    private final NotificationStreamService streamService;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationDeliveryRepository deliveryRepository,
                               List<NotificationChannel> channels,
                               NotificationStreamService streamService) {
        this.notificationRepository = notificationRepository;
        this.deliveryRepository = deliveryRepository;
        this.channelMap = channels.stream().collect(Collectors.toMap(NotificationChannel::name, c -> c));
        this.streamService = streamService;
    }

    @Transactional
    public NotificationResponse createAndSend(NotificationRequest req) {
        // Idempotency by correlationId if provided
        if (req.correlationId != null && !req.correlationId.isBlank()) {
            Optional<Notification> existing = notificationRepository.findByCorrelationId(req.correlationId);
            if (existing.isPresent()) {
                log.info("Idempotent request matched correlationId={}, returning existing notification {}", req.correlationId, existing.get().getId());
                return toResponse(existing.get());
            }
        }

        Notification n = new Notification();
        n.setRecipientId(req.recipientId);
        n.setTitle(req.title);
        n.setContent(req.content);
        n.setSeverity(req.severity);
        n.setCorrelationId(req.correlationId);

        // initialize deliveries per requested channels
        for (NotificationChannelType ch : req.channels) {
            NotificationDelivery d = new NotificationDelivery();
            d.setNotification(n);
            d.setChannel(ch);
            n.getDeliveries().add(d);
        }

        notificationRepository.save(n);

        // Attempt sending on each channel in given order
        boolean anySent = false;
        boolean anyFailed = false;
        for (NotificationChannelType ch : req.channels) {
            NotificationDelivery d = n.getDeliveries().stream().filter(dd -> dd.getChannel() == ch).findFirst().orElse(null);
            if (d == null) continue;
            try {
                d.setAttempts(d.getAttempts() + 1);
                boolean ok = trySend(ch, n);
                if (ok) {
                    d.setStatus(DeliveryStatus.SENT);
                    d.setSentAt(Instant.now());
                    anySent = true;
                } else {
                    d.setStatus(DeliveryStatus.FAILED);
                    anyFailed = true;
                }
            } catch (Exception e) {
                log.warn("Channel {} failed: {}", ch, e.toString());
                d.setStatus(DeliveryStatus.FAILED);
                d.setLastError(e.getMessage());
                anyFailed = true;
            }
        }

        if (anySent && anyFailed) {
            n.setStatus(NotificationStatus.PARTIALLY_SENT);
        } else if (anySent) {
            n.setStatus(NotificationStatus.SENT);
            n.setSentAt(Instant.now());
        } else {
            n.setStatus(NotificationStatus.FAILED);
        }

        notificationRepository.save(n);
        deliveryRepository.saveAll(n.getDeliveries());

        NotificationResponse resp = toResponse(n);

        // Broadcast to in-app subscribers regardless if IN_APP requested, as a convenience
        streamService.broadcast(n.getRecipientId(), resp);
        return resp;
    }

    private boolean trySend(NotificationChannelType ch, Notification n) throws Exception {
        NotificationChannel channel = channelMap.get(ch.name());
        if (channel == null) {
            log.warn("Channel {} is not configured", ch);
            return false;
        }
        return channel.send(n);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listForRecipient(String recipientId, NotificationStatus status, int page, int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(Math.max(page,0), Math.min(Math.max(size,1), 100));
        var pageRes = (status == null)
                ? notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable)
                : notificationRepository.findByRecipientIdAndStatusOrderByCreatedAtDesc(recipientId, status, pageable);
        return pageRes.stream().map(this::toResponse).toList();
    }

    @Transactional
    public void markAsRead(UUID id) {
        var n = notificationRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Notification not found"));
        n.setStatus(NotificationStatus.READ);
        n.setReadAt(Instant.now());
        notificationRepository.save(n);
    }

    private NotificationResponse toResponse(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.id = n.getId();
        r.recipientId = n.getRecipientId();
        r.title = n.getTitle();
        r.content = n.getContent();
        r.severity = n.getSeverity();
        r.status = n.getStatus();
        r.createdAt = n.getCreatedAt();
        r.sentAt = n.getSentAt();
        r.readAt = n.getReadAt();
        r.correlationId = n.getCorrelationId();
        r.deliveries = n.getDeliveries().stream().map(d -> {
            DeliveryDTO dd = new DeliveryDTO();
            dd.id = d.getId();
            dd.channel = d.getChannel();
            dd.status = d.getStatus();
            dd.attempts = d.getAttempts();
            dd.lastError = d.getLastError();
            dd.sentAt = d.getSentAt();
            return dd;
        }).collect(Collectors.toList());
        return r;
    }
}

