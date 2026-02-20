package com.maplog.user.command.controller;

import com.maplog.common.response.ApiResponse;
import com.maplog.common.storage.FileStorageService;
import com.maplog.user.command.service.UserCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserCommandController {

    private final UserCommandService userCommandService;
    private final FileStorageService fileStorageService;

    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String nickname,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {
        String profileImageUrl = null;
        if (profileImage != null && !profileImage.isEmpty()) {
            profileImageUrl = fileStorageService.store(profileImage);
        }
        userCommandService.updateProfile(userDetails.getUsername(), nickname, profileImageUrl);
        return ResponseEntity.ok(ApiResponse.success("프로필이 수정되었습니다.", null));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails) {
        userCommandService.deleteAccount(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 완료되었습니다.", null));
    }
}
