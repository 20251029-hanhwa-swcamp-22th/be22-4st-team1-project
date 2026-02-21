package com.maplog.diary.command.service;

import com.maplog.common.exception.BusinessException;
import com.maplog.common.exception.ErrorCode;
import com.maplog.common.storage.FileStorageService;
import com.maplog.diary.command.domain.Diary;
import com.maplog.diary.command.domain.Visibility;
import com.maplog.diary.command.dto.CreateDiaryRequest;
import com.maplog.diary.command.dto.UpdateDiaryRequest;
import com.maplog.diary.command.repository.DiaryCommandRepository;
import com.maplog.diary.command.repository.DiaryImageRepository;
import com.maplog.diary.command.repository.DiaryShareRepository;
import com.maplog.diary.command.repository.ScrapRepository;
import com.maplog.notification.command.service.NotificationCommandService;
import com.maplog.user.command.domain.User;
import com.maplog.user.command.repository.UserCommandRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiaryCommandServiceTest {

    @InjectMocks
    private DiaryCommandService diaryCommandService;

    @Mock
    private DiaryCommandRepository diaryCommandRepository;

    @Mock
    private DiaryImageRepository diaryImageRepository;

    @Mock
    private ScrapRepository scrapRepository;

    @Mock
    private UserCommandRepository userCommandRepository;

    @Mock
    private DiaryShareRepository diaryShareRepository;

    @Mock
    private NotificationCommandService notificationCommandService;

    @Mock
    private FileStorageService fileStorageService;

    @Nested
    @DisplayName("일기 생성 테스트")
    class CreateDiaryTest {
        @Test
        @DisplayName("성공")
        void success() {
            // given
            String email = "test@email.com";
            CreateDiaryRequest request = new CreateDiaryRequest(
                    "title", "content", 37.5, 127.0, "location", "address",
                    LocalDateTime.now(), Visibility.PRIVATE, null
            );
            User user = User.create(email, "pw", "nick");
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userCommandRepository.findByEmailAndDeletedAtIsNull(email)).willReturn(Optional.of(user));
            
            // when
            diaryCommandService.createDiary(email, request, Collections.emptyList());

            // then
            verify(diaryCommandRepository).save(any(Diary.class));
        }
    }

    @Nested
    @DisplayName("일기 수정 테스트")
    class UpdateDiaryTest {
        @Test
        @DisplayName("성공")
        void success() {
            // given
            String email = "test@email.com";
            Long diaryId = 100L;
            UpdateDiaryRequest request = new UpdateDiaryRequest(
                    "new title", "new content", LocalDateTime.now(), Visibility.FRIENDS_ONLY, null
            );
            User user = User.create(email, "pw", "nick");
            ReflectionTestUtils.setField(user, "id", 1L);

            Diary diary = Diary.create(1L, new CreateDiaryRequest("t", "c", 37.5, 127.0, "l", "a", LocalDateTime.now(), Visibility.PRIVATE, null));
            ReflectionTestUtils.setField(diary, "id", diaryId);

            given(userCommandRepository.findByEmailAndDeletedAtIsNull(email)).willReturn(Optional.of(user));
            given(diaryCommandRepository.findByIdAndDeletedAtIsNull(diaryId)).willReturn(Optional.of(diary));
            given(diaryShareRepository.findAllByDiaryId(diaryId)).willReturn(Collections.emptyList());

            // when
            diaryCommandService.updateDiary(email, diaryId, request, null, null);

            // then
            assertThat(diary.getTitle()).isEqualTo("new title");
            assertThat(diary.getVisibility()).isEqualTo(Visibility.FRIENDS_ONLY);
        }

        @Test
        @DisplayName("작성자가 아닌 사용자가 수정 시도 시 예외 발생")
        void failNotOwner() {
            // given
            String email = "other@email.com";
            Long diaryId = 100L;
            User other = User.create(email, "pw", "other");
            ReflectionTestUtils.setField(other, "id", 2L);

            Diary diary = Diary.create(1L, new CreateDiaryRequest("t", "c", 37.5, 127.0, "l", "a", LocalDateTime.now(), Visibility.PRIVATE, null));
            ReflectionTestUtils.setField(diary, "id", diaryId);

            given(userCommandRepository.findByEmailAndDeletedAtIsNull(email)).willReturn(Optional.of(other));
            given(diaryCommandRepository.findByIdAndDeletedAtIsNull(diaryId)).willReturn(Optional.of(diary));

            // when & then
            assertThatThrownBy(() -> diaryCommandService.updateDiary(email, diaryId, new UpdateDiaryRequest("t", "c", LocalDateTime.now(), Visibility.PRIVATE, null), null, null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DIARY_ACCESS_DENIED);
        }
    }

    @Nested
    @DisplayName("스크랩 테스트")
    class ScrapTest {
        @Test
        @DisplayName("성공")
        void successAddScrap() {
            // given
            String email = "test@email.com";
            Long diaryId = 100L;
            User user = User.create(email, "pw", "nick");
            ReflectionTestUtils.setField(user, "id", 1L);

            Diary diary = Diary.create(2L, new CreateDiaryRequest("t", "c", 37.5, 127.0, "l", "a", LocalDateTime.now(), Visibility.PRIVATE, null));
            ReflectionTestUtils.setField(diary, "id", diaryId);

            given(userCommandRepository.findByEmailAndDeletedAtIsNull(email)).willReturn(Optional.of(user));
            given(diaryCommandRepository.findByIdAndDeletedAtIsNull(diaryId)).willReturn(Optional.of(diary));
            given(scrapRepository.existsByUserIdAndDiaryId(1L, 100L)).willReturn(false);

            // when
            diaryCommandService.addScrap(email, diaryId);

            // then
            verify(scrapRepository).save(any());
        }

        @Test
        @DisplayName("이미 스크랩한 경우 예외 발생")
        void failAlreadyScrapped() {
            // given
            String email = "test@email.com";
            Long diaryId = 100L;
            User user = User.create(email, "pw", "nick");
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userCommandRepository.findByEmailAndDeletedAtIsNull(email)).willReturn(Optional.of(user));
            given(diaryCommandRepository.findByIdAndDeletedAtIsNull(diaryId)).willReturn(Optional.of(mock(Diary.class)));
            given(scrapRepository.existsByUserIdAndDiaryId(1L, 100L)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> diaryCommandService.addScrap(email, diaryId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_SCRAPED);
        }
    }
}
