package com.maplog.diary.command.service;

import com.maplog.common.exception.BusinessException;
import com.maplog.common.exception.ErrorCode;
import com.maplog.diary.command.domain.Diary;
import com.maplog.diary.command.domain.DiaryShare;
import com.maplog.diary.command.repository.DiaryCommandRepository;
import com.maplog.diary.command.repository.DiaryShareRepository;
import com.maplog.user.command.domain.User;
import com.maplog.user.command.repository.UserCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DiaryShareService {

    private final DiaryCommandRepository diaryCommandRepository;
    private final DiaryShareRepository diaryShareRepository;
    private final UserCommandRepository userCommandRepository;

    public void shareDiary(String email, Long diaryId, List<Long> friendIds) {
        User owner = getUser(email);
        Diary diary = getDiary(diaryId);

        if (!diary.isOwner(owner.getId())) {
            throw new BusinessException(ErrorCode.DIARY_ACCESS_DENIED);
        }
        friendIds.forEach(friendId -> {
            if (!diaryShareRepository.existsByDiaryIdAndUserId(diaryId, friendId)) {
                diaryShareRepository.save(DiaryShare.create(diaryId, friendId));
            }
        });
    }

    public void unshareDiary(String email, Long diaryId, Long targetUserId) {
        User owner = getUser(email);
        Diary diary = getDiary(diaryId);

        if (!diary.isOwner(owner.getId())) {
            throw new BusinessException(ErrorCode.DIARY_ACCESS_DENIED);
        }
        if (!diaryShareRepository.existsByDiaryIdAndUserId(diaryId, targetUserId)) {
            throw new BusinessException(ErrorCode.DIARY_SHARE_NOT_FOUND);
        }
        diaryShareRepository.deleteByDiaryIdAndUserId(diaryId, targetUserId);
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
