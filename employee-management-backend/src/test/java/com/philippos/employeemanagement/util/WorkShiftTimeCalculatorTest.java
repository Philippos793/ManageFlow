package com.philippos.employeemanagement.util;

import com.philippos.employeemanagement.entity.WorkShift;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkShiftTimeCalculatorTest {
    private static final ZoneId ATHENS = ZoneId.of("Europe/Athens");

    @Test
    void springDstDayUsesActualElapsedTime() {
        Instant dayStart = LocalDate.of(2026, 3, 29).atStartOfDay(ATHENS).toInstant();
        Instant dayEnd = LocalDate.of(2026, 3, 30).atStartOfDay(ATHENS).toInstant();
        WorkShift shift = new WorkShift(null, dayStart, dayEnd);

        assertEquals(23 * 60,
                WorkShiftTimeCalculator.calculateMinutesWithin(shift, dayStart, dayEnd));
    }

    @Test
    void autumnDstDayUsesActualElapsedTime() {
        Instant dayStart = LocalDate.of(2026, 10, 25).atStartOfDay(ATHENS).toInstant();
        Instant dayEnd = LocalDate.of(2026, 10, 26).atStartOfDay(ATHENS).toInstant();
        WorkShift shift = new WorkShift(null, dayStart, dayEnd);

        assertEquals(25 * 60,
                WorkShiftTimeCalculator.calculateMinutesWithin(shift, dayStart, dayEnd));
    }
}
