package com.maplog.diary.query.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DiaryImageResponse {
    private Long imageId;
    private String imageUrl;
}