package org.eSante.notification;


import org.eSante.notification.api.dto.NotificationRequest;
import org.eSante.notification.domain.NotificationChannelType;
import org.eSante.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class NotificationServiceTest {

    @Autowired
    NotificationService notificationService;

    @Test
    void createAndSend_basic() {
        NotificationRequest req = new NotificationRequest();
        req.recipientId = "user-1";
        req.title = "Test";
        req.content = "Hello";
        req.channels = List.of(NotificationChannelType.IN_APP, NotificationChannelType.EMAIL);

        var resp = notificationService.createAndSend(req);
        assertNotNull(resp.id);
        assertEquals("user-1", resp.recipientId);
        assertFalse(resp.deliveries.isEmpty());
    }
}

