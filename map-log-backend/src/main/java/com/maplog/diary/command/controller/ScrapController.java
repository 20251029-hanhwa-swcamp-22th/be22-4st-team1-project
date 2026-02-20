package com.maplog.diary.command.controller;

import com.maplog.common.response.ApiResponse;
import com.maplog.diary.command.dto.ScrapRequest;
import com.maplog.diary.command.service.DiaryCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scraps")
@RequiredArgsConstructor
public class ScrapController {

    private final DiaryCommandService diaryCommandService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addScrap(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid ScrapRequest request) {
        diaryCommandService.addScrap(userDetails.getUsername(), request.diaryId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("스크랩이 추가되었습니다.", null));
    }

    @DeleteMapping("/{diaryId}")
    public ResponseEntity<ApiResponse<Void>> cancelScrap(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long diaryId) {
        diaryCommandService.cancelScrap(userDetails.getUsername(), diaryId);
        return ResponseEntity.ok(ApiResponse.success("스크랩이 취소되었습니다.", null));
    }
}