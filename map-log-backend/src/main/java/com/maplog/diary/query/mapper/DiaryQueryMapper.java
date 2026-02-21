package com.maplog.diary.query.mapper;

import com.maplog.diary.query.dto.DiaryDetailResponse;
import com.maplog.diary.query.dto.DiaryMarkerResponse;
import com.maplog.diary.query.dto.DiarySummaryResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiaryQueryMapper {

    DiaryDetailResponse findDiaryDetail(@Param("diaryId") Long diaryId, @Param("userId") Long userId);

    List<DiaryMarkerResponse> findMapMarkers(@Param("minLat") Double minLat,
                                             @Param("maxLat") Double maxLat,
                                             @Param("minLng") Double minLng,
                                             @Param("maxLng") Double maxLng,
                                             @Param("userId") Long userId);

    List<DiarySummaryResponse> findMyDiaries(@Param("userId") Long userId,
                                             @Param("offset") int offset,
                                             @Param("size") int size);

    long countMyDiaries(@Param("userId") Long userId);

    List<DiarySummaryResponse> findMyScraps(@Param("userId") Long userId,
                                            @Param("offset") int offset,
                                            @Param("size") int size);

    long countMyScraps(@Param("userId") Long userId);

    List<DiarySummaryResponse> findFeedDiaries(@Param("userId") Long userId,
                                              @Param("offset") int offset,
                                              @Param("size") int size);

    long countFeedDiaries(@Param("userId") Long userId);
}