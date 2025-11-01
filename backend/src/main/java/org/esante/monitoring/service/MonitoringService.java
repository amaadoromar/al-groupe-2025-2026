package org.esante.monitoring.service;

import org.esante.monitoring.api.dto.EventResponse;
import org.esante.monitoring.api.dto.MeasurementRequest;
import org.esante.monitoring.api.dto.MeasurementResponse;
import org.esante.monitoring.domain.*;
import org.esante.monitoring.repository.MeasurementRepository;
import org.esante.monitoring.repository.MonitoringEventRepository;
import org.esante.notification.api.dto.NotificationRequest;
import org.esante.notification.api.dto.NotificationResponse;
import org.esante.notification.domain.NotificationChannelType;
import org.esante.notification.domain.NotificationSeverity;
import org.esante.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
public class MonitoringService {
    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);

    private final MeasurementRepository measurementRepository;
    private final MonitoringEventRepository eventRepository;
    private final NotificationService notificationService;

    public MonitoringService(MeasurementRepository measurementRepository,
                             MonitoringEventRepository eventRepository,
                             NotificationService notificationService) {
        this.measurementRepository = measurementRepository;
        this.eventRepository = eventRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public MeasurementResponse ingest(MeasurementRequest req) {
        Measurement m = new Measurement();
        m.setPatientId(req.patientId);
        m.setType(req.type);
        m.setValue(req.value);
        m.setValue2(req.value2);
        m.setUnit(req.unit);
        m.setMeasuredAt(req.measuredAt != null ? req.measuredAt : Instant.now());
        measurementRepository.save(m);

        Optional<MonitoringEvent> eventOpt = evaluateAndCreateEvent(m);
        eventOpt.ifPresent(eventRepository::save);

        eventOpt.ifPresent(ev -> maybeNotify(ev));

        MeasurementResponse resp = toResponse(m);
        eventOpt.ifPresent(ev -> {
            resp.eventId = ev.getId();
            resp.eventSeverity = ev.getSeverity();
            resp.eventMessage = ev.getMessage();
        });
        return resp;
    }

    @Transactional(readOnly = true)
    public List<EventResponse> listEvents(String patientId, EventStatus status, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        var events = (status == null)
                ? eventRepository.findByPatientIdOrderByCreatedAtDesc(patientId, pageable)
                : eventRepository.findByPatientIdAndStatusOrderByCreatedAtDesc(patientId, status, pageable);
        return events.stream().map(this::toResponse).toList();
    }

    @Transactional
    public void acknowledgeEvent(UUID id) {
        var ev = eventRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Event not found"));
        ev.setStatus(EventStatus.ACKNOWLEDGED);
        eventRepository.save(ev);
    }

    @Transactional
    public void resolveEvent(UUID id) {
        var ev = eventRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Event not found"));
        ev.setStatus(EventStatus.RESOLVED);
        ev.setResolvedAt(Instant.now());
        eventRepository.save(ev);
    }

    @Transactional(readOnly = true)
    public List<MeasurementResponse> latestByType(String patientId) {
        return java.util.Arrays.stream(MeasurementType.values())
                .map(type -> measurementRepository.findTop1ByPatientIdAndTypeOrderByMeasuredAtDesc(patientId, type))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::toResponse)
                .toList();
    }

    private MeasurementResponse toResponse(Measurement m) {
        MeasurementResponse r = new MeasurementResponse();
        r.id = m.getId();
        r.patientId = m.getPatientId();
        r.type = m.getType();
        r.value = m.getValue();
        r.value2 = m.getValue2();
        r.unit = m.getUnit();
        r.measuredAt = m.getMeasuredAt();
        r.createdAt = m.getCreatedAt();
        return r;
    }

    private EventResponse toResponse(MonitoringEvent e) {
        EventResponse r = new EventResponse();
        r.id = e.getId();
        r.patientId = e.getPatientId();
        r.type = e.getType();
        r.status = e.getStatus();
        r.severity = e.getSeverity();
        r.message = e.getMessage();
        r.measurementId = e.getMeasurement() != null ? e.getMeasurement().getId() : null;
        r.createdAt = e.getCreatedAt();
        r.resolvedAt = e.getResolvedAt();
        return r;
    }

    private Optional<MonitoringEvent> evaluateAndCreateEvent(Measurement m) {
        String patientId = m.getPatientId();
        MeasurementType type = m.getType();
        double v = m.getValue();
        Double v2 = m.getValue2();

        NotificationSeverity severity = null;
        String message = null;

        switch (type) {
            case HEART_RATE -> {
                if (v < 40 || v > 130) { severity = NotificationSeverity.CRITICAL; message = "Heart rate critical: " + v + " bpm"; }
                else if (v < 50 || v > 110) { severity = NotificationSeverity.ALERT; message = "Heart rate out-of-range: " + v + " bpm"; }
            }
            case SPO2 -> {
                if (v < 88) { severity = NotificationSeverity.CRITICAL; message = "SpO2 critical: " + v + "%"; }
                else if (v < 92) { severity = NotificationSeverity.ALERT; message = "SpO2 low: " + v + "%"; }
            }
            case BLOOD_PRESSURE -> {
                double sys = v;
                double dia = v2 != null ? v2 : -1;
                if (sys >= 180 || dia >= 110 || sys < 85 || dia < 50) {
                    severity = NotificationSeverity.CRITICAL; message = "BP critical: " + sys + "/" + dia + " mmHg";
                } else if (sys >= 160 || dia >= 100 || sys < 90 || dia < 55) {
                    severity = NotificationSeverity.ALERT; message = "BP out-of-range: " + sys + "/" + dia + " mmHg";
                }
            }
            case GLUCOSE -> {
                if (v < 60 || v > 250) { severity = NotificationSeverity.CRITICAL; message = "Glucose critical: " + v + " mg/dL"; }
                else if (v < 70 || v > 180) { severity = NotificationSeverity.ALERT; message = "Glucose out-of-range: " + v + " mg/dL"; }
            }
            default -> {}
        }

        if (severity == null) return Optional.empty();

        MonitoringEvent ev = new MonitoringEvent();
        ev.setPatientId(patientId);
        ev.setType(type);
        ev.setSeverity(severity);
        ev.setMessage(message);
        ev.setMeasurement(m);
        ev.setStatus(EventStatus.OPEN);
        return Optional.of(ev);
    }

    private void maybeNotify(MonitoringEvent ev) {
        // For ALERT and CRITICAL, push an in-app notification as a first channel.
        if (ev.getSeverity() == NotificationSeverity.ALERT || ev.getSeverity() == NotificationSeverity.CRITICAL) {
            try {
                NotificationRequest req = new NotificationRequest();
                req.recipientId = ev.getPatientId();
                req.title = "Monitoring: " + ev.getSeverity();
                req.content = ev.getMessage();
                req.severity = ev.getSeverity();
                req.channels = List.of(NotificationChannelType.IN_APP);
                req.correlationId = "mon-" + ev.getId(); // basic idempotency link
                // Invoke and ignore the response; we only care about side effects
                notificationService.createAndSend(req);
            } catch (Exception e) {
                log.warn("Failed to send notification for event {}: {}", ev.getId(), e.toString());
            }
        }
    }
}
