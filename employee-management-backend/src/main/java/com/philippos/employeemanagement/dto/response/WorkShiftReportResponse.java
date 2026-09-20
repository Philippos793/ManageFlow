package com.philippos.employeemanagement.dto.response;

public class WorkShiftReportResponse {
    private final Long employeeId;
    private final String fullName;
    private final long totalWorkedMinutes;
    private final String totalWorkedHours;
    private final long completedShifts;

    public WorkShiftReportResponse(Long employeeId, String fullName,
                                   long totalWorkedMinutes, String totalWorkedHours,
                                   long completedShifts) {
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.totalWorkedMinutes = totalWorkedMinutes;
        this.totalWorkedHours = totalWorkedHours;
        this.completedShifts = completedShifts;
    }

    public Long getEmployeeId() { return employeeId; }
    public String getFullName() { return fullName; }
    public long getTotalWorkedMinutes() { return totalWorkedMinutes; }
    public String getTotalWorkedHours() { return totalWorkedHours; }
    public long getCompletedShifts() { return completedShifts; }
}