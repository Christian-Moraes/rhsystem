package com.rhsystem.adapters.outbound.persistence.entity;

import com.rhsystem.domain.model.Timesheet;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "timesheets", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"employee_id", "month", "year"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TimesheetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "worked_hours", nullable = false)
    private Integer workedHours;

    @Column(name = "extra_hours", nullable = false)
    private Integer extraHours;

    @Column(name = "delay_minutes", nullable = false)
    private Integer delayMinutes;

    @Column(name = "absence_minutes", nullable = false)
    private Integer absenceMinutes;

    @Column(name = "closed_at", nullable = false)
    private LocalDateTime closedAt;

    public Timesheet toDomain() {
        return new Timesheet(id, employeeId, month, year, workedHours, extraHours, 
                            delayMinutes, absenceMinutes, closedAt);
    }

    public static TimesheetEntity fromDomain(Timesheet timesheet) {
        TimesheetEntity entity = new TimesheetEntity();
        entity.setId(timesheet.getId());
        entity.setEmployeeId(timesheet.getEmployeeId());
        entity.setMonth(timesheet.getMonth());
        entity.setYear(timesheet.getYear());
        entity.setWorkedHours(timesheet.getWorkedHours());
        entity.setExtraHours(timesheet.getExtraHours());
        entity.setDelayMinutes(timesheet.getDelayMinutes());
        entity.setAbsenceMinutes(timesheet.getAbsenceMinutes());
        entity.setClosedAt(timesheet.getClosedAt());
        return entity;
    }
}
