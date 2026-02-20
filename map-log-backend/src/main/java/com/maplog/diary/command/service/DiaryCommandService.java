package com.maplog.diary.command.service;

import com.maplog.common.exception.BusinessException;
import com.maplog.common.exception.ErrorCode;
import com.maplog.diary.command.domain.Diary;
import com.maplog.diary.command.domain.Scrap;
import com.maplog.diary.command.dto.CreateDiaryRequest;
import com.maplog.diary.command.dto.UpdateDiaryRequest;
import com.maplog.diary.command.repository.DiaryCommandRepository;
import com.maplog.diary.command.repository.ScrapRepository;
import com.maplog.user.command.domain.User;
import com.maplog.user.command.repository.UserCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DiaryCommandService {

    private final DiaryCommandRepository diaryCommandRepository;
    private final ScrapRepository scrapRepository;
    private final UserCommandRepository userCommandRepository;

    public Long createDiary(String email, CreateDiaryRequest request) {
        User user = getUser(email);
        Diary diary = Diary.create(user.getId(), request);
        return diaryCommandRepository.save(diary).getId();
    }

    public void updateDiary(String email, Long diaryId, UpdateDiaryRequest request) {
        User user = getUser(email);
        Diary diary = getDiary(diaryId);

        if (!diary.isOwner(user.getId())) {
            throw new BusinessException(ErrorCode.DIARY_ACCESS_DENIED);
        }
        diary.update(request);
    }

    public void deleteDiary(String email, Long diaryId) {
        User user = getUser(email);
        Diary diary = getDiary(diaryId);

        if (!diary.isOwner(user.getId())) {
            throw new BusinessException(ErrorCode.DIARY_ACCESS_DENIED);
        }
        diary.softDelete();
    }

    public void addScrap(String email, Long diaryId) {
        User user = getUser(email);
        Diary diary = getDiary(diaryId);

        if (scrapRepository.existsByUserIdAndDiaryId(user.getId(), diaryId)) {
            throw new BusinessException(ErrorCode.ALREADY_SCRAPED);
        }
        scrapRepository.save(Scrap.create(user.getId(), diary.getId()));
    }

    public void cancelScrap(String email, Long diaryId) {
        User user = getUser(email);

        if (!scrapRepository.existsByUserIdAndDiaryId(user.getId(), diaryId)) {
            throw new BusinessException(ErrorCode.SCRAP_NOT_FOUND);
        }
        scrapRepository.deleteByUserIdAndDiaryId(user.getId(), diaryId);
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