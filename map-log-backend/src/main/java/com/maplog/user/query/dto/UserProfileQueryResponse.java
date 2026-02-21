package com.maplog.user.query.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class UserProfileQueryResponse {
    private Long id;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String role;
    private LocalDateTime createdAt;
}