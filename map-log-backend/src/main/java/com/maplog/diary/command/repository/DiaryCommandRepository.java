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
import java.util.Set;

public interface DiaryCommandRepository extends JpaRepository<Diary, Long> {

    Optional<Diary> findByIdAndDeletedAtIsNull(Long id);

    Page<Diary> findByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);

    @Query("SELECT d FROM Diary d WHERE d.deletedAt IS NULL AND d.userId IN :friendIds " +
           "AND d.visibility IN ('PUBLIC', 'FRIENDS_ONLY') ORDER BY d.createdAt DESC")
    Page<Diary> findFriendFeed(@Param("friendIds") Set<Long> friendIds, Pageable pageable);

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