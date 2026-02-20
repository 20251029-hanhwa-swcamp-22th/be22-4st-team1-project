package com.maplog.user.command.controller;

import com.maplog.common.response.ApiResponse;
import com.maplog.user.command.dto.UpdateProfileRequest;
import com.maplog.user.command.service.UserCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserCommandController {

    private final UserCommandService userCommandService;

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid UpdateProfileRequest request) {
        userCommandService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("프로필이 수정되었습니다.", null));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails) {
        userCommandService.deleteAccount(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 완료되었습니다.", null));
    }
}
