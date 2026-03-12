package com.rhsystem.adapters.outbound.persistence.repository;

import com.rhsystem.adapters.outbound.persistence.entity.TimeEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaTimeEventRepository extends JpaRepository<TimeEventEntity, UUID> {
    List<TimeEventEntity> findByEmployeeIdOrderByEventTimeAsc(UUID employeeId);
    
    @Query("SELECT te FROM TimeEventEntity te WHERE te.employeeId = :employeeId " +
           "AND te.eventTime >= :startDate AND te.eventTime < :endDate " +
           "ORDER BY te.eventTime ASC")
    List<TimeEventEntity> findByEmployeeIdAndDateRange(
        @Param("employeeId") UUID employeeId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT te FROM TimeEventEntity te WHERE te.employeeId = :employeeId " +
           "AND EXTRACT(MONTH FROM te.eventTime) = :month " +
           "AND EXTRACT(YEAR FROM te.eventTime) = :year " +
           "ORDER BY te.eventTime ASC")
    List<TimeEventEntity> findByEmployeeIdAndMonth(
        @Param("employeeId") UUID employeeId,
        @Param("month") int month,
        @Param("year") int year
    );
}
