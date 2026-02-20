package com.maplog.diary.command.repository;

import com.maplog.diary.command.domain.Scrap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScrapRepository extends JpaRepository<Scrap, Long> {

    boolean existsByUserIdAndDiaryId(Long userId, Long diaryId);

    Page<Scrap> findByUserId(Long userId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM Scrap s WHERE s.userId = :userId AND s.diaryId = :diaryId")
    void deleteByUserIdAndDiaryId(@Param("userId") Long userId, @Param("diaryId") Long diaryId);
}