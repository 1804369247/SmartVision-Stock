package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.dto.response.ApiResponse;
import com.example.smartvisionstock.entity.Notification;
import com.example.smartvisionstock.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> getNotificationList(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean isRead) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notificationPage;

        if (isRead != null) {
            notificationPage = notificationRepository.findByUserIdAndIsReadOrderByCreateTimeDesc(userId, isRead, pageable);
        } else {
            notificationPage = notificationRepository.findByUserIdOrderByCreateTimeDesc(userId, pageable);
        }

        long unreadCount = notificationRepository.countByUserIdAndIsRead(userId, false);

        Map<String, Object> result = new HashMap<>();
        result.put("content", notificationPage.getContent());
        result.put("totalElements", notificationPage.getTotalElements());
        result.put("totalPages", notificationPage.getTotalPages());
        result.put("currentPage", notificationPage.getNumber());
        result.put("pageSize", notificationPage.getSize());
        result.put("unreadCount", unreadCount);
        return ApiResponse.success(result);
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Object>> getUnreadCount(@RequestParam Long userId) {
        long unreadCount = notificationRepository.countByUserIdAndIsRead(userId, false);
        Map<String, Object> result = new HashMap<>();
        result.put("unreadCount", unreadCount);
        return ApiResponse.success(result);
    }

    @PostMapping("/{id}/read")
    @Transactional
    public ApiResponse<Object> markAsRead(@PathVariable Long id) {
        notificationRepository.markAsRead(id);
        return ApiResponse.success("已标记为已读");
    }

    @PostMapping("/read-all")
    @Transactional
    public ApiResponse<Object> markAllAsRead(@RequestParam Long userId) {
        notificationRepository.markAllAsRead(userId);
        return ApiResponse.success("全部已标记为已读");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Object> deleteNotification(@PathVariable Long id) {
        notificationRepository.deleteById(id);
        return ApiResponse.success("删除成功");
    }

    @PostMapping("/send")
    public ApiResponse<Object> sendNotification(@RequestBody Map<String, Object> request) {
        try {
            Notification notification = new Notification();
            notification.setUserId(request.get("userId") != null ? ((Number) request.get("userId")).longValue() : null);
            notification.setUsername((String) request.get("username"));
            notification.setType((String) request.get("type"));
            notification.setTitle((String) request.get("title"));
            notification.setContent((String) request.get("content"));
            notification.setRelatedId(request.get("relatedId") != null ? ((Number) request.get("relatedId")).longValue() : null);
            notification.setRelatedType((String) request.get("relatedType"));
            notification.setPriority(request.get("priority") != null ? ((Number) request.get("priority")).intValue() : 1);

            notificationRepository.save(notification);
            return ApiResponse.success("通知发送成功");
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }
}