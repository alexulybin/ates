package ru.toughdev.ates.account.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskEvent {

    private String eventType;

    private UUID publicId;
    private String description;
    private UUID assigneeId;
    private Long fee;
    private Long reward;
    private Boolean completed;
}

