package com.philippos.employeemanagement.mapper;

import com.philippos.employeemanagement.dto.response.WorkShiftResponse;
import com.philippos.employeemanagement.entity.WorkShift;
import org.springframework.stereotype.Component;

@Component
public class WorkShiftMapper {
    public WorkShiftResponse toResponse(WorkShift workShift) {
        return new WorkShiftResponse(
                workShift.getId(), workShift.getEmployee().getId(),
                workShift.getStartTime(), workShift.getEndTime());
    }
}