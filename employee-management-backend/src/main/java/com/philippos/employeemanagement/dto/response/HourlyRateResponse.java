package com.philippos.employeemanagement.dto.response;

import java.math.BigDecimal;

public class HourlyRateResponse {
    private final Long employeeId;
    private final BigDecimal hourlyRate;
    public HourlyRateResponse(Long employeeId, BigDecimal hourlyRate) {
        this.employeeId = employeeId;
        this.hourlyRate = hourlyRate;
    }
    public Long getEmployeeId() { return employeeId; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
}