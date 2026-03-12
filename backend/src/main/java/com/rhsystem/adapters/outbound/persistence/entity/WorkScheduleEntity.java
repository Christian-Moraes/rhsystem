package com.rhsystem.adapters.outbound.persistence.entity;

import com.rhsystem.domain.model.WorkSchedule;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "work_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "daily_hours", nullable = false)
    private Integer dailyHours;

    @Column(name = "entry_time", nullable = false)
    private LocalTime entryTime;

    @Column(name = "exit_time", nullable = false)
    private LocalTime exitTime;

    @Column(name = "break_minutes", nullable = false)
    private Integer breakMinutes;

    @Column(name = "tolerance_late", nullable = false)
    private Integer toleranceLate;

    @Column(name = "tolerance_leave", nullable = false)
    private Integer toleranceLeave;

    public WorkSchedule toDomain() {
        return new WorkSchedule(id, companyId, dailyHours, entryTime, exitTime, 
                                breakMinutes, toleranceLate, toleranceLeave);
    }

    public static WorkScheduleEntity fromDomain(WorkSchedule workSchedule) {
        WorkScheduleEntity entity = new WorkScheduleEntity();
        entity.setId(workSchedule.getId());
        entity.setCompanyId(workSchedule.getCompanyId());
        entity.setDailyHours(workSchedule.getDailyHours());
        entity.setEntryTime(workSchedule.getEntryTime());
        entity.setExitTime(workSchedule.getExitTime());
        entity.setBreakMinutes(workSchedule.getBreakMinutes());
        entity.setToleranceLate(workSchedule.getToleranceLate());
        entity.setToleranceLeave(workSchedule.getToleranceLeave());
        return entity;
    }
}
