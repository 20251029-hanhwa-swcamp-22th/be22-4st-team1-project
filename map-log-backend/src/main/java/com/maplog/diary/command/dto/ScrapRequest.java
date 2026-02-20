package com.maplog.diary.command.dto;

import jakarta.validation.constraints.NotNull;

public record ScrapRequest(

        @NotNull(message = "일기 ID는 필수입니다.")
        Long diaryId
) {}