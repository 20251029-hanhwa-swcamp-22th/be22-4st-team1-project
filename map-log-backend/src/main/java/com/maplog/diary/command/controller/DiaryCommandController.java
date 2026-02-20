package com.maplog.diary.command.controller;

import com.maplog.common.response.ApiResponse;
import com.maplog.diary.command.dto.CreateDiaryRequest;
import com.maplog.diary.command.dto.UpdateDiaryRequest;
import com.maplog.diary.command.service.DiaryCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryCommandController {

    private final DiaryCommandService diaryCommandService;

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createDiary(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid CreateDiaryRequest request) {
        Long diaryId = diaryCommandService.createDiary(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("일기가 작성되었습니다.", diaryId));
    }

    @PutMapping("/{diaryId}")
    public ResponseEntity<ApiResponse<Void>> updateDiary(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long diaryId,
            @RequestBody @Valid UpdateDiaryRequest request) {
        diaryCommandService.updateDiary(userDetails.getUsername(), diaryId, request);
        return ResponseEntity.ok(ApiResponse.success("일기가 수정되었습니다.", null));
    }

    @DeleteMapping("/{diaryId}")
    public ResponseEntity<ApiResponse<Void>> deleteDiary(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long diaryId) {
        diaryCommandService.deleteDiary(userDetails.getUsername(), diaryId);
        return ResponseEntity.ok(ApiResponse.success("일기가 삭제되었습니다.", null));
    }
}