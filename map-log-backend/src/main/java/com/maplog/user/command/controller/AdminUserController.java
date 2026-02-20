package com.maplog.user.command.controller;

import com.maplog.common.response.ApiResponse;
import com.maplog.user.command.domain.UserStatus;
import com.maplog.user.command.dto.UserStatusUpdateRequest;
import com.maplog.user.command.service.AdminUserService;
import com.maplog.user.query.dto.AdminUserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> getUsers(
            @RequestParam(required = false) UserStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AdminUserResponse> users = adminUserService.getUsers(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<Void>> changeUserStatus(
            @PathVariable Long userId,
            @RequestBody @Valid UserStatusUpdateRequest request) {
        adminUserService.changeUserStatus(userId, request);
        return ResponseEntity.ok(ApiResponse.success("회원 상태가 변경되었습니다.", null));
    }
}