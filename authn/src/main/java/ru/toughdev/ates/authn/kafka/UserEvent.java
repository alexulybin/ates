package ru.toughdev.ates.authn.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class UserEvent {

    private String eventType;

    private UUID publicId;
    private String login;
    private String email;
    private String role;
}
