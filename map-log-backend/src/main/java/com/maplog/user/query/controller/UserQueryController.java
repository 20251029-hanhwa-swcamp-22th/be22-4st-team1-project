package com.maplog.user.query.controller;

import com.maplog.common.response.ApiResponse;
import com.maplog.diary.query.dto.DiarySummaryResponse;
import com.maplog.diary.query.service.DiaryQueryService;
import com.maplog.user.query.dto.UserProfileQueryResponse;
import com.maplog.user.query.dto.UserSummaryResponse;
import com.maplog.user.query.service.UserQueryService;
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

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserQueryController {

    private final UserQueryService userQueryService;
    private final DiaryQueryService diaryQueryService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileQueryResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        UserProfileQueryResponse response = userQueryService.getMyProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserSummaryResponse>>> searchUsers(
            @RequestParam String keyword) {
        List<UserSummaryResponse> responses = userQueryService.searchUsers(keyword);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/me/diaries")
    public ResponseEntity<ApiResponse<Page<DiarySummaryResponse>>> getMyDiaries(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<DiarySummaryResponse> diaries = diaryQueryService.getMyDiaries(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(ApiResponse.success(diaries));
    }

    @GetMapping("/me/scraps")
    public ResponseEntity<ApiResponse<Page<DiarySummaryResponse>>> getMyScraps(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<DiarySummaryResponse> scraps = diaryQueryService.getMyScraps(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(ApiResponse.success(scraps));
    }
}