package com.philippos.employeemanagement.dto.response;

public class AdminDashboardSummaryResponse {
    private final long totalEmployees;
    private final long totalDepartments;
    private final long activeShifts;
    private final long hoursThisMonthMinutes;
    private final String hoursThisMonth;

    public AdminDashboardSummaryResponse(long totalEmployees, long totalDepartments,
            long activeShifts, long hoursThisMonthMinutes, String hoursThisMonth) {
        this.totalEmployees = totalEmployees;
        this.totalDepartments = totalDepartments;
        this.activeShifts = activeShifts;
        this.hoursThisMonthMinutes = hoursThisMonthMinutes;
        this.hoursThisMonth = hoursThisMonth;
    }

    public long getTotalEmployees() { return totalEmployees; }
    public long getTotalDepartments() { return totalDepartments; }
    public long getActiveShifts() { return activeShifts; }
    public long getHoursThisMonthMinutes() { return hoursThisMonthMinutes; }
    public String getHoursThisMonth() { return hoursThisMonth; }
}