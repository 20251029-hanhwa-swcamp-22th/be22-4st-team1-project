package com.maplog.notification.command.controller;

import com.maplog.common.response.ApiResponse;
import com.maplog.notification.command.service.NotificationCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationCommandController {

    private final NotificationCommandService notificationCommandService;

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long notificationId) {
        notificationCommandService.markAsRead(userDetails.getUsername(), notificationId);
        return ResponseEntity.ok(ApiResponse.success("알림을 읽음 처리했습니다.", null));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {
        notificationCommandService.markAllAsRead(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("모든 알림을 읽음 처리했습니다.", null));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteAll(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String isRead) {
        Boolean readFilter = null;
        if ("Y".equalsIgnoreCase(isRead)) readFilter = true;
        else if ("N".equalsIgnoreCase(isRead)) readFilter = false;
        notificationCommandService.deleteAll(userDetails.getUsername(), readFilter);
        return ResponseEntity.ok(ApiResponse.success("알림을 삭제했습니다.", null));
    }
}