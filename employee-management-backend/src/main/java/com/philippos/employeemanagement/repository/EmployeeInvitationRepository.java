package com.philippos.employeemanagement.repository;

import com.philippos.employeemanagement.entity.EmployeeInvitation;
import com.philippos.employeemanagement.entity.EmployeeInvitationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeeInvitationRepository
        extends JpaRepository<EmployeeInvitation, Long> {

    Optional<EmployeeInvitation> findByTokenHash(String tokenHash);

    Optional<EmployeeInvitation> findByEmployeeIdAndStatus(
            Long employeeId, EmployeeInvitationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT invitation FROM EmployeeInvitation invitation "
            + "JOIN FETCH invitation.employee "
            + "WHERE invitation.tokenHash = :tokenHash")
    Optional<EmployeeInvitation> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash);
}
