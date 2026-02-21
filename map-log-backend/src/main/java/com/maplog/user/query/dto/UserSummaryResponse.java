package com.maplog.user.query.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserSummaryResponse {
    private Long id;
    private String nickname;
    private String profileImageUrl;
}