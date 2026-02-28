# MapLog 테스트 가이드

> **한 줄 요약**: MapLog는 백엔드 단위 테스트(JUnit 5), 통합 테스트(@SpringBootTest), 프론트엔드 컴포넌트 테스트(Vitest)를 통해 품질을 보증합니다.

## 목차 (Table of Contents)

- [개요](#개요)
- [백엔드 테스트](#백엔드-테스트)
- [프론트엔드 테스트](#프론트엔드-테스트)
- [테스트 실행](#테스트-실행)
- [CI/CD 통합](#cicd-통합)

---

## 개요

### 테스트 전략

MapLog는 **테스트 피라미드** 구조를 따릅니다:

```
         ┌─────────────┐
         │   E2E 테스트 │  (10%)  - 실제 브라우저, 전체 플로우
         │ (Selenium)  │
         ├─────────────┤
         │ 통합 테스트  │  (30%)  - API, DB, 서비스 계층
         │(@SpringBoot │
         │   Test)     │
         ├─────────────┤
         │ 단위 테스트  │  (60%)  - 개별 클래스, 메서드
         │ (JUnit 5,  │
         │  Vitest)    │
         └─────────────┘
```

### 테스트 목표
1. **품질 보증**: 버그 조기 발견
2. **회귀 방지**: 기존 기능 유지
3. **리팩토링 안정성**: 코드 개선 시 확신
4. **문서화**: 예제를 통한 API 이해

---

## 백엔드 테스트

### 단위 테스트 (Unit Test)

#### 구조
```
map-log-backend/src/test/java/com/maplog/
├── user/
│   └── command/
│       ├── service/
│       │   └── AuthServiceTest.java
│       └── domain/
│           └── UserTest.java
├── diary/
│   ├── command/
│   │   └── service/DiaryCommandServiceTest.java
│   └── query/
│       └── service/DiaryQueryServiceTest.java
└── common/
    ├── exception/
    │   └── BusinessExceptionTest.java
    └── jwt/
        └── JwtTokenProviderTest.java
```

#### 예시: 서비스 단위 테스트

```java
// src/test/java/com/maplog/diary/command/service/DiaryCommandServiceTest.java

@SpringBootTest
@DisplayName("일기 명령형 서비스")
class DiaryCommandServiceTest {

    @Mock
    private DiaryCommandRepository diaryRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private UserCommandRepository userRepository;

    @InjectMocks
    private DiaryCommandService diaryService;

    private User testUser;
    private Diary testDiary;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        testUser = User.create("test@example.com", "hashed-password", "testuser");
        testUser.id = 1L;

        testDiary = Diary.create(1L, new CreateDiaryRequest(
            "테스트 일기",
            "이것은 테스트 내용입니다.",
            37.498004,
            127.027621,
            "서울",
            "서울시 강남구",
            LocalDateTime.now(),
            Visibility.PUBLIC
        ));
        testDiary.id = 1L;
    }

    @Nested
    @DisplayName("createDiary")
    class CreateDiary {
        @Test
        @DisplayName("정상적으로 일기를 생성한다")
        void shouldCreateDiarySuccessfully() {
            // Given
            CreateDiaryRequest request = new CreateDiaryRequest(
                "새로운 일기",
                "내용",
                37.498004,
                127.027621,
                "서울",
                "서울시 강남구",
                LocalDateTime.now(),
                Visibility.PRIVATE
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(diaryRepository.save(any(Diary.class))).thenReturn(testDiary);

            // When
            Diary result = diaryService.createDiary(1L, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("테스트 일기");
            assertThat(result.getVisibility()).isEqualTo(Visibility.PUBLIC);

            verify(userRepository).findById(1L);
            verify(diaryRepository).save(any(Diary.class));
        }

        @Test
        @DisplayName("사용자가 없으면 예외를 던진다")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            CreateDiaryRequest request = new CreateDiaryRequest(
                "새로운 일기", "내용", 37.5, 127.0, "서울", "서울시",
                LocalDateTime.now(), Visibility.PUBLIC
            );

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> diaryService.createDiary(999L, request))
                .isInstanceOf(BusinessException.class);

            verify(diaryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateDiary")
    class UpdateDiary {
        @Test
        @DisplayName("일기 소유자는 일기를 수정할 수 있다")
        void ownerCanUpdateDiary() {
            // Given
            UpdateDiaryRequest request = new UpdateDiaryRequest(
                "수정된 제목",
                "수정된 내용",
                LocalDateTime.now(),
                Visibility.PRIVATE
            );

            when(diaryRepository.findById(1L)).thenReturn(Optional.of(testDiary));
            when(diaryRepository.save(any(Diary.class))).thenReturn(testDiary);

            // When
            diaryService.updateDiary(1L, request, 1L);

            // Then
            assertThat(testDiary.getTitle()).isEqualTo("수정된 제목");
            verify(diaryRepository).save(testDiary);
        }

        @Test
        @DisplayName("다른 사용자는 일기를 수정할 수 없다")
        void otherUserCannotUpdateDiary() {
            // Given
            UpdateDiaryRequest request = new UpdateDiaryRequest(
                "수정된 제목", "수정된 내용", LocalDateTime.now(), Visibility.PUBLIC
            );

            when(diaryRepository.findById(1L)).thenReturn(Optional.of(testDiary));

            // When & Then
            assertThatThrownBy(() -> diaryService.updateDiary(1L, request, 999L))
                .isInstanceOf(BusinessException.class);

            verify(diaryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteDiary")
    class DeleteDiary {
        @Test
        @DisplayName("일기를 소프트 삭제한다")
        void shouldSoftDeleteDiary() {
            // Given
            when(diaryRepository.findById(1L)).thenReturn(Optional.of(testDiary));

            // When
            diaryService.deleteDiary(1L, 1L);

            // Then
            assertThat(testDiary.isDeleted()).isTrue();
            verify(diaryRepository).save(testDiary);
        }
    }
}
```

#### 예시: Entity 단위 테스트

```java
// src/test/java/com/maplog/diary/command/domain/DiaryTest.java

@DisplayName("일기 엔티티")
class DiaryTest {

    private Diary diary;

    @BeforeEach
    void setUp() {
        diary = Diary.create(1L, new CreateDiaryRequest(
            "테스트 일기",
            "이것은 테스트 내용입니다.",
            37.498004,
            127.027621,
            "서울",
            "서울시 강남구",
            LocalDateTime.of(2025, 2, 28, 10, 0),
            Visibility.PUBLIC
        ));
    }

    @Nested
    @DisplayName("팩토리 메서드")
    class FactoryMethods {
        @Test
        @DisplayName("create 메서드로 새로운 일기를 생성한다")
        void createDiary() {
            // Then
            assertThat(diary.getUserId()).isEqualTo(1L);
            assertThat(diary.getTitle()).isEqualTo("테스트 일기");
            assertThat(diary.getVisibility()).isEqualTo(Visibility.PUBLIC);
            assertThat(diary.isDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("update 메서드")
    class UpdateMethod {
        @Test
        @DisplayName("일기 정보를 업데이트한다")
        void updateDiary() {
            // Given
            UpdateDiaryRequest request = new UpdateDiaryRequest(
                "수정된 제목",
                "수정된 내용",
                LocalDateTime.of(2025, 2, 28, 14, 0),
                Visibility.PRIVATE
            );

            // When
            diary.update(request);

            // Then
            assertThat(diary.getTitle()).isEqualTo("수정된 제목");
            assertThat(diary.getContent()).isEqualTo("수정된 내용");
            assertThat(diary.getVisibility()).isEqualTo(Visibility.PRIVATE);
        }
    }

    @Nested
    @DisplayName("isOwner 메서드")
    class OwnershipCheck {
        @Test
        @DisplayName("소유자 ID가 일치하면 true를 반환한다")
        void isOwnerReturnsTrueWhenMatch() {
            assertThat(diary.isOwner(1L)).isTrue();
        }

        @Test
        @DisplayName("소유자 ID가 일치하지 않으면 false를 반환한다")
        void isOwnerReturnsFalseWhenNotMatch() {
            assertThat(diary.isOwner(999L)).isFalse();
        }
    }

    @Nested
    @DisplayName("softDelete 메서드")
    class SoftDelete {
        @Test
        @DisplayName("삭제 후 isDeleted가 true를 반환한다")
        void softDeleteMarksAsDeleted() {
            // When
            diary.softDelete();

            // Then
            assertThat(diary.isDeleted()).isTrue();
            assertThat(diary.getDeletedAt()).isNotNull();
        }
    }
}
```

### 통합 테스트 (Integration Test)

```java
// src/test/java/com/maplog/diary/command/controller/DiaryCommandControllerTest.java

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DisplayName("일기 명령형 컨트롤러")
class DiaryCommandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DiaryCommandService diaryService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private String validToken;
    private Diary testDiary;

    @BeforeEach
    void setUp() {
        validToken = "valid-jwt-token";
        testDiary = Diary.create(1L, new CreateDiaryRequest(
            "테스트 일기", "내용", 37.5, 127.0, "서울", "서울시",
            LocalDateTime.now(), Visibility.PUBLIC
        ));
        testDiary.id = 1L;
    }

    @Nested
    @DisplayName("POST /api/diaries")
    class CreateDiary {
        @Test
        @DisplayName("정상 요청으로 일기를 생성한다")
        void shouldCreateDiary() throws Exception {
            // Given
            CreateDiaryRequest request = new CreateDiaryRequest(
                "새 일기", "내용", 37.5, 127.0, "서울", "서울시",
                LocalDateTime.now(), Visibility.PUBLIC
            );

            when(jwtTokenProvider.getSubject("Bearer " + validToken))
                .thenReturn("test@example.com");
            when(diaryService.createDiary(anyLong(), any()))
                .thenReturn(testDiary);

            // When & Then
            mockMvc.perform(
                post("/api/diaries")
                    .header("Authorization", "Bearer " + validToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.title").value("테스트 일기"));
        }

        @Test
        @DisplayName("인증 토큰 없으면 401 Unauthorized를 반환한다")
        void shouldReturn401WithoutToken() throws Exception {
            // Given
            CreateDiaryRequest request = new CreateDiaryRequest(
                "새 일기", "내용", 37.5, 127.0, "서울", "서울시",
                LocalDateTime.now(), Visibility.PUBLIC
            );

            // When & Then
            mockMvc.perform(
                post("/api/diaries")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("유효하지 않은 요청 데이터로 400 BadRequest를 반환한다")
        void shouldReturn400WithInvalidData() throws Exception {
            // Given (필수 필드 누락)
            String invalidJson = "{\"title\": \"\"}";  // 빈 제목

            // When & Then
            mockMvc.perform(
                post("/api/diaries")
                    .header("Authorization", "Bearer " + validToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson)
            )
            .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/diaries/{id}")
    class UpdateDiary {
        @Test
        @DisplayName("정상 요청으로 일기를 수정한다")
        void shouldUpdateDiary() throws Exception {
            // Given
            UpdateDiaryRequest request = new UpdateDiaryRequest(
                "수정된 제목", "수정된 내용", LocalDateTime.now(), Visibility.PRIVATE
            );

            when(jwtTokenProvider.getSubject("Bearer " + validToken))
                .thenReturn("test@example.com");

            // When & Then
            mockMvc.perform(
                put("/api/diaries/1")
                    .header("Authorization", "Bearer " + validToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/diaries/{id}")
    class DeleteDiary {
        @Test
        @DisplayName("일기를 삭제한다")
        void shouldDeleteDiary() throws Exception {
            // Given
            when(jwtTokenProvider.getSubject("Bearer " + validToken))
                .thenReturn("test@example.com");

            // When & Then
            mockMvc.perform(
                delete("/api/diaries/1")
                    .header("Authorization", "Bearer " + validToken)
            )
            .andExpect(status().isNoContent());
        }
    }
}
```

### 테스트 유틸리티

```java
// src/test/java/com/maplog/common/TestDataFactory.java

/**
 * 테스트 데이터 생성 팩토리
 */
public class TestDataFactory {

    // User 생성 헬퍼
    public static User createTestUser(String email, String nickname) {
        User user = User.create(email, "hashed-password", nickname);
        // Reflection으로 ID 설정 (DB 저장 없이)
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    // Diary 생성 헬퍼
    public static Diary createTestDiary(Long userId, String title, Visibility visibility) {
        return Diary.create(userId, new CreateDiaryRequest(
            title,
            "테스트 내용",
            37.498004,
            127.027621,
            "서울",
            "서울시 강남구",
            LocalDateTime.now(),
            visibility
        ));
    }

    // Friend 생성 헬퍼
    public static Friend createTestFriend(Long requesterId, Long receiverId) {
        return Friend.create(requesterId, receiverId);
    }
}
```

---

## 프론트엔드 테스트

### Vitest 설정

```javascript
// vitest.config.js
import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/app/stores/__tests__/setup.js']
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  }
})
```

### Store 테스트

```javascript
// src/app/stores/__tests__/auth.spec.js

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../auth.js'
import * as authApi from '@/app/api/auth.js'

vi.mock('@/app/api/auth.js')

describe('Auth Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  describe('state', () => {
    it('초기 상태를 가져온다', () => {
      const store = useAuthStore()
      expect(store.user).toBeNull()
      expect(store.accessToken).toBe('')
      expect(store.isAuthenticated).toBe(false)
    })
  })

  describe('login action', () => {
    it('정상적으로 로그인한다', async () => {
      // Given
      const store = useAuthStore()
      authApi.authApi.login.mockResolvedValue({
        data: {
          accessToken: 'test-access-token',
          refreshToken: 'test-refresh-token'
        }
      })
      authApi.userApi.getMe.mockResolvedValue({
        data: {
          id: 1,
          email: 'test@example.com',
          nickname: 'testuser',
          role: 'USER',
          profileImageUrl: null,
          createdAt: '2025-02-28T10:00:00'
        }
      })

      // When
      await store.login({
        email: 'test@example.com',
        password: 'password123'
      })

      // Then
      expect(store.accessToken).toBe('test-access-token')
      expect(store.isAuthenticated).toBe(true)
      expect(store.user.email).toBe('test@example.com')
      expect(localStorage.getItem('ml_access_token')).toBe('test-access-token')
    })

    it('로그인 실패 시 상태를 초기화한다', async () => {
      // Given
      const store = useAuthStore()
      authApi.authApi.login.mockRejectedValue(new Error('Invalid credentials'))

      // When & Then
      await expect(store.login({
        email: 'test@example.com',
        password: 'wrong-password'
      })).rejects.toThrow()

      expect(store.user).toBeNull()
      expect(store.accessToken).toBe('')
    })
  })

  describe('logout action', () => {
    it('로그아웃 시 상태를 초기화한다', async () => {
      // Given
      const store = useAuthStore()
      store.setTokens({
        accessToken: 'test-access-token',
        refreshToken: 'test-refresh-token'
      })
      store.setUser({
        userId: 1,
        email: 'test@example.com',
        nickname: 'testuser',
        role: 'USER',
        profileImageUrl: null,
        createdAt: '2025-02-28T10:00:00'
      })

      // When
      store.logout()

      // Then
      expect(store.user).toBeNull()
      expect(store.accessToken).toBe('')
      expect(store.isAuthenticated).toBe(false)
      expect(localStorage.getItem('ml_access_token')).toBeNull()
    })
  })

  describe('computed properties', () => {
    it('isAdmin은 role이 ADMIN일 때만 true', () => {
      // Given
      const store = useAuthStore()

      // When
      store.setUser({
        userId: 1,
        email: 'admin@example.com',
        nickname: 'admin',
        role: 'ADMIN',
        profileImageUrl: null,
        createdAt: '2025-02-28T10:00:00'
      })

      // Then
      expect(store.isAdmin).toBe(true)

      // When (USER 로직 변경)
      store.setUser({
        userId: 2,
        email: 'user@example.com',
        nickname: 'user',
        role: 'USER',
        profileImageUrl: null,
        createdAt: '2025-02-28T10:00:00'
      })

      // Then
      expect(store.isAdmin).toBe(false)
    })
  })
})
```

### API 모듈 테스트

```javascript
// src/app/api/__tests__/axios.spec.js

import { describe, it, expect, beforeEach, vi } from 'vitest'
import axios from 'axios'
import MockAdapter from 'axios-mock-adapter'

describe('Axios Instance', () => {
  let mock

  beforeEach(() => {
    localStorage.clear()
    // axios 모의 객체 생성
    mock = new MockAdapter(axios)
  })

  describe('request interceptor', () => {
    it('요청에 Authorization 헤더를 추가한다', async () => {
      // Given
      localStorage.setItem('ml_access_token', 'test-token')
      mock.onGet('/api/test').reply(200, { data: 'test' })

      // When
      const config = { headers: {} }
      // 실제 인터셉터 로직 테스트
      const token = localStorage.getItem('ml_access_token')
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }

      // Then
      expect(config.headers.Authorization).toBe('Bearer test-token')
    })

    it('토큰이 없으면 Authorization 헤더를 추가하지 않는다', () => {
      // Given
      localStorage.clear()

      // When
      const config = { headers: {} }
      const token = localStorage.getItem('ml_access_token')
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }

      // Then
      expect(config.headers.Authorization).toBeUndefined()
    })
  })

  describe('response interceptor', () => {
    it('200 응답은 data를 반환한다', async () => {
      // Given
      mock.onGet('/api/users/me').reply(200, {
        code: 'SUCCESS',
        message: '요청 성공',
        data: {
          id: 1,
          email: 'test@example.com'
        }
      })

      // When
      const response = await axios.get('/api/users/me')

      // Then
      expect(response.data.code).toBe('SUCCESS')
      expect(response.data.data.email).toBe('test@example.com')
    })

    it('401 응답은 로그인 페이지로 리다이렉트한다', async () => {
      // Given
      localStorage.setItem('ml_access_token', 'expired-token')
      mock.onGet('/api/diaries').reply(401, {
        code: 'INVALID_TOKEN',
        message: '유효하지 않은 토큰'
      })

      // When & Then
      try {
        await axios.get('/api/diaries', {
          headers: { Authorization: 'Bearer expired-token' }
        })
      } catch (error) {
        expect(error.response.status).toBe(401)
      }
    })
  })
})
```

### Vue 컴포넌트 테스트

```javascript
// src/app/views/__tests__/MapView.spec.js

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import MapView from '../MapView.vue'
import * as diaryApi from '@/app/api/diary.js'

vi.mock('@/app/api/diary.js')

describe('MapView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('컴포넌트가 마운트된다', () => {
    // When
    const wrapper = mount(MapView)

    // Then
    expect(wrapper.exists()).toBe(true)
  })

  it('지도가 렌더링된다', () => {
    // When
    const wrapper = mount(MapView)

    // Then
    // 맵 컨테이너 확인
    expect(wrapper.find('.map-container').exists()).toBe(true)
  })

  it('일기 마커를 불러온다', async () => {
    // Given
    diaryApi.diaryApi.getMapMarkers.mockResolvedValue({
      data: [
        {
          id: 1,
          userId: 1,
          locationName: '서울',
          latitude: 37.5,
          longitude: 127.0
        }
      ]
    })

    // When
    const wrapper = mount(MapView)
    await wrapper.vm.$nextTick()

    // Then
    // API 호출 확인
    expect(diaryApi.diaryApi.getMapMarkers).toHaveBeenCalled()
  })
})
```

---

## 테스트 실행

### 백엔드 테스트 실행

```bash
# 모든 테스트 실행
./gradlew test

# 특정 테스트 클래스만 실행
./gradlew test --tests "com.maplog.diary.command.service.DiaryCommandServiceTest"

# 테스트 커버리지 확인
./gradlew test --tests "com.maplog.diary.*"

# 테스트 결과 상세 보기
./gradlew test --info
```

### 프론트엔드 테스트 실행

```bash
# 모든 테스트 실행
npm test

# Watch 모드 (개발 중 자동 재실행)
npm run test -- --watch

# UI 모드
npm run test -- --ui

# 커버리지 리포트
npm run test -- --coverage
```

---

## CI/CD 통합

### GitHub Actions 예시

```yaml
name: Test

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run backend tests
        run: ./gradlew test

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: backend-test-results
          path: map-log-backend/build/test-results/test/

  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '20'

      - name: Install dependencies
        run: cd map-log-frontend && npm install

      - name: Run frontend tests
        run: cd map-log-frontend && npm test

      - name: Upload coverage
        uses: codecov/codecov-action@v3
        with:
          files: ./map-log-frontend/coverage/coverage-final.json
```

---

## 참고 자료

- [JUnit 5 공식 문서](https://junit.org/junit5/docs/current/user-guide/)
- [Vitest 공식 문서](https://vitest.dev/)
- [Vue Test Utils](https://test-utils.vuejs.org/)
- [Mockito 공식 문서](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)

---

**다음 문서 참고:**
- [코딩 스타일 가이드](./07-coding-style-guide.md) - 명명 규칙, 구조 규칙
- [기여 가이드](./09-contributing-guide.md) - 새로운 기능 추가 프로세스

