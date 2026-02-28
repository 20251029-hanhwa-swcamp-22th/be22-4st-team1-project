# MapLog 코딩 스타일 가이드

> **한 줄 요약**: MapLog는 일관된 코드 스타일을 유지하기 위해 백엔드(Java)와 프론트엔드(JavaScript/Vue)에 대해 명확한 명명 규칙, 파일 구조, 작성 패턴을 정의합니다.

## 목차 (Table of Contents)

- [개요](#개요)
- [공통 규칙](#공통-규칙)
- [백엔드 (Java) 스타일 가이드](#백엔드-java-스타일-가이드)
- [프론트엔드 (JavaScript/Vue) 스타일 가이드](#프론트엔드-javascriptvue-스타일-가이드)
- [데이터베이스 (SQL) 스타일 가이드](#데이터베이스-sql-스타일-가이드)
- [Commit 메시지 규칙](#commit-메시지-규칙)

---

## 개요

### 목표
- 팀원 간 코드 리뷰 효율성 증대
- 신입 개발자의 온보딩 시간 단축
- 버그 발생 가능성 감소
- 일관된 프로젝트 구조 유지

### 적용 범위
- 모든 백엔드 Java 파일
- 모든 프론트엔드 JavaScript/Vue 파일
- SQL 쿼리 및 마이그레이션
- Git commit 메시지

---

## 공통 규칙

### 인코딩
- **파일 인코딩**: UTF-8 (BOM 제외)
- **라인 종료**: LF (Unix-style)
- **들여쓰기**: 탭 1개 (4 spaces 설정 권장)

### 주석 (Comments)

#### 한국어 사용 규칙
```java
// 좋은 예: 한국어 주석
// 사용자 인증 후 토큰 발급
public String generateAccessToken(String email) { ... }

// 나쁜 예: 영어와 한국어 혼용
// Get access token from user email
// 사용자 이메일로부터 토큰 획득
public String generateAccessToken(String email) { ... }
```

#### Javadoc (Java)
```java
/**
 * 사용자 인증 정보로 토큰을 생성합니다.
 *
 * @param email 사용자 이메일
 * @return 생성된 Access Token
 * @throws BusinessException JWT 생성 실패 시
 */
public String generateAccessToken(String email) throws BusinessException { ... }
```

#### JSDoc (JavaScript)
```javascript
/**
 * Axios 요청 인터셉터 설정
 * @param {AxiosInstance} instance - Axios 인스턴스
 * @returns {void}
 */
function setupRequestInterceptor(instance) { ... }
```

### 길이 제한
- **최대 라인 길이**: 120 자 (필요시 초과 가능, 가독성 우선)
- **함수 길이**: 50줄 이내 권장 (복잡한 로직은 분리)
- **클래스 크기**: 500줄 이상일 경우 분리 검토

---

## 백엔드 (Java) 스타일 가이드

### 패키지 명명 규칙
```
com.maplog.{domain}.{layer}.{component}
```

**예시**:
```
com.maplog.diary.command.controller    (명령형 컨트롤러)
com.maplog.diary.command.service       (명령형 서비스)
com.maplog.diary.command.domain        (도메인 모델)
com.maplog.friend.query.service        (조회형 서비스)
com.maplog.common.config               (설정)
com.maplog.common.exception            (예외 처리)
```

### 클래스 명명 규칙

#### Controller
```java
// 패턴: {Domain}{Command|Query}Controller
public class DiaryCommandController { ... }     // 올바름
public class DiaryQueryController { ... }       // 올바름
public class DiaryController { ... }            // 피할 것 (command/query 불명확)
```

#### Service
```java
// 패턴: {Domain}{Command|Query}Service
public class DiaryCommandService { ... }
public class DiaryQueryService { ... }
public class AuthService { ... }                // 특수: 인증은 항상 Command
```

#### Domain (Entity)
```java
// 패턴: {Entity}
public class Diary { ... }              // 올바름
public class DiaryEntity { ... }        // 피할 것 (중복된 접미사)
```

#### Repository
```java
// 패턴: {Domain}{Command|Query}Repository
public interface DiaryCommandRepository extends JpaRepository<Diary, Long> { ... }
public interface DiaryQueryRepository extends JpaRepository<Diary, Long> { ... }
```

#### Mapper (MyBatis)
```java
// 패턴: {Domain}QueryMapper
public interface DiaryQueryMapper { ... }
```

#### DTO
```java
// 요청 DTO: {Action}{Resource}Request
public record CreateDiaryRequest(
    String title,
    String content,
    Double latitude,
    Double longitude,
    String locationName,
    String address,
    LocalDateTime visitedAt,
    Visibility visibility
) { }

// 응답 DTO: {Resource}{ViewName}Response
public class DiaryDetailResponse { ... }
public class DiarySummaryResponse { ... }
public record DiaryMarkerResponse(
    Long id,
    Long userId,
    String locationName,
    Double latitude,
    Double longitude
) { }
```

#### Enum
```java
public enum Visibility {
    PUBLIC,           // 모든 인증 사용자에게 공개
    PRIVATE,          // 작성자만 조회
    FRIENDS_ONLY      // 작성자 + 친구만 조회
}
```

### 메서드 명명 규칙

```java
// Getter/Setter (생성하지 말 것, Lombok @Getter 사용)
// @Getter
// private String title;  // 자동 생성: getTitle()

// 생성 팩토리 메서드
public static Diary create(Long userId, CreateDiaryRequest request) { ... }

// 수정 메서드
public void update(UpdateDiaryRequest request) { ... }

// 삭제 메서드
public void softDelete() { ... }

// 상태 확인
public boolean isOwner(Long userId) { ... }
public boolean isDeleted() { ... }

// 서비스 레이어: 도메인 동작을 설명하는 명령형
public void createDiary(Long userId, CreateDiaryRequest request) { ... }
public DiaryDetailResponse getDiaryDetail(Long diaryId, Long requesterId) { ... }
public void deleteDiary(Long diaryId, Long requesterId) { ... }
```

### 필드 순서

클래스의 필드는 다음 순서로 정렬:

```java
@Entity
@Table(name = "users")
public class User {
    // 1. 기본 키
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 2. 비즈니스 필드 (중요도순)
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String nickname;

    // 3. 부가 정보 필드
    private String profileImageUrl;

    // 4. Enum/Status 필드
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    // 5. 타임스탬프 필드
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
```

### 접근 제한자 (Access Modifiers)

```java
public class Diary {
    // public: 필요한 경우만 (API 응답 DTO 등)

    // protected: 상속 계층에서만 필요할 때

    // private (기본): 대부분의 필드와 내부 메서드
    @Column(nullable = false)
    private String title;

    // 주의: Lombok을 사용하므로 명시적 getter 작성 금지
    // ❌ public String getTitle() { return this.title; }
    // ✅ @Getter 어노테이션으로 자동 생성
}

public class DiaryCommandService {
    // private: 내부 헬퍼 메서드
    private void validateDiaryOwnership(Long diaryId, Long userId) { ... }

    // public: API에 노출되는 메서드
    public void updateDiary(Long diaryId, UpdateDiaryRequest request) { ... }
}
```

### Exception 처리

```java
// BusinessException 사용 (checked exception 대신)
public void validateUser(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    if (user.isDeleted()) {
        throw new BusinessException(ErrorCode.DELETED_USER);
    }
}

// ErrorCode는 Enum으로 중앙 관리
public enum ErrorCode {
    USER_NOT_FOUND("사용자를 찾을 수 없습니다."),
    INVALID_TOKEN("유효하지 않은 토큰입니다."),
    UNAUTHORIZED("권한이 없습니다.");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }
}
```

### Annotation 순서

```java
@Entity
@Table(name = "diaries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Diary {
    // 메타 주석 먼저 (@Entity, @Table 등)
    // 데이터 주석 나중 (@Column, @Enumerated 등)
    // 라이브러리 주석 마지막 (@Getter, @Builder 등)
}
```

### 상수 정의

```java
public class DiaryConstants {
    // 상수는 모두 대문자 + 언더스코어
    public static final int MAX_TITLE_LENGTH = 200;
    public static final int MAX_CONTENT_LENGTH = 10000;
    public static final int MAX_IMAGES_PER_DIARY = 10;
    public static final long MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024;  // 10MB
}

// 사용
if (title.length() > DiaryConstants.MAX_TITLE_LENGTH) {
    throw new BusinessException(ErrorCode.INVALID_REQUEST);
}
```

---

## 프론트엔드 (JavaScript/Vue) 스타일 가이드

### 파일 명명 규칙

```
src/app/
├── components/
│   ├── Layout.vue                    # PascalCase, 재사용 가능한 컴포넌트
│   ├── DaumPostcode.vue              # 써드파티 라이브러리는 원래 이름 유지
│   └── [예약된 이름].vue             # 컴포넌트 이름 충돌 회피
├── views/
│   ├── MapView.vue                   # PascalCase, 페이지 단위 컴포넌트
│   ├── DiaryDetailView.vue
│   └── [Route이름]View.vue
├── stores/
│   ├── auth.js                       # camelCase (Pinia store)
│   ├── notification.js
│   └── __tests__/auth.spec.js        # 테스트는 .spec.js
├── api/
│   ├── axios.js                      # Axios 인스턴스 + 인터셉터
│   ├── auth.js                       # API 모듈
│   ├── diary.js
│   ├── user.js
│   └── __tests__/axios.spec.js
├── router/
│   └── index.js                      # 라우트 정의
└── data/
    └── MockData.js                   # 테스트용 Mock 데이터
```

### Vue 3 Composition API 패턴

```vue
<template>
  <div class="diary-detail">
    <!-- 1. 템플릿 로직은 간단하게 유지 -->
    <h1>{{ diary.title }}</h1>
    <p>{{ diary.content }}</p>
    <button @click="handleDelete">삭제</button>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { diaryApi } from '@/app/api/diary.js'

// Props 정의
const props = defineProps({
  diaryId: {
    type: Number,
    required: true
  }
})

// Emits 정의
const emit = defineEmits(['update:modelValue', 'delete'])

// 상태 관리 (ref + reactive)
const diary = ref(null)
const loading = ref(false)
const error = ref(null)

// 라우터
const router = useRouter()

// 계산된 속성 (computed)
const isOwner = computed(() => {
  // 이 로직은 권한 확인 필요
  return true
})

const formattedDate = computed(() => {
  return diary.value ? diary.value.createdAt.slice(0, 10) : ''
})

// 라이프사이클 훅
onMounted(async () => {
  await loadDiary()
})

// 메서드 (이벤트 핸들러)
async function loadDiary() {
  loading.value = true
  error.value = null

  try {
    const response = await diaryApi.getDiaryDetail(props.diaryId)
    diary.value = response.data
  } catch (err) {
    error.value = err.message || '일기를 불러오는데 실패했습니다.'
  } finally {
    loading.value = false
  }
}

async function handleDelete() {
  if (!confirm('정말 삭제하시겠습니까?')) {
    return
  }

  try {
    await diaryApi.deleteDiary(props.diaryId)
    emit('delete')
    router.push('/diaries')
  } catch (err) {
    error.value = err.message || '삭제에 실패했습니다.'
  }
}
</script>

<style scoped>
.diary-detail {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.diary-detail h1 {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 16px;
}

.diary-detail p {
  line-height: 1.6;
  color: #333;
}
</style>
```

### JavaScript/API 모듈 패턴

```javascript
// src/app/api/diary.js
import api from './axios.js'

export const diaryApi = {
  /**
   * 일기 목록 조회
   * @param {Object} params - 쿼리 파라미터
   * @param {number} params.page - 페이지 번호
   * @param {number} params.pageSize - 페이지 크기
   * @returns {Promise<{data: Array, totalCount: number}>}
   */
  async getDiaries(params = {}) {
    const response = await api.get('/diaries', { params })
    return response
  },

  /**
   * 일기 상세 조회
   * @param {number} diaryId - 일기 ID
   * @returns {Promise<{data: Object}>}
   */
  async getDiaryDetail(diaryId) {
    const response = await api.get(`/diaries/${diaryId}`)
    return response
  },

  /**
   * 일기 생성
   * @param {FormData} formData - 제목, 내용, 이미지 포함
   * @returns {Promise<{data: {id: number}}>}
   */
  async createDiary(formData) {
    const response = await api.post('/diaries', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    return response
  },

  /**
   * 일기 수정
   * @param {number} diaryId - 일기 ID
   * @param {Object} data - 수정할 데이터
   * @returns {Promise<void>}
   */
  async updateDiary(diaryId, data) {
    await api.put(`/diaries/${diaryId}`, data)
  },

  /**
   * 일기 삭제
   * @param {number} diaryId - 일기 ID
   * @returns {Promise<void>}
   */
  async deleteDiary(diaryId) {
    await api.delete(`/diaries/${diaryId}`)
  }
}
```

### Pinia Store 패턴

```javascript
// src/app/stores/notification.js
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { notificationApi } from '@/app/api/notification.js'

export const useNotificationStore = defineStore('notification', () => {
  // State
  const notifications = ref([])
  const unreadCount = ref(0)
  const loading = ref(false)
  const error = ref(null)

  // Computed
  const unreadNotifications = computed(() =>
    notifications.value.filter(n => !n.read)
  )

  const hasUnread = computed(() => unreadCount.value > 0)

  // Actions
  async function fetchNotifications() {
    loading.value = true
    error.value = null

    try {
      const response = await notificationApi.getNotifications()
      notifications.value = response.data || []
      unreadCount.value = unreadNotifications.value.length
    } catch (err) {
      error.value = err.message || '알림을 불러오는데 실패했습니다.'
    } finally {
      loading.value = false
    }
  }

  async function markAsRead(notificationId) {
    try {
      await notificationApi.markAsRead(notificationId)

      // 로컬 상태 즉시 업데이트
      const notification = notifications.value.find(n => n.id === notificationId)
      if (notification) {
        notification.read = true
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
    } catch (err) {
      error.value = err.message || '알림 표시에 실패했습니다.'
    }
  }

  async function deleteNotification(notificationId) {
    try {
      await notificationApi.deleteNotification(notificationId)
      notifications.value = notifications.value.filter(n => n.id !== notificationId)
    } catch (err) {
      error.value = err.message || '알림 삭제에 실패했습니다.'
    }
  }

  function addNotification(notification) {
    notifications.value.unshift(notification)
    if (!notification.read) {
      unreadCount.value++
    }
  }

  function clear() {
    notifications.value = []
    unreadCount.value = 0
    error.value = null
  }

  return {
    // State
    notifications,
    unreadCount,
    loading,
    error,

    // Computed
    unreadNotifications,
    hasUnread,

    // Actions
    fetchNotifications,
    markAsRead,
    deleteNotification,
    addNotification,
    clear
  }
})
```

### 변수명 규칙

```javascript
// camelCase 사용
const userName = ref('')
const isLoading = ref(false)
const userIdList = ref([])

// 상수는 UPPER_SNAKE_CASE
const MAX_USERNAME_LENGTH = 50
const API_BASE_URL = 'http://localhost:8080'

// Boolean은 is/has/should 접두사
const isVisible = ref(false)
const hasErrors = ref(false)
const shouldShowButton = computed(() => !isLoading.value)

// 나쁜 예
const user_name = ref('')          // snake_case 금지
const UserName = ref('')            // PascalCase (클래스만)
const isUserNameVisible = ref(false) // 너무 길면 분리 검토
```

### Promise/Async 패턴

```javascript
// ✅ 좋은 예: async/await
async function saveDiary() {
  try {
    loading.value = true
    const response = await diaryApi.createDiary(formData)
    await router.push(`/diary/${response.data.id}`)
  } catch (err) {
    showError(err.message)
  } finally {
    loading.value = false
  }
}

// ❌ 피해야 할 패턴: then/catch 체인
diaryApi.createDiary(formData)
  .then(response => {
    return router.push(`/diary/${response.data.id}`)
  })
  .catch(err => {
    showError(err.message)
  })
```

---

## 데이터베이스 (SQL) 스타일 가이드

### SQL 작성 규칙

```sql
-- 1. 키워드는 대문자
-- 2. 테이블/컬럼명은 소문자 + 언더스코어
-- 3. 들여쓰기로 가독성 향상

-- ✅ 좋은 예
SELECT u.id, u.nickname, COUNT(d.id) AS diary_count
FROM users u
LEFT JOIN diaries d ON u.id = d.user_id
WHERE u.status = 'ACTIVE'
GROUP BY u.id
ORDER BY u.created_at DESC;

-- ❌ 나쁜 예
select u.id, u.nickname, count(d.id) as diary_count from users u left join diaries d on u.id = d.user_id where u.status = 'ACTIVE' group by u.id;
```

### MyBatis Mapper 패턴

```xml
<!-- src/main/resources/mybatis/mapper/DiaryQueryMapper.xml -->
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.maplog.diary.query.mapper.DiaryQueryMapper">

  <!-- 쿼리 ID는 메서드명과 동일 -->
  <select id="findDiariesByUserId" parameterType="long" resultType="com.maplog.diary.query.dto.DiarySummaryResponse">
    SELECT
      d.id,
      d.title,
      d.location_name,
      d.created_at
    FROM diaries d
    WHERE d.user_id = #{userId}
      AND d.deleted_at IS NULL
    ORDER BY d.created_at DESC
    LIMIT #{limit} OFFSET #{offset}
  </select>

  <!-- 맵 파라미터 -->
  <select id="findMapMarkers" parameterType="map" resultType="com.maplog.diary.query.dto.DiaryMarkerResponse">
    SELECT
      d.id,
      d.user_id,
      d.location_name,
      d.latitude,
      d.longitude
    FROM diaries d
    WHERE d.deleted_at IS NULL
      AND d.latitude BETWEEN #{minLat} AND #{maxLat}
      AND d.longitude BETWEEN #{minLng} AND #{maxLng}
      AND (
        d.visibility = 'PUBLIC'
        OR (d.visibility = 'FRIENDS_ONLY' AND EXISTS (
          SELECT 1 FROM friends f
          WHERE f.requester_id = #{userId} AND f.receiver_id = d.user_id AND f.status = 'ACCEPTED'
          UNION
          SELECT 1 FROM friends f
          WHERE f.receiver_id = #{userId} AND f.requester_id = d.user_id AND f.status = 'ACCEPTED'
        ))
        OR d.user_id = #{userId}
      )
  </select>

</mapper>
```

---

## Commit 메시지 규칙

### Conventional Commits 기반

```
<type>[optional scope]: <description>

[optional body]

[optional footer]
```

### Type

| Type | 설명 | 예 |
|------|------|-----|
| `feat` | 새로운 기능 | `feat: 일기 공유 기능 추가` |
| `fix` | 버그 수정 | `fix: 토큰 갱신 실패 문제 해결` |
| `refactor` | 기능 변화 없는 코드 개선 | `refactor: DiaryService 메서드 분리` |
| `test` | 테스트 추가/수정 | `test: 일기 조회 단위 테스트 추가` |
| `docs` | 문서 수정 | `docs: API 설계 문서 업데이트` |
| `chore` | 빌드, 의존성, 설정 변경 | `chore: Spring Boot 업그레이드` |
| `ci` | CI/CD 설정 변경 | `ci: Jenkins 파이프라인 수정` |

### Scope (선택)

도메인 또는 모듈을 명시:

```
feat(diary): 일기 공유 기능 추가
feat(auth): JWT 토큰 갱신 로직 개선
fix(notification): SSE 연결 끊김 문제 해결
```

### Description (제목)

- 명령형으로 작성 (동사 원형)
- 50자 이내
- 첫 글자는 대문자
- 마침표 제외

```
✅ feat: 일기 공유 기능 추가
❌ feat: 일기 공유 기능을 추가했습니다
❌ feat: 일기를 공유할 수 있도록 구현.
```

### Body (선택)

상세한 설명이 필요한 경우:

```
feat(diary): 일기 공유 기능 추가

- 특정 사용자에게 일기 공유 가능
- DiaryShare 엔티티 추가
- /api/diaries/{id}/share 엔드포인트 구현
- 공유 대상에게 실시간 알림 전송

이 기능으로 협업 일기 작성이 가능해집니다.
```

### Footer (선택)

이슈 참조:

```
feat(diary): 일기 공유 기능 추가

관련 이슈 #42, #45 해결

Closes #42
Closes #45
```

### 완전한 예시

```
feat(friend): 친구 요청 거절 기능 추가

- FriendStatus에 REJECTED 추가
- Friend.reject() 메서드 구현
- /api/friends/requests/{id}/reject 엔드포인트 추가
- 요청 거절 시 거절 대상에 알림 전송

사용자가 친구 요청을 거절할 수 있게 되었습니다.

Closes #38
```

---

## 참고 자료

- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- [Airbnb JavaScript Style Guide](https://github.com/airbnb/javascript)
- [Vue 3 Style Guide](https://vuejs.org/guide/scaling-up/styling.html)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [SQL Style Guide](https://www.sqlstyle.guide/)

---

**다음 문서 참고:**
- [테스트 가이드](./08-testing-guide.md) - 단위 테스트, 통합 테스트 작성 방법
- [기여 가이드](./09-contributing-guide.md) - 새로운 기능 추가 프로세스
- [성능 최적화](./10-performance-optimization.md) - 데이터베이스, 이미지 처리 등의 최적화

