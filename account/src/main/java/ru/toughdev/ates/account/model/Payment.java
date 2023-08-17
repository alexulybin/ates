package ru.toughdev.ates.account.model;

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
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Data
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "PAYMENTS")
public class Payment {

    @Id
    @SequenceGenerator(name = "payment_seq", sequenceName = "payment_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payment_seq")
    private Long id;

    private final UUID publicId = UUID.randomUUID();
    private Long userId;
    private Long taskId;
    private Long amount;
    private String type;
    private LocalDateTime dateTime;
}
