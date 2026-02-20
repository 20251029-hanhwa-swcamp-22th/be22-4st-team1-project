package com.maplog.diary.command.controller;

import com.maplog.common.response.ApiResponse;
import com.maplog.diary.command.domain.Visibility;
import com.maplog.diary.command.dto.CreateDiaryRequest;
import com.maplog.diary.command.dto.UpdateDiaryRequest;
import com.maplog.diary.command.service.DiaryCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryCommandController {

    private final DiaryCommandService diaryCommandService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> createDiary(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam String locationName,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime visitedAt,
            @RequestParam(required = false, defaultValue = "PUBLIC") Visibility visibility,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        CreateDiaryRequest request = new CreateDiaryRequest(
                title, content, latitude, longitude, locationName, address,
                visitedAt != null ? visitedAt : LocalDateTime.now(),
                visibility
        );
        Long diaryId = diaryCommandService.createDiary(userDetails.getUsername(), request, images);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("일기가 작성되었습니다.", diaryId));
    }

    @PutMapping(value = "/{diaryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> updateDiary(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long diaryId,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime visitedAt,
            @RequestParam(required = false, defaultValue = "PUBLIC") Visibility visibility,
            @RequestParam(required = false) List<Long> deleteImageIds,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        UpdateDiaryRequest request = new UpdateDiaryRequest(
                title, content,
                visitedAt != null ? visitedAt : LocalDateTime.now(),
                visibility
        );
        diaryCommandService.updateDiary(userDetails.getUsername(), diaryId, request, deleteImageIds, images);
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
