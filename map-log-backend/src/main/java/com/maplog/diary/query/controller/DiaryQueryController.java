package com.maplog.diary.query.controller;

import com.maplog.common.response.ApiResponse;
import com.maplog.diary.query.dto.DiaryDetailResponse;
import com.maplog.diary.query.dto.DiaryMarkerResponse;
import com.maplog.diary.query.dto.DiarySummaryResponse;
import com.maplog.diary.query.service.DiaryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryQueryController {

    private final DiaryQueryService diaryQueryService;

    @GetMapping("/{diaryId}")
    public ResponseEntity<ApiResponse<DiaryDetailResponse>> getDiaryDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long diaryId) {
        DiaryDetailResponse response = diaryQueryService.getDiaryDetail(userDetails.getUsername(), diaryId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/map")
    public ResponseEntity<ApiResponse<List<DiaryMarkerResponse>>> getMapMarkers(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Double swLat,
            @RequestParam Double swLng,
            @RequestParam Double neLat,
            @RequestParam Double neLng) {
        List<DiaryMarkerResponse> markers = diaryQueryService.getMapMarkers(
                userDetails.getUsername(), swLat, neLat, swLng, neLng);
        return ResponseEntity.ok(ApiResponse.success(markers));
    }

    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<Page<DiarySummaryResponse>>> getFeedDiaries(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<DiarySummaryResponse> response = diaryQueryService.getFeedDiaries(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}