package com.maplog.diary.command.repository;

import com.maplog.diary.command.domain.Diary;
import com.maplog.diary.command.domain.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DiaryCommandRepository extends JpaRepository<Diary, Long> {

    Optional<Diary> findByIdAndDeletedAtIsNull(Long id);

    Page<Diary> findByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);

    @Query("SELECT d FROM Diary d WHERE d.latitude BETWEEN :minLat AND :maxLat " +
           "AND d.longitude BETWEEN :minLng AND :maxLng " +
           "AND d.deletedAt IS NULL " +
           "AND (d.visibility = :publicVisibility OR d.userId = :userId)")
    List<Diary> findMapMarkers(@Param("minLat") Double minLat,
                               @Param("maxLat") Double maxLat,
                               @Param("minLng") Double minLng,
                               @Param("maxLng") Double maxLng,
                               @Param("userId") Long userId,
                               @Param("publicVisibility") Visibility publicVisibility);
}