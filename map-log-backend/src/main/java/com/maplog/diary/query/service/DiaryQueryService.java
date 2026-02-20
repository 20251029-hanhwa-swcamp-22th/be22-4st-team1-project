package com.maplog.diary.query.service;

import com.maplog.common.exception.BusinessException;
import com.maplog.common.exception.ErrorCode;
import com.maplog.diary.command.domain.Diary;
import com.maplog.diary.command.domain.Visibility;
import com.maplog.diary.command.repository.DiaryCommandRepository;
import com.maplog.diary.command.repository.ScrapRepository;
import com.maplog.diary.query.dto.DiaryDetailResponse;
import com.maplog.diary.query.dto.DiaryMarkerResponse;
import com.maplog.diary.query.dto.DiarySummaryResponse;
import com.maplog.user.command.domain.User;
import com.maplog.user.command.repository.UserCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryQueryService {

    private final DiaryCommandRepository diaryCommandRepository;
    private final ScrapRepository scrapRepository;
    private final UserCommandRepository userCommandRepository;

    public DiaryDetailResponse getDiaryDetail(String email, Long diaryId) {
        User requestingUser = getUser(email);
        Diary diary = getDiary(diaryId);

        if (!canAccess(requestingUser, diary)) {
            throw new BusinessException(ErrorCode.DIARY_ACCESS_DENIED);
        }

        User author = userCommandRepository.findById(diary.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        boolean scraped = scrapRepository.existsByUserIdAndDiaryId(requestingUser.getId(), diaryId);

        return new DiaryDetailResponse(
                diary.getId(), diary.getUserId(), author.getNickname(),
                diary.getTitle(), diary.getContent(),
                diary.getLatitude(), diary.getLongitude(),
                diary.getLocationName(), diary.getAddress(),
                diary.getVisitedAt(), diary.getVisibility().name(),
                diary.getCreatedAt(), scraped
        );
    }

    public List<DiaryMarkerResponse> getMapMarkers(String email,
                                                    Double minLat, Double maxLat,
                                                    Double minLng, Double maxLng) {
        User user = getUser(email);
        return diaryCommandRepository
                .findMapMarkers(minLat, maxLat, minLng, maxLng, user.getId(), Visibility.PUBLIC)
                .stream()
                .map(d -> new DiaryMarkerResponse(d.getId(), d.getLatitude(), d.getLongitude(), d.getTitle()))
                .toList();
    }

    public Page<DiarySummaryResponse> getMyDiaries(String email, Pageable pageable) {
        User user = getUser(email);
        return diaryCommandRepository.findByUserIdAndDeletedAtIsNull(user.getId(), pageable)
                .map(d -> new DiarySummaryResponse(
                        d.getId(), d.getTitle(), d.getLocationName(),
                        d.getVisitedAt(), d.getVisibility().name(), d.getCreatedAt()
                ));
    }

    public Page<DiarySummaryResponse> getMyScraps(String email, Pageable pageable) {
        User user = getUser(email);
        return scrapRepository.findByUserId(user.getId(), pageable)
                .map(s -> {
                    Diary diary = diaryCommandRepository.findByIdAndDeletedAtIsNull(s.getDiaryId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_NOT_FOUND));
                    return new DiarySummaryResponse(
                            diary.getId(), diary.getTitle(), diary.getLocationName(),
                            diary.getVisitedAt(), diary.getVisibility().name(), diary.getCreatedAt()
                    );
                });
    }

    private boolean canAccess(User requestingUser, Diary diary) {
        if (diary.isOwner(requestingUser.getId())) return true;
        // FRIENDS_ONLY: C(friend) 완성 후 친구 여부 체크 추가
        return diary.getVisibility() == Visibility.PUBLIC;
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