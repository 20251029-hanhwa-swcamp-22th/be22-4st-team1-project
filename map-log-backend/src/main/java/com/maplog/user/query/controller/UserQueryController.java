package com.maplog.user.query.controller;

import com.maplog.common.response.ApiResponse;
import com.maplog.user.query.dto.UserProfileQueryResponse;
import com.maplog.user.query.dto.UserSummaryResponse;
import com.maplog.user.query.service.UserQueryService;
import lombok.RequiredArgsConstructor;
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
}