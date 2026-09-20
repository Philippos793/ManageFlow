package com.philippos.employeemanagement.repository;

import com.philippos.employeemanagement.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findFirstByNameIgnoreCase(String name);

    @Modifying
    @Query(value = """
            INSERT INTO departments (name)
            VALUES (:name)
            ON CONFLICT ((LOWER(name))) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("name") String name);
}