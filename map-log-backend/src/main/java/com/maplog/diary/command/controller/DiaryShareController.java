package com.maplog.diary.command.controller;

import com.maplog.common.response.ApiResponse;
import com.maplog.diary.command.dto.DiaryShareRequest;
import com.maplog.diary.command.service.DiaryShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryShareController {

    private final DiaryShareService diaryShareService;

    @PostMapping("/{diaryId}/share")
    public ResponseEntity<ApiResponse<Void>> shareDiary(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long diaryId,
            @RequestBody @Valid DiaryShareRequest request) {
        diaryShareService.shareDiary(userDetails.getUsername(), diaryId, request.friendIds());
        return ResponseEntity.ok(ApiResponse.success("일기를 친구에게 공유했습니다.", null));
    }

    @DeleteMapping("/{diaryId}/share/{userId}")
    public ResponseEntity<ApiResponse<Void>> unshareDiary(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long diaryId,
            @PathVariable Long userId) {
        diaryShareService.unshareDiary(userDetails.getUsername(), diaryId, userId);
        return ResponseEntity.ok(ApiResponse.success("공유를 취소했습니다.", null));
    }
}
