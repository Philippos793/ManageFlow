package com.philippos.employeemanagement.entity;

import com.philippos.employeemanagement.repository.TaskRepository;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskMappingTest {

    @Test
    void taskStatusContainsExpectedLifecycleValues() {
        assertEquals(
                List.of("PENDING", "IN_PROGRESS", "DECLINED", "COMPLETED"),
                Arrays.stream(TaskStatus.values()).map(Enum::name).toList());
    }

    @Test
    void taskUsesRequiredLazyManyToOneRelationships() throws NoSuchFieldException {
        assertRequiredLazyRelationship("assignedEmployee", "assigned_employee_id");
        assertRequiredLazyRelationship("createdByUser", "created_by_user_id");
    }

    @Test
    void employeeAndUserExposeOneToManyTaskRelationships() throws NoSuchFieldException {
        OneToMany employeeTasks = Employee.class.getDeclaredField("assignedTasks")
                .getAnnotation(OneToMany.class);
        OneToMany userTasks = User.class.getDeclaredField("createdTasks")
                .getAnnotation(OneToMany.class);

        assertEquals("assignedEmployee", employeeTasks.mappedBy());
        assertEquals("createdByUser", userTasks.mappedBy());
    }

    @Test
    void taskExposesOneToManyChecklistRelationship() throws NoSuchFieldException {
        OneToMany checklistItems = Task.class.getDeclaredField("checklistItems")
                .getAnnotation(OneToMany.class);

        assertEquals("task", checklistItems.mappedBy());
    }

    @Test
    void taskAndUserExposeTaskAttachmentRelationships() throws NoSuchFieldException {
        OneToMany taskAttachments = Task.class.getDeclaredField("attachments")
                .getAnnotation(OneToMany.class);
        OneToMany uploadedAttachments = User.class.getDeclaredField("uploadedTaskAttachments")
                .getAnnotation(OneToMany.class);

        assertEquals("task", taskAttachments.mappedBy());
        assertEquals("uploader", uploadedAttachments.mappedBy());
    }

    @Test
    void repositoryUsesSpringDataJpa() {
        assertTrue(JpaRepository.class.isAssignableFrom(TaskRepository.class));
    }

    private void assertRequiredLazyRelationship(String fieldName, String columnName)
            throws NoSuchFieldException {
        Field field = Task.class.getDeclaredField(fieldName);
        ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);

        assertEquals(jakarta.persistence.FetchType.LAZY, manyToOne.fetch());
        assertFalse(manyToOne.optional());
        assertEquals(columnName, joinColumn.name());
        assertFalse(joinColumn.nullable());
    }
}
