package com.maplog.diary.query.service;

import com.maplog.common.exception.BusinessException;
import com.maplog.common.exception.ErrorCode;
import com.maplog.diary.command.domain.Diary;
import com.maplog.diary.command.domain.Visibility;
import com.maplog.diary.command.repository.DiaryCommandRepository;
import com.maplog.diary.command.repository.DiaryShareRepository;
import com.maplog.diary.query.dto.DiaryDetailResponse;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryQueryService {

    private final DiaryCommandRepository diaryCommandRepository;
    private final UserCommandRepository userCommandRepository;
    private final DiaryShareRepository diaryShareRepository;
    private final DiaryQueryMapper diaryQueryMapper;

    public DiaryDetailResponse getDiaryDetail(String email, Long diaryId) {
        User requestingUser = getUser(email);
        Diary diary = getDiary(diaryId);

        if (!canAccess(requestingUser, diary)) {
            throw new BusinessException(ErrorCode.DIARY_ACCESS_DENIED);
        }

        DiaryDetailResponse response = diaryQueryMapper.findDiaryDetail(diaryId, requestingUser.getId());
        if (response == null) {
            throw new BusinessException(ErrorCode.DIARY_NOT_FOUND);
        }
        return response;
    }

    public List<DiaryMarkerResponse> getMapMarkers(String email,
                                                    Double minLat, Double maxLat,
                                                    Double minLng, Double maxLng) {
        User user = getUser(email);
        return diaryQueryMapper.findMapMarkers(minLat, maxLat, minLng, maxLng, user.getId());
    }

    public Page<DiarySummaryResponse> getMyDiaries(String email, Pageable pageable) {
        User user = getUser(email);
        int offset = (int) pageable.getOffset();
        int size = pageable.getPageSize();
        List<DiarySummaryResponse> items = diaryQueryMapper.findMyDiaries(user.getId(), offset, size);
        long total = diaryQueryMapper.countMyDiaries(user.getId());
        return new PageImpl<>(items, pageable, total);
    }

    public Page<DiarySummaryResponse> getMyScraps(String email, Pageable pageable) {
        User user = getUser(email);
        int offset = (int) pageable.getOffset();
        int size = pageable.getPageSize();
        List<DiarySummaryResponse> items = diaryQueryMapper.findMyScraps(user.getId(), offset, size);
        long total = diaryQueryMapper.countMyScraps(user.getId());
        return new PageImpl<>(items, pageable, total);
    }

    public Page<DiarySummaryResponse> getFeedDiaries(String email, Pageable pageable) {
        User user = getUser(email);
        int offset = (int) pageable.getOffset();
        int size = pageable.getPageSize();
        List<DiarySummaryResponse> items = diaryQueryMapper.findFeedDiaries(user.getId(), offset, size);
        long total = diaryQueryMapper.countFeedDiaries(user.getId());
        return new PageImpl<>(items, pageable, total);
    }

    private boolean canAccess(User requestingUser, Diary diary) {
        // 본인 일기면 무조건 접근 가능
        if (diary.isOwner(requestingUser.getId())) return true;
        
        // 비공개면 접근 불가
        if (diary.getVisibility() == Visibility.PRIVATE) return false;
        
        // 친구 공개인 경우 diary_shares 테이블에 정보가 있어야 함
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