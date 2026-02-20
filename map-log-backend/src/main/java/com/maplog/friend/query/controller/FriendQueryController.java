package com.maplog.friend.query.controller;

import com.maplog.common.response.ApiResponse;
import com.maplog.diary.query.dto.DiarySummaryResponse;
import com.maplog.friend.query.dto.FriendRequestResponse;
import com.maplog.friend.query.dto.FriendSummaryResponse;
import com.maplog.friend.query.service.FriendQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FriendQueryController {

    private final FriendQueryService friendQueryService;

    @GetMapping("/api/friends")
    public ResponseEntity<ApiResponse<List<FriendSummaryResponse>>> getFriends(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<FriendSummaryResponse> friends = friendQueryService.getFriends(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(friends));
    }

    @GetMapping("/api/friends/pending")
    public ResponseEntity<ApiResponse<Page<FriendRequestResponse>>> getPendingRequests(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<FriendRequestResponse> requests = friendQueryService.getPendingRequests(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    @GetMapping("/api/feed")
    public ResponseEntity<ApiResponse<Page<DiarySummaryResponse>>> getFeed(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<DiarySummaryResponse> feed = friendQueryService.getFeed(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(ApiResponse.success(feed));
    }
}