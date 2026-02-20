package com.maplog.diary.command.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "diary_shares", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"diary_id", "user_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiaryShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "diary_id", nullable = false)
    private Long diaryId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static DiaryShare create(Long diaryId, Long userId) {
        DiaryShare share = new DiaryShare();
        share.diaryId = diaryId;
        share.userId = userId;
        return share;
    }
}
