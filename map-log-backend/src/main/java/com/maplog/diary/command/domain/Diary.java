package com.maplog.diary.command.domain;

import com.maplog.diary.command.dto.CreateDiaryRequest;
import com.maplog.diary.command.dto.UpdateDiaryRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "diaries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Diary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private String locationName;

    private String address;

    @Column(nullable = false)
    private LocalDateTime visitedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    public static Diary create(Long userId, CreateDiaryRequest request) {
        Diary diary = new Diary();
        diary.userId = userId;
        diary.title = request.title();
        diary.content = request.content();
        diary.latitude = request.latitude();
        diary.longitude = request.longitude();
        diary.locationName = request.locationName();
        diary.address = request.address();
        diary.visitedAt = request.visitedAt();
        diary.visibility = request.visibility();
        return diary;
    }

    public void update(UpdateDiaryRequest request) {
        this.title = request.title();
        this.content = request.content();
        this.visitedAt = request.visitedAt();
        this.visibility = request.visibility();
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isOwner(Long userId) {
        return this.userId.equals(userId);
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}