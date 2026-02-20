package com.maplog.diary.command.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DiaryShareRequest(

        @NotEmpty(message = "공유할 친구를 선택해주세요.")
        List<Long> friendIds
) {}
