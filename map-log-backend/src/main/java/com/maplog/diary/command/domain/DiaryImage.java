package com.maplog.diary.command.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "diary_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiaryImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long diaryId;

    @Column(nullable = false)
    private String imageUrl;

    public static DiaryImage create(Long diaryId, String imageUrl) {
        DiaryImage img = new DiaryImage();
        img.diaryId = diaryId;
        img.imageUrl = imageUrl;
        return img;
    }
}
