package com.maplog.diary.query.service;

import com.maplog.common.exception.BusinessException;
import com.maplog.common.exception.ErrorCode;
import com.maplog.common.storage.FileStorageService;
import com.maplog.diary.command.domain.Diary;
import com.maplog.diary.command.domain.Visibility;
import com.maplog.diary.command.repository.DiaryCommandRepository;
import com.maplog.diary.command.repository.DiaryShareRepository;
import com.maplog.diary.query.dto.DiaryDetailResponse;
import com.maplog.diary.query.dto.DiaryImageResponse;
import com.maplog.diary.query.dto.DiaryMarkerResponse;
import com.maplog.diary.query.dto.DiarySummaryResponse;
import com.maplog.diary.query.mapper.DiaryQueryMapper;
import com.maplog.user.command.domain.User;
import com.maplog.user.command.repository.UserCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 일기(Diary) 도메인의 조회(Query) 작업을 담당하는 서비스입니다.
 * MyBatis를 활용한 고성능 조회와 이미지 보안 조회를 위한 Presigned URL 처리를 수행합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryQueryService {

    private final DiaryCommandRepository diaryCommandRepository;
    private final UserCommandRepository userCommandRepository;
    private final DiaryShareRepository diaryShareRepository;
    private final DiaryQueryMapper diaryQueryMapper;
    private final FileStorageService fileStorageService;

    /**
     * 일기 상세 내용을 조회합니다. 
     * 작성자 본인 혹은 공유받은 대상자만 접근 가능하며, 이미지들은 보안 서명된 URL로 변환됩니다.
     *
     * @param email 요청자 이메일
     * @param diaryId 일기 ID
     * @return 일기 상세 정보 (작성자, 내용, 위치, 서명된 이미지 URL 등)
     */
    public DiaryDetailResponse getDiaryDetail(String email, Long diaryId) {
        User requestingUser = getUser(email);
        Diary diary = getDiary(diaryId);

        // 비즈니스 권한 체크: 본인 일기 혹은 공유받은 일기인지 검증
        if (!canAccess(requestingUser, diary)) {
            throw new BusinessException(ErrorCode.DIARY_ACCESS_DENIED);
        }

        // MyBatis를 이용한 데이터 로드
        DiaryDetailResponse response = diaryQueryMapper.findDiaryDetail(diaryId, requestingUser.getId());
        if (response == null) {
            throw new BusinessException(ErrorCode.DIARY_NOT_FOUND);
        }

        // S3 Private 이미지 접근을 위한 Presigned URL 생성 (1시간 유효)
        if (response.getImages() != null) {
            response.getImages().forEach(img -> 
                img.setImageUrl(fileStorageService.generatePresignedUrl(img.getImageUrl()))
            );
        }
        
        return response;
    }

    /**
     * 지도 화면에 표시할 마커 목록을 조회합니다.
     * 본인 일기와 공유받은 친구의 일기만 필터링되어 반환됩니다.
     */
    public List<DiaryMarkerResponse> getMapMarkers(String email,
                                                    Double minLat, Double maxLat,
                                                    Double minLng, Double maxLng) {
        User user = getUser(email);
        return diaryQueryMapper.findMapMarkers(minLat, maxLat, minLng, maxLng, user.getId());
    }

    /**
     * 내가 공유받은 친구들의 일기 목록(피드)을 조회합니다.
     */
    public Page<DiarySummaryResponse> getFeedDiaries(String email, Pageable pageable) {
        User user = getUser(email);
        int offset = (int) pageable.getOffset();
        int size = pageable.getPageSize();
        
        List<DiarySummaryResponse> items = diaryQueryMapper.findFeedDiaries(user.getId(), offset, size);
        long total = diaryQueryMapper.countFeedDiaries(user.getId());
        
        return new PageImpl<>(items, pageable, total);
    }

    /**
     * 해당 일기에 대한 접근 권한을 판단하는 비즈니스 규칙입니다.
     * 1. 작성자 본인은 항상 접근 가능
     * 2. PRIVATE(나만보기)인 경우 본인이 아니면 접근 불가
     * 3. FRIENDS_ONLY인 경우 diary_shares 테이블에 요청자 ID가 존재해야 접근 가능
     */
    private boolean canAccess(User requestingUser, Diary diary) {
        if (diary.isOwner(requestingUser.getId())) return true;
        
        if (diary.getVisibility() == Visibility.PRIVATE) return false;
        
        if (diary.getVisibility() == Visibility.FRIENDS_ONLY) {
            return diaryShareRepository.existsByDiaryIdAndUserId(diary.getId(), requestingUser.getId());
        }
        
        return false;
    }

    private User getUser(String email) {
        return userCommandRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Diary getDiary(Long diaryId) {
        return diaryCommandRepository.findByIdAndDeletedAtIsNull(diaryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_NOT_FOUND));
    }
}