package com.philippos.employeemanagement.dto.response;

import com.philippos.employeemanagement.entity.Role;

public class EmployeeAccountResponse {
    private Long userId;
    private Long employeeId;
    private String username;
    private Role role;

    public EmployeeAccountResponse() {}
    public EmployeeAccountResponse(Long userId, Long employeeId, String username, Role role) {
        this.userId = userId;
        this.employeeId = employeeId;
        this.username = username;
        this.role = role;
    }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
