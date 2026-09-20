package com.philippos.employeemanagement.dto.response;

import java.math.BigDecimal;
import java.util.List;

public class EmployeeMonthlyWorkDetailsResponse {
    private final Long employeeId;
    private final String fullName;
    private final int year;
    private final int month;
    private final List<DailyWorkedTimeResponse> dailyWorkedTimes;
    private final long totalWorkedMinutes;
    private final String totalWorkedHours;
    private final BigDecimal hourlyRate;

    public EmployeeMonthlyWorkDetailsResponse(Long employeeId, String fullName, int year, int month,
            List<DailyWorkedTimeResponse> dailyWorkedTimes, long totalWorkedMinutes,
            String totalWorkedHours, BigDecimal hourlyRate) {
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.year = year;
        this.month = month;
        this.dailyWorkedTimes = dailyWorkedTimes;
        this.totalWorkedMinutes = totalWorkedMinutes;
        this.totalWorkedHours = totalWorkedHours;
        this.hourlyRate = hourlyRate;
    }
    public Long getEmployeeId() { return employeeId; }
    public String getFullName() { return fullName; }
    public int getYear() { return year; }
    public int getMonth() { return month; }
    public List<DailyWorkedTimeResponse> getDailyWorkedTimes() { return dailyWorkedTimes; }
    public long getTotalWorkedMinutes() { return totalWorkedMinutes; }
    public String getTotalWorkedHours() { return totalWorkedHours; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
}