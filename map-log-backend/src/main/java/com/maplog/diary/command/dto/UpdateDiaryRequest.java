package com.maplog.diary.command.dto;

import com.maplog.diary.command.domain.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record UpdateDiaryRequest(

        @NotBlank(message = "제목은 필수입니다.")
        String title,

        @NotBlank(message = "내용은 필수입니다.")
        String content,

        @NotNull(message = "방문 일시는 필수입니다.")
        LocalDateTime visitedAt,

        @NotNull(message = "공개 범위는 필수입니다.")
        Visibility visibility,

        List<Long> sharedUserIds
) {}