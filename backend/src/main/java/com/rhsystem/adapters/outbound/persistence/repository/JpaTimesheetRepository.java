package com.rhsystem.adapters.outbound.persistence.repository;

import com.rhsystem.adapters.outbound.persistence.entity.TimesheetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaTimesheetRepository extends JpaRepository<TimesheetEntity, UUID> {
    Optional<TimesheetEntity> findByEmployeeIdAndMonthAndYear(UUID employeeId, int month, int year);
    List<TimesheetEntity> findByEmployeeId(UUID employeeId);
    
    @Query("SELECT t FROM TimesheetEntity t JOIN EmployeeEntity e ON t.employeeId = e.id " +
           "WHERE e.companyId = :companyId AND t.month = :month AND t.year = :year")
    List<TimesheetEntity> findByCompanyIdAndMonthAndYear(
        @Param("companyId") UUID companyId,
        @Param("month") int month,
        @Param("year") int year
    );
}
