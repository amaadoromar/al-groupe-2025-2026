package fr.polytech.notification.service.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationRequest {

    @NotBlank(message = "Mail Recipient must not be blank")
    @Email(message = "Mail Recipient must be a valid email address")
    private String to;

    @NotBlank(message = "Mail Subject must not be blank")
    private String subject;

    @NotBlank(message = "Message must not be blank")
    private String message;

}
