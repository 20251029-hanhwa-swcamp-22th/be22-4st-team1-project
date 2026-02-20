package com.maplog.diary.command.repository;

import com.maplog.diary.command.domain.DiaryImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiaryImageRepository extends JpaRepository<DiaryImage, Long> {

    List<DiaryImage> findByDiaryId(Long diaryId);

    void deleteByDiaryId(Long diaryId);
}
