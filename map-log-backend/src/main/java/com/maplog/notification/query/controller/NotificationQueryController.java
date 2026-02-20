package com.maplog.notification.query.controller;

import com.maplog.common.response.ApiResponse;
import com.maplog.notification.query.dto.NotificationResponse;
import com.maplog.notification.query.service.NotificationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationQueryController {

    private final NotificationQueryService notificationQueryService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String isRead,
            @PageableDefault(size = 20) Pageable pageable) {
        Boolean readFilter = null;
        if ("Y".equalsIgnoreCase(isRead)) readFilter = true;
        else if ("N".equalsIgnoreCase(isRead)) readFilter = false;
        Page<NotificationResponse> notifications = notificationQueryService.getNotifications(
                userDetails.getUsername(), readFilter, pageable);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }
}