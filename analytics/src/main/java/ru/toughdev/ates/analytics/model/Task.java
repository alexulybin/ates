package ru.toughdev.ates.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "TASKS")
public class Task {

    @Id
    @SequenceGenerator(name = "task_seq", sequenceName = "task_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_seq")
    private Long id;

    private UUID publicId;
    private String description;
    private String jiraId;
    private Long assigneeId;
    private Long fee;
    private Long reward;
    @Builder.Default
    private Boolean completed = false;
    private LocalDate completeDate;
}
