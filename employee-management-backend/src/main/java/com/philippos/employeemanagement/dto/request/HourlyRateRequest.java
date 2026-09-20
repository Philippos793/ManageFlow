package com.philippos.employeemanagement.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class HourlyRateRequest {
    @NotNull(message = "Hourly rate is required")
    @DecimalMin(value = "0.00", message = "Hourly rate cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Hourly rate must have at most two decimal places")
    private BigDecimal hourlyRate;

    public HourlyRateRequest() {}
    public HourlyRateRequest(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }
}