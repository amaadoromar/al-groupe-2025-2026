package org.eSante.interfaces;

import org.eSante.model.EmailNotificationRequest;

public interface EmailService {

    void sendSimpleMail(EmailNotificationRequest request) throws Exception;
    void sendHtmlMail(EmailNotificationRequest request) throws Exception;

}
