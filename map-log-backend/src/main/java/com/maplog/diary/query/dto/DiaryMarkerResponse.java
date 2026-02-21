package com.maplog.diary.query.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DiaryMarkerResponse {
    private Long id;
    private Double latitude;
    private Double longitude;
    private String title;
}