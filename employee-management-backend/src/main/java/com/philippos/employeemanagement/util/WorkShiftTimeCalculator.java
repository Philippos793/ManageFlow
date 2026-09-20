package com.philippos.employeemanagement.util;

import com.philippos.employeemanagement.entity.WorkShift;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;

public final class WorkShiftTimeCalculator {

    private WorkShiftTimeCalculator() {
    }

    public static long calculateMinutesWithin(
            WorkShift shift,
            Instant periodStart,
            Instant periodEnd) {
        if (!isCompletedAndOverlapping(shift, periodStart, periodEnd)) {
            return 0;
        }

        Instant effectiveStart = shift.getStartTime().isAfter(periodStart)
                ? shift.getStartTime()
                : periodStart;
        Instant effectiveEnd = shift.getEndTime().isBefore(periodEnd)
                ? shift.getEndTime()
                : periodEnd;

        if (!effectiveStart.isBefore(effectiveEnd)) {
            return 0;
        }
        return Duration.between(effectiveStart, effectiveEnd).toMinutes();
    }

    public static boolean isCompletedAndOverlapping(
            WorkShift shift,
            Instant periodStart,
            Instant periodEnd) {
        return shift.getEndTime() != null
                && shift.getStartTime().isBefore(periodEnd)
                && shift.getEndTime().isAfter(periodStart);
    }
    public static long calculateTotalMinutesWithin(
            Collection<WorkShift> shifts,
            Instant periodStart,
            Instant periodEnd) {
        return shifts.stream()
                .mapToLong(shift -> calculateMinutesWithin(
                        shift, periodStart, periodEnd))
                .sum();
    }
}
