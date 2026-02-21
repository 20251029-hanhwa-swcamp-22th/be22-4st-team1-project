package com.maplog.diary.command.service;

import com.maplog.common.exception.BusinessException;
import com.maplog.common.exception.ErrorCode;
import com.maplog.common.storage.FileStorageService;
import com.maplog.diary.command.domain.Diary;
import com.maplog.diary.command.domain.DiaryImage;
import com.maplog.diary.command.domain.DiaryShare;
import com.maplog.diary.command.domain.Scrap;
import com.maplog.diary.command.dto.CreateDiaryRequest;
import com.maplog.diary.command.dto.UpdateDiaryRequest;
import com.maplog.diary.command.repository.DiaryCommandRepository;
import com.maplog.diary.command.repository.DiaryImageRepository;
import com.maplog.diary.command.repository.DiaryShareRepository;
import com.maplog.diary.command.repository.ScrapRepository;
import com.maplog.notification.command.service.NotificationCommandService;
import com.maplog.user.command.domain.User;
import com.maplog.user.command.repository.UserCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 일기(Diary) 도메인의 쓰기(Command) 작업을 담당하는 서비스입니다.
 * 일기 생성, 수정, 삭제 및 S3 이미지 업로드, 친구 공유 로직을 관리합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DiaryCommandService {

    private final DiaryCommandRepository diaryCommandRepository;
    private final DiaryImageRepository diaryImageRepository;
    private final ScrapRepository scrapRepository;
    private final UserCommandRepository userCommandRepository;
    private final DiaryShareRepository diaryShareRepository;
    private final NotificationCommandService notificationCommandService;
    private final FileStorageService fileStorageService;

    /**
     * 새로운 일기를 생성하고 이미지를 S3에 업로드하며, 선택된 친구들에게 공유합니다.
     *
     * @param email 작성자의 이메일
     * @param request 일기 제목, 내용, 위치 정보, 공개 범위 및 공유 대상 ID 목록
     * @param images 첨부 이미지 파일 목록 (MultipartFile)
     * @return 생성된 일기의 고유 ID
     */
    public Long createDiary(String email, CreateDiaryRequest request, List<MultipartFile> images) {
        User user = getUser(email);
        
        // 1. 일기 기본 정보 저장 (JPA)
        Diary diary = Diary.create(user.getId(), request);
        diaryCommandRepository.save(diary);
        
        // 2. 이미지 파일 처리 (S3 업로드 및 DB URL 저장)
        if (images != null) {
            images.stream()
                    .filter(f -> f != null && !f.isEmpty())
                    .forEach(f -> {
                        String url = fileStorageService.store(f);
                        diaryImageRepository.save(DiaryImage.create(diary.getId(), url));
                    });
        }

        // 3. 친구 공유 처리 및 알림 발송
        if (request.sharedUserIds() != null && !request.sharedUserIds().isEmpty()) {
            request.sharedUserIds().forEach(targetUserId -> {
                diaryShareRepository.save(DiaryShare.create(diary.getId(), targetUserId));
                notificationCommandService.createDiarySharedNotification(
                        targetUserId, diary.getId(), diary.getTitle(), user.getNickname());
            });
        }

        return diary.getId();
    }

    /**
     * 기존 일기를 수정합니다. 내용 변경, 이미지 추가/삭제 및 공유 대상 변경을 처리합니다.
     *
     * @param email 수정 요청자의 이메일
     * @param diaryId 수정할 일기 ID
     * @param request 수정된 정보
     * @param deleteImageIds 삭제할 기존 이미지 ID 목록
     * @param images 추가할 새 이미지 파일 목록
     */
    public void updateDiary(String email, Long diaryId, UpdateDiaryRequest request,
                            List<Long> deleteImageIds, List<MultipartFile> images) {
        User user = getUser(email);
        Diary diary = getDiary(diaryId);

        // 작성자 본인 확인
        if (!diary.isOwner(user.getId())) {
            throw new BusinessException(ErrorCode.DIARY_ACCESS_DENIED);
        }
        diary.update(request);

        // 1. 기존 이미지 삭제 처리 (S3 및 DB)
        if (deleteImageIds != null) {
            deleteImageIds.forEach(imgId -> {
                diaryImageRepository.findById(imgId).ifPresent(img -> {
                    fileStorageService.delete(img.getImageUrl());
                    diaryImageRepository.delete(img);
                });
            });
        }
        
        // 2. 새 이미지 추가 처리
        if (images != null) {
            images.stream()
                    .filter(f -> f != null && !f.isEmpty())
                    .forEach(f -> {
                        String url = fileStorageService.store(f);
                        diaryImageRepository.save(DiaryImage.create(diaryId, url));
                    });
        }

        // 3. 공유 목록 동기화 및 새 공유 대상자 알림 발송
        syncSharingList(diaryId, request.sharedUserIds(), diary.getTitle(), user.getNickname());
    }

    /**
     * 공유 친구 목록을 현재 요청된 목록과 비교하여 추가/삭제를 수행합니다.
     */
    private void syncSharingList(Long diaryId, List<Long> newIds, String title, String nickname) {
        Set<Long> currentSharedUserIds = diaryShareRepository.findAllByDiaryId(diaryId).stream()
                .map(DiaryShare::getUserId)
                .collect(Collectors.toSet());
        
        Set<Long> newSharedUserIds = newIds != null ? new HashSet<>(newIds) : new HashSet<>();

        // 제외된 유저: 권한 삭제
        currentSharedUserIds.stream()
                .filter(id -> !newSharedUserIds.contains(id))
                .forEach(id -> diaryShareRepository.deleteByDiaryIdAndUserId(diaryId, id));

        // 새로 추가된 유저: 권한 추가 및 알림 발송
        newSharedUserIds.stream()
                .filter(id -> !currentSharedUserIds.contains(id))
                .forEach(id -> {
                    diaryShareRepository.save(DiaryShare.create(diaryId, id));
                    notificationCommandService.createDiarySharedNotification(id, diaryId, title, nickname);
                });
    }

    /**
     * 일기를 논리적으로 삭제(Soft Delete)하고 공유 정보도 정리합니다.
     */
    public void deleteDiary(String email, Long diaryId) {
        User user = getUser(email);
        Diary diary = getDiary(diaryId);

        if (!diary.isOwner(user.getId())) {
            throw new BusinessException(ErrorCode.DIARY_ACCESS_DENIED);
        }
        diary.softDelete();
        diaryShareRepository.deleteAllByDiaryId(diaryId);
    }

    /**
     * 일기를 내 스크랩 목록에 추가합니다.
     */
    public void addScrap(String email, Long diaryId) {
        User user = getUser(email);
        Diary diary = getDiary(diaryId);

        if (scrapRepository.existsByUserIdAndDiaryId(user.getId(), diaryId)) {
            throw new BusinessException(ErrorCode.ALREADY_SCRAPED);
        }
        scrapRepository.save(Scrap.create(user.getId(), diary.getId()));
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