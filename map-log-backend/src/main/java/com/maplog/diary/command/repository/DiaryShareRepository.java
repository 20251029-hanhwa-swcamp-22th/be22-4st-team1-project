package com.maplog.diary.command.repository;

import com.maplog.diary.command.domain.DiaryShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiaryShareRepository extends JpaRepository<DiaryShare, Long> {

    boolean existsByDiaryIdAndUserId(Long diaryId, Long userId);

    void deleteByDiaryIdAndUserId(Long diaryId, Long userId);

    List<DiaryShare> findAllByDiaryId(Long diaryId);

    void deleteAllByDiaryId(Long diaryId);
}
