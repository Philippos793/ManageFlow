package com.philippos.employeemanagement.repository;

import com.philippos.employeemanagement.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByRecipientUser_IdOrderByCreatedAtDesc(Long recipientUserId);
    Optional<Notification> findByIdAndRecipientUser_Id(Long id, Long recipientUserId);
}
