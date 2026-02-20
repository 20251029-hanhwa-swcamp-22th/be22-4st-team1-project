package com.maplog.friend.command.controller;

import com.maplog.common.response.ApiResponse;
import com.maplog.friend.command.dto.FriendRespondRequest;
import com.maplog.friend.command.dto.SendFriendRequest;
import com.maplog.friend.command.service.FriendCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendCommandController {

    private final FriendCommandService friendCommandService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> sendFriendRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid SendFriendRequest request) {
        friendCommandService.sendFriendRequest(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("친구 요청을 보냈습니다.", null));
    }

    @PatchMapping("/{friendId}")
    public ResponseEntity<ApiResponse<Void>> respondToRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long friendId,
            @RequestBody @Valid FriendRespondRequest request) {
        friendCommandService.respondToRequest(userDetails.getUsername(), friendId, request);
        return ResponseEntity.ok(ApiResponse.success("친구 요청에 응답했습니다.", null));
    }
}