package com.philippos.employeemanagement.dto.response;

import java.time.Instant;

public class CurrentWorkShiftResponse {
    private final boolean active;
    private final Instant startTime;

    public CurrentWorkShiftResponse(boolean active, Instant startTime) {
        this.active = active;
        this.startTime = startTime;
    }

    public boolean isActive() { return active; }
    public Instant getStartTime() { return startTime; }
}
