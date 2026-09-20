package com.philippos.employeemanagement.repository;

import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.entity.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = "employee")
    Optional<User> findByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmployee_Id(Long employeeId);

    Optional<User> findByEmployee_Id(Long employeeId);

    boolean existsByRole(Role role);

}
