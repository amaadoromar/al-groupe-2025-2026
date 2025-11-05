package fr.polytech.notification.service.interfaces;

import fr.polytech.notification.service.model.EmailNotificationLog;

import java.util.List;

public interface NotificationHistoryService {
    void addEmail(EmailNotificationLog log);
    List<EmailNotificationLog> listEmails(Long patientId, String to, String authorEmail, int limit);
}
