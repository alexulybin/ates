package ru.toughdev.ates.account.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {

    private String eventType;

    private UUID publicId;
    private String login;
    private String email;
    private String role;
}

