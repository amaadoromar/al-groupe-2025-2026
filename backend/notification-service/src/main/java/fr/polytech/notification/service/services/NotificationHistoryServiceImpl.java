package fr.polytech.notification.service.services;

import fr.polytech.notification.service.interfaces.NotificationHistoryService;
import fr.polytech.notification.service.model.EmailNotificationLog;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class NotificationHistoryServiceImpl implements NotificationHistoryService {
    private static final int MAX_ITEMS = 1000;
    private final ConcurrentLinkedDeque<EmailNotificationLog> emails = new ConcurrentLinkedDeque<>();

    @Override
    public void addEmail(EmailNotificationLog log) {
        emails.addLast(log);
        while (emails.size() > MAX_ITEMS) {
            emails.pollFirst();
        }
    }

    @Override
    public List<EmailNotificationLog> listEmails(Long patientId, String to, String authorEmail, int limit) {
        List<EmailNotificationLog> out = new ArrayList<>();
        for (EmailNotificationLog e : emails) {
            if (patientId != null && (e.getPatientId() == null || !patientId.equals(e.getPatientId()))) continue;
            if (to != null && !to.isBlank() && (e.getTo() == null || !e.getTo().equalsIgnoreCase(to))) continue;
            if (authorEmail != null && !authorEmail.isBlank() && (e.getAuthorEmail() == null || !e.getAuthorEmail().equalsIgnoreCase(authorEmail))) continue;
            out.add(e);
        }
        Collections.reverse(out);
        if (limit > 0 && out.size() > limit) {
            return out.subList(0, limit);
        }
        return out;
    }
}
