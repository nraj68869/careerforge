package com.careerforge.backend.service;

import com.careerforge.backend.Repository.NotificationRepository;
import com.careerforge.backend.Repository.UserRepository;
import com.careerforge.backend.dto.NotificationResponse;
import com.careerforge.backend.entity.Notification;
import com.careerforge.backend.entity.NotificationType;
import com.careerforge.backend.entity.Role;
import com.careerforge.backend.security.AuthUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class NotificationService {
    private static final int MAX_PAGE_SIZE = 30;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // ---------- creating (called by other services, inside THEIR transaction) ----------

    @Transactional
    public void send(Long userId, NotificationType type, String title, String message, String route) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(cut(title, 120));
        n.setMessage(cut(message, 300));
        n.setRoute(route);
        notificationRepository.save(n);
    }

    @Transactional
    public void sendToAdmins(NotificationType type, String title, String message, String route) {
        for (Long adminId : userRepository.findIdsByRole(Role.ADMIN)) {
            send(adminId, type, title, message, route);
        }
    }

    // ---------- reading: ALWAYS limited to the logged-in user ----------

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(AuthUser actor, boolean unreadOnly, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<Notification> result = unreadOnly
                ? notificationRepository.findByUserIdAndReadAtIsNull(actor.id(), pageable)
                : notificationRepository.findByUserId(actor.id(), pageable);
        return result.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long unreadCount(AuthUser actor) {
        return notificationRepository.countByUserIdAndReadAtIsNull(actor.id());
    }

    @Transactional
    public void markRead(AuthUser actor, Long id) {
        // no error if it is not yours or does not exist: nothing changes, and nothing is revealed
        notificationRepository.markRead(id, actor.id(), Instant.now());
    }

    @Transactional
    public void markAllRead(AuthUser actor) {
        notificationRepository.markAllRead(actor.id(), Instant.now());
    }

    @Transactional
    public void deleteFor(Long userId) {
        notificationRepository.deleteByUserId(userId);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(),
                n.getRoute(), n.getReadAt() != null, n.getCreatedAt());
    }

    private String cut(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}