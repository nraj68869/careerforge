package com.careerforge.backend.controller;

import com.careerforge.backend.dto.NotificationResponse;
import com.careerforge.backend.security.AuthUser;
import com.careerforge.backend.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Page<NotificationResponse> list(@AuthenticationPrincipal AuthUser actor,
                                           @RequestParam(defaultValue = "false") boolean unreadOnly,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return notificationService.list(actor, unreadOnly, page, size);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal AuthUser actor) {
        return Map.of("count", notificationService.unreadCount(actor));
    }

    @PutMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id) {
        notificationService.markRead(actor, id);
    }

    @PutMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal AuthUser actor) {
        notificationService.markAllRead(actor);
    }
}