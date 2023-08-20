package ru.toughdev.ates.analytics.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

    private String eventType;

    private UUID publicId;
    private UUID userPublicId;
    private UUID taskPublicId;
    private Long amount;
    private String type;
    private LocalDateTime dateTime;
}
