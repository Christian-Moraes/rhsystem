package com.rhsystem.adapters.outbound.persistence.entity;

import com.rhsystem.domain.model.TimeEvent;
import com.rhsystem.domain.model.TimeEventType;
import com.rhsystem.domain.model.TimeEventStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "time_events", indexes = {
    @Index(name = "idx_time_events_employee_time", columnList = "employee_id, event_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TimeEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private TimeEventType eventType;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    @Column(nullable = false)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeEventStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public TimeEvent toDomain() {
        return new TimeEvent(id, employeeId, eventType, eventTime, source, status, createdAt);
    }

    public static TimeEventEntity fromDomain(TimeEvent timeEvent) {
        TimeEventEntity entity = new TimeEventEntity();
        entity.setId(timeEvent.getId());
        entity.setEmployeeId(timeEvent.getEmployeeId());
        entity.setEventType(timeEvent.getEventType());
        entity.setEventTime(timeEvent.getEventTime());
        entity.setSource(timeEvent.getSource());
        entity.setStatus(timeEvent.getStatus() != null ? timeEvent.getStatus() : TimeEventStatus.CONFIRMED);
        entity.setCreatedAt(timeEvent.getCreatedAt());
        return entity;
    }
}
