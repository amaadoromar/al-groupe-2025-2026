package fr.polytech.notification.service.services;

import fr.polytech.notification.service.interfaces.EmailService;
import fr.polytech.notification.service.model.EmailNotificationRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Override
    public void sendSimpleMail(EmailNotificationRequest request) throws Exception {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (fromAddress != null && !fromAddress.isBlank()) {
                message.setFrom(fromAddress);
            }
            message.setTo(request.getTo());
            message.setSubject(request.getSubject());
            message.setText(request.getMessage());

            mailSender.send(message);

            log.info("Email sent to {}", request.getTo());
        } catch (Exception e) {
            log.error("Error sending email to {}: {}", request.getTo(), e.getMessage());
            throw new Exception("Failed to send email: " + e.getMessage());
        }
    }

    @Override
    public void sendHtmlMail(EmailNotificationRequest request) throws Exception {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            if (fromAddress != null && !fromAddress.isBlank()) {
                helper.setFrom(fromAddress);
            }
            helper.setTo(request.getTo());
            helper.setSubject(request.getSubject());
            helper.setText(request.getMessage(), true);

            mailSender.send(mimeMessage);

            log.info("HTML Email sent to {}", request.getTo());
        } catch (MessagingException e) {
            log.error("Error sending HTML email to {}: {}", request.getTo(), e.getMessage());
            throw new Exception("Failed to send HTML email: " + e.getMessage());
        }
    }
}
