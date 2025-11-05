package org.eSante.services;

import org.eSante.model.EmailNotificationLog;
import java.util.List;

public interface NotificationHistoryService {
    void addEmail(EmailNotificationLog log);
    List<EmailNotificationLog> listEmails(Long patientId, String to, String authorEmail, int limit);
}
