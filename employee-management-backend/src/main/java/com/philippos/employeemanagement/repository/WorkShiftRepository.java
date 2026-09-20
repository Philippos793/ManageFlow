package com.philippos.employeemanagement.repository;

import com.philippos.employeemanagement.entity.WorkShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WorkShiftRepository extends JpaRepository<WorkShift, Long> {
    Optional<WorkShift> findFirstByEmployeeIdAndEndTimeIsNull(Long employeeId);

    long countByEndTimeIsNull();

    @Query("""
            SELECT shift
            FROM WorkShift shift
            WHERE shift.startTime < :periodEnd
              AND shift.endTime IS NOT NULL
              AND shift.endTime > :periodStart
            """)
    List<WorkShift> findCompletedShiftsOverlapping(
            @Param("periodStart") Instant periodStart,
            @Param("periodEnd") Instant periodEnd);
}
