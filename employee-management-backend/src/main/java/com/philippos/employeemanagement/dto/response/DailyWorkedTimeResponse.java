package com.philippos.employeemanagement.dto.response;

public class DailyWorkedTimeResponse {
    private final int day;
    private final long workedMinutes;
    private final String workedTime;
    public DailyWorkedTimeResponse(int day, long workedMinutes, String workedTime) {
        this.day = day;
        this.workedMinutes = workedMinutes;
        this.workedTime = workedTime;
    }
    public int getDay() { return day; }
    public long getWorkedMinutes() { return workedMinutes; }
    public String getWorkedTime() { return workedTime; }
}