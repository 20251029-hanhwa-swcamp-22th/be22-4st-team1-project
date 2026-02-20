package com.maplog.diary.command.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "scraps",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "diary_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Scrap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "user_id")
    private Long userId;

    @Column(nullable = false, name = "diary_id")
    private Long diaryId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public static Scrap create(Long userId, Long diaryId) {
        Scrap scrap = new Scrap();
        scrap.userId = userId;
        scrap.diaryId = diaryId;
        return scrap;
    }
}