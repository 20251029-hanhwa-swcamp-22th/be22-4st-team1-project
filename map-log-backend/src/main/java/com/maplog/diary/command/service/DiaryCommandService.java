package com.maplog.diary.command.service;

import com.maplog.common.exception.BusinessException;
import com.maplog.common.exception.ErrorCode;
import com.maplog.common.storage.FileStorageService;
import com.maplog.diary.command.domain.Diary;
import com.maplog.diary.command.domain.DiaryImage;
import com.maplog.diary.command.domain.Scrap;
import com.maplog.diary.command.dto.CreateDiaryRequest;
import com.maplog.diary.command.dto.UpdateDiaryRequest;
import com.maplog.diary.command.repository.DiaryCommandRepository;
import com.maplog.diary.command.repository.DiaryImageRepository;
import com.maplog.diary.command.repository.ScrapRepository;
import com.maplog.user.command.domain.User;
import com.maplog.user.command.repository.UserCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DiaryCommandService {

    private final DiaryCommandRepository diaryCommandRepository;
    private final DiaryImageRepository diaryImageRepository;
    private final ScrapRepository scrapRepository;
    private final UserCommandRepository userCommandRepository;
    private final FileStorageService fileStorageService;

    public Long createDiary(String email, CreateDiaryRequest request, List<MultipartFile> images) {
        User user = getUser(email);
        Diary diary = Diary.create(user.getId(), request);
        diaryCommandRepository.save(diary);
        if (images != null) {
            images.stream()
                    .filter(f -> f != null && !f.isEmpty())
                    .forEach(f -> {
                        String url = fileStorageService.store(f);
                        diaryImageRepository.save(DiaryImage.create(diary.getId(), url));
                    });
        }
        return diary.getId();
    }

    public void updateDiary(String email, Long diaryId, UpdateDiaryRequest request,
                            List<Long> deleteImageIds, List<MultipartFile> images) {
        User user = getUser(email);
        Diary diary = getDiary(diaryId);

        if (!diary.isOwner(user.getId())) {
            throw new BusinessException(ErrorCode.DIARY_ACCESS_DENIED);
        }
        diary.update(request);

        if (deleteImageIds != null) {
            deleteImageIds.forEach(imgId -> {
                diaryImageRepository.findById(imgId).ifPresent(img -> {
                    fileStorageService.delete(img.getImageUrl());
                    diaryImageRepository.delete(img);
                });
            });
        }
        if (images != null) {
            images.stream()
                    .filter(f -> f != null && !f.isEmpty())
                    .forEach(f -> {
                        String url = fileStorageService.store(f);
                        diaryImageRepository.save(DiaryImage.create(diaryId, url));
                    });
        }
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