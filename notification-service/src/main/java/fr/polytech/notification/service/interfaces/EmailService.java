package fr.polytech.notification.service.interfaces;

import fr.polytech.notification.service.model.EmailNotificationRequest;

public interface EmailService {

    void sendSimpleMail(EmailNotificationRequest request) throws Exception;
    void sendHtmlMail(EmailNotificationRequest request) throws Exception;

}
