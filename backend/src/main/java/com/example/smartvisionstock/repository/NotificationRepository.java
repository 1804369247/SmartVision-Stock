package com.example.smartvisionstock.repository;

import com.example.smartvisionstock.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);
    Page<Notification> findByUserIdAndIsReadOrderByCreateTimeDesc(Long userId, Boolean isRead, Pageable pageable);
    long countByUserIdAndIsRead(Long userId, Boolean isRead);
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId")
    int markAllAsRead(Long userId);
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id")
    int markAsRead(Long id);
}