package com.philippos.employeemanagement.repository;

import com.philippos.employeemanagement.entity.RegistrationRequest;
import com.philippos.employeemanagement.entity.RegistrationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RegistrationRequestRepository extends JpaRepository<RegistrationRequest, Long> {
    boolean existsByUsernameIgnoreCaseAndStatus(String username, RegistrationStatus status);
    boolean existsByEmailIgnoreCaseAndStatus(String email, RegistrationStatus status);
    List<RegistrationRequest> findByStatusOrderByCreatedAtAsc(RegistrationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT request FROM RegistrationRequest request WHERE request.id = :id")
    Optional<RegistrationRequest> findByIdForUpdate(@Param("id") Long id);
}
