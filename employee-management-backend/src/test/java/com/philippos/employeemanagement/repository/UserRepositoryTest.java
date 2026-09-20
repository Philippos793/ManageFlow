package com.philippos.employeemanagement.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserRepositoryTest {

    @Test
    void authenticationLookupFetchesLinkedEmployee() throws NoSuchMethodException {
        EntityGraph entityGraph = UserRepository.class
                .getMethod("findByUsername", String.class)
                .getAnnotation(EntityGraph.class);

        assertNotNull(entityGraph);
        assertArrayEquals(new String[]{"employee"}, entityGraph.attributePaths());
    }
}
