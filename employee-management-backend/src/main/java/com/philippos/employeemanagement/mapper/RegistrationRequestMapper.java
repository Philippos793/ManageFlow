package com.philippos.employeemanagement.mapper;

import com.philippos.employeemanagement.dto.response.RegistrationRequestResponse;
import com.philippos.employeemanagement.entity.RegistrationRequest;
import org.springframework.stereotype.Component;

@Component
public class RegistrationRequestMapper {
    public RegistrationRequestResponse toResponse(RegistrationRequest request) {
        return new RegistrationRequestResponse(
                request.getId(), request.getFirstName(), request.getLastName(), request.getEmail(),
                request.getUsername(), request.getStatus(), request.getCreatedAt()
        );
    }
}
