package com.philippos.employeemanagement.dto.response;

import java.time.Instant;

public class WorkShiftResponse {
    private final Long id;
    private final Long employeeId;
    private final Instant startTime;
    private final Instant endTime;

    public WorkShiftResponse(Long id, Long employeeId, Instant startTime, Instant endTime) {
        this.id = id;
        this.employeeId = employeeId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
}
