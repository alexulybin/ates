package ru.toughdev.ates.analytics.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountEvent {

    private String eventType;

    private UUID userPublicId;
    private Long balance;
}
