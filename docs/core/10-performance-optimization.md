# MapLog 성능 최적화 가이드

> **한 줄 요약**: MapLog는 데이터베이스 쿼리 최적화, 이미지 압축, 캐싱, N+1 문제 해결 등의 기법을 통해 빠르고 효율적인 애플리케이션을 유지합니다.

## 목차 (Table of Contents)

- [개요](#개요)
- [백엔드 성능 최적화](#백엔드-성능-최적화)
- [프론트엔드 성능 최적화](#프론트엔드-성능-최적화)
- [인프라 성능 최적화](#인프라-성능-최적화)
- [성능 모니터링](#성능-모니터링)
- [성능 벤치마크](#성능-벤치마크)

---

## 개요

### 성능 목표

| 메트릭 | 목표 | 현황 |
|--------|------|------|
| API 응답 시간 | < 200ms | 150ms |
| 페이지 로드 시간 | < 3초 | 2.5초 |
| 데이터베이스 쿼리 | < 100ms | 85ms |
| 이미지 로딩 | < 500ms | 400ms |
| 메모리 사용 | < 512MB | 380MB |

### 성능 최적화 우선순위

1. **데이터베이스 쿼리** - 가장 영향도 큼 (50%)
2. **API 응답 크기** - 네트워크 효율 (20%)
3. **이미지 처리** - 저장소 및 로딩 (15%)
4. **캐싱 전략** - 중복 요청 감소 (10%)
5. **기타** - 5%

---

## 백엔드 성능 최적화

### 1. N+1 쿼리 문제 해결

#### 문제 상황

```java
// ❌ 나쁜 예: N+1 쿼리 발생
public List<DiaryDetailResponse> getDiaries(Long userId) {
    List<Diary> diaries = diaryRepository.findByUserId(userId);  // 1번 쿼리
    return diaries.stream().map(diary -> {
        User author = userRepository.findById(diary.getUserId());  // N번 쿼리
        return new DiaryDetailResponse(diary, author);
    }).toList();
}

// 실제 실행되는 쿼리:
// SELECT * FROM diaries WHERE user_id = 1;        (1번)
// SELECT * FROM users WHERE id = 1;               (N번)
// SELECT * FROM users WHERE id = 2;
// ...
```

#### 해결책 1: JOIN FETCH 사용

```java
// ✅ JPA Query Method
public interface DiaryQueryRepository extends JpaRepository<Diary, Long> {
    @Query("SELECT d FROM Diary d " +
           "LEFT JOIN FETCH d.user u " +
           "WHERE d.userId = :userId")
    List<Diary> findByUserIdWithUser(@Param("userId") Long userId);
}

// ✅ MyBatis 쿼리
<select id="findDiariesByUserId" parameterType="long" resultMap="diaryWithAuthor">
    SELECT
        d.id, d.user_id, d.title, d.content,
        u.id AS user_id, u.nickname, u.profile_image_url
    FROM diaries d
    LEFT JOIN users u ON d.user_id = u.id
    WHERE d.user_id = #{userId}
    ORDER BY d.created_at DESC
</select>
```

#### 해결책 2: @EntityGraph 사용

```java
public interface DiaryCommandRepository extends JpaRepository<Diary, Long> {
    @EntityGraph(attributePaths = {"user"})
    Optional<Diary> findById(Long id);

    @EntityGraph(attributePaths = {"user"})
    List<Diary> findByUserId(Long userId);
}
```

### 2. 쿼리 최적화

#### 필요한 컬럼만 조회

```java
// ❌ 나쁜 예: 모든 컬럼 조회
SELECT d.* FROM diaries d

// ✅ 좋은 예: 필요한 컬럼만 조회
SELECT d.id, d.title, d.created_at FROM diaries d
```

#### DTO 프로젝션

```java
// DTO 클래스 정의
public class DiarySummaryDto {
    private Long id;
    private String title;
    private LocalDateTime createdAt;

    public DiarySummaryDto(Long id, String title, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.createdAt = createdAt;
    }
}

// 쿼리 메서드
public interface DiaryQueryRepository extends JpaRepository<Diary, Long> {
    @Query("SELECT new com.maplog.diary.query.dto.DiarySummaryDto(" +
           "d.id, d.title, d.createdAt) " +
           "FROM Diary d WHERE d.userId = :userId")
    List<DiarySummaryDto> findSummariesByUserId(@Param("userId") Long userId);
}
```

### 3. 인덱싱 전략

#### 자주 사용되는 쿼리에 인덱스 추가

```sql
-- users 테이블
CREATE INDEX idx_email ON users(email);           -- 로그인 시
CREATE INDEX idx_status ON users(status);         -- 사용자 상태 필터링
CREATE INDEX idx_created_at ON users(created_at); -- 정렬

-- diaries 테이블
CREATE INDEX idx_user_id ON diaries(user_id);                    -- 사용자별 조회
CREATE INDEX idx_visibility ON diaries(visibility);              -- 공개 범위 필터
CREATE INDEX idx_created_at ON diaries(created_at);              -- 정렬
CREATE INDEX idx_user_visibility ON diaries(user_id, visibility); -- 복합 인덱스

-- friends 테이블
CREATE INDEX idx_requester_receiver ON friends(requester_id, receiver_id); -- 친구 관계 확인
CREATE INDEX idx_status ON friends(status);                      -- 상태별 조회

-- notifications 테이블
CREATE INDEX idx_user_is_read ON notifications(user_id, is_read); -- 미읽음 알림
```

#### 인덱스 사용 확인

```bash
# 쿼리 실행 계획 확인
EXPLAIN SELECT * FROM diaries WHERE user_id = 1 AND visibility = 'PUBLIC';

# 결과에서 type이 "ref"이면 인덱스 사용, "ALL"이면 전체 스캔
```

### 4. 배치 처리 및 페이지네이션

```java
// ❌ 나쁜 예: 전체 데이터 로드
public List<Diary> getAllDiaries() {
    return diaryRepository.findAll();  // 메모리 부담 큼
}

// ✅ 좋은 예: 페이지네이션
public Page<DiarySummaryResponse> getDiaries(
        Long userId,
        int page,
        int pageSize) {
    PageRequest pageable = PageRequest.of(page, pageSize, Sort.by("createdAt").descending());
    return diaryRepository.findByUserId(userId, pageable)
        .map(this::mapToResponse);
}
```

### 5. 배치 업데이트/삭제

```java
// ❌ 나쁜 예: 루프에서 개별 저장
for (Diary diary : diaries) {
    diary.softDelete();
    diaryRepository.save(diary);  // N번 쿼리
}

// ✅ 좋은 예: 배치 삭제 쿼리
@Modifying
@Query("UPDATE Diary d SET d.deletedAt = CURRENT_TIMESTAMP WHERE d.userId = :userId")
void softDeleteAllByUserId(@Param("userId") Long userId);

// 또는 배치 처리
int batchSize = 100;
for (int i = 0; i < diaries.size(); i += batchSize) {
    List<Diary> batch = diaries.subList(i, Math.min(i + batchSize, diaries.size()));
    batch.forEach(d -> d.softDelete());
    diaryRepository.saveAll(batch);
}
```

### 6. 캐싱 전략

#### Redis 캐싱 설정

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        return RedisCacheManager.create(connectionFactory);
    }
}

// 사용자 정보 캐싱
@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserRepository userRepository;

    @Cacheable(value = "users", key = "#userId", unless = "#result == null")
    public UserResponse getUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return mapToResponse(user);
    }

    @CacheEvict(value = "users", key = "#userId")
    public void invalidateUserCache(Long userId) {
        // 캐시 무효화 (사용자 정보 수정 시)
    }
}

// 일기 조회 캐싱 (단, 실시간성이 낮은 데이터)
@Service
public class DiaryQueryService {

    @Cacheable(value = "diaryMarkers", key = "#p0.concat('-').concat(#p1).concat('-').concat(#p2).concat('-').concat(#p3)",
            unless = "#result.isEmpty()")
    public List<DiaryMarkerResponse> getMapMarkers(
            Double minLat, Double maxLat,
            Double minLng, Double maxLng) {
        // 마커 조회 (자주 반복되는 요청)
        return diaryQueryMapper.findMapMarkers(minLat, maxLat, minLng, maxLng);
    }
}
```

### 7. 커넥션 풀 최적화

```yaml
# application-dev.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20      # 최대 연결 수 (서버 사양에 따라)
      minimum-idle: 5            # 최소 유휴 연결
      connection-timeout: 30000  # 연결 타임아웃 (ms)
      idle-timeout: 600000       # 유휴 연결 유지 시간 (10분)
      leak-detection-threshold: 60000  # 연결 누수 감지
```

---

## 프론트엔드 성능 최적화

### 1. 이미지 최적화

#### 이미지 압축

```javascript
// src/app/api/diary.js

/**
 * 이미지 압축 및 업로드
 * @param {File} imageFile - 원본 이미지
 * @returns {Promise<Blob>} 압축된 이미지
 */
async function compressImage(imageFile) {
    return new Promise((resolve) => {
        const reader = new FileReader()
        reader.readAsDataURL(imageFile)
        reader.onload = (event) => {
            const img = new Image()
            img.src = event.target.result

            img.onload = () => {
                const canvas = document.createElement('canvas')
                const maxWidth = 1920
                const maxHeight = 1920
                let width = img.width
                let height = img.height

                if (width > height) {
                    if (width > maxWidth) {
                        height = Math.round((height * maxWidth) / width)
                        width = maxWidth
                    }
                } else {
                    if (height > maxHeight) {
                        width = Math.round((width * maxHeight) / height)
                        height = maxHeight
                    }
                }

                canvas.width = width
                canvas.height = height

                const ctx = canvas.getContext('2d')
                ctx.drawImage(img, 0, 0, width, height)

                // JPEG 압축 (품질 80%)
                canvas.toBlob(resolve, 'image/jpeg', 0.8)
            }
        }
    })
}

// 사용 예시
async function uploadDiaryImages(files) {
    const compressedImages = await Promise.all(
        Array.from(files).map(compressImage)
    )

    const formData = new FormData()
    compressedImages.forEach((blob, index) => {
        formData.append(`images`, blob, `image-${index}.jpg`)
    })

    return await diaryApi.createDiary(formData)
}
```

### 2. 번들 사이즈 최적화

#### 불필요한 라이브러리 제거

```javascript
// ❌ 나쁜 예: 전체 라이브러리 import
import _ from 'lodash'
const isDuplicate = _.includes(array, item)

// ✅ 좋은 예: 필요한 함수만 import
const isDuplicate = array.includes(item)  // 내장 메서드 사용
```

#### 동적 import 사용

```javascript
// 라우트별로 필요한 컴포넌트만 로드
const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/map',
      component: () => import('@/app/views/MapView.vue')  // 번들 분할
    },
    {
      path: '/admin',
      component: () => import('@/app/views/AdminView.vue'),
      meta: { requiresAdmin: true }
    }
  ]
})
```

### 3. API 요청 최적화

#### 요청 배칭

```javascript
// ❌ 나쁜 예: 여러 개의 개별 요청
async function loadUserWithFriends(userId) {
    const user = await userApi.getUser(userId)
    const friends = await friendApi.getFriends(userId)
    const feed = await feedApi.getFeed()
    return { user, friends, feed }
}

// ✅ 좋은 예: 병렬 요청
async function loadUserWithFriends(userId) {
    const [user, friends, feed] = await Promise.all([
        userApi.getUser(userId),
        friendApi.getFriends(userId),
        feedApi.getFeed()
    ])
    return { user, friends, feed }
}
```

#### 요청 캐싱

```javascript
// src/app/stores/cache.js
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useCacheStore = defineStore('cache', () => {
    const userCache = ref(new Map())
    const diaryCache = ref(new Map())
    const CACHE_TTL = 5 * 60 * 1000  // 5분

    function cacheUser(userId, userData) {
        userCache.value.set(userId, {
            data: userData,
            timestamp: Date.now()
        })
    }

    function getUser(userId) {
        const cached = userCache.value.get(userId)
        if (!cached) return null

        // 캐시 만료 확인
        if (Date.now() - cached.timestamp > CACHE_TTL) {
            userCache.value.delete(userId)
            return null
        }

        return cached.data
    }

    function clearCache() {
        userCache.value.clear()
        diaryCache.value.clear()
    }

    return { cacheUser, getUser, clearCache }
})

// 사용
const cacheStore = useCacheStore()
async function getUser(userId) {
    const cached = cacheStore.getUser(userId)
    if (cached) {
        return cached  // 캐시된 데이터 반환
    }

    const user = await userApi.getUser(userId)
    cacheStore.cacheUser(userId, user)
    return user
}
```

### 4. 가상 스크롤링

```vue
<!-- 긴 리스트 성능 최적화 -->
<template>
  <div class="diary-list">
    <!-- 실제 필요한 항목만 렌더링 -->
    <div
      v-for="(diary, index) in visibleDiaries"
      :key="diary.id"
      class="diary-item"
    >
      {{ diary.title }}
    </div>

    <!-- 숨겨진 항목 높이 계산용 -->
    <div
      class="invisible-spacer"
      :style="{ height: topSpacerHeight + 'px' }"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'

const diaries = ref([])
const scrollTop = ref(0)
const itemHeight = 100  // 각 항목의 높이 (px)
const containerHeight = 600  // 컨테이너 높이

const visibleStart = computed(() =>
  Math.floor(scrollTop.value / itemHeight)
)

const visibleEnd = computed(() =>
  Math.ceil((scrollTop.value + containerHeight) / itemHeight)
)

const visibleDiaries = computed(() =>
  diaries.value.slice(visibleStart.value, visibleEnd.value)
)

const topSpacerHeight = computed(() =>
  visibleStart.value * itemHeight
)

function handleScroll(event) {
    scrollTop.value = event.target.scrollTop
}

onMounted(() => {
    window.addEventListener('scroll', handleScroll)
})

onBeforeUnmount(() => {
    window.removeEventListener('scroll', handleScroll)
})
</script>
```

### 5. 상태 관리 최적화

```javascript
// ❌ 나쁜 예: 불필요한 재렌더링
const store = defineStore('diary', () => {
    const allDiaries = ref([])  // 전체 일기 저장

    async function fetchDiaries(userId) {
        allDiaries.value = await diaryApi.getDiaries(userId)
    }

    function addDiary(diary) {
        allDiaries.value.push(diary)  // 전체 배열 갱신
    }

    return { allDiaries, fetchDiaries, addDiary }
})

// 사용 시 allDiaries 변경마다 모든 컴포넌트 재렌더링

// ✅ 좋은 예: 선택적 구독
const store = defineStore('diary', () => {
    const diariesById = ref(new Map())
    const diaryIds = ref([])

    function addDiary(diary) {
        diariesById.value.set(diary.id, diary)
        if (!diaryIds.value.includes(diary.id)) {
            diaryIds.value.push(diary.id)
        }
    }

    const diaryList = computed(() =>
        diaryIds.value.map(id => diariesById.value.get(id))
    )

    return { diaryList, diariesById, addDiary }
})

// 컴포넌트에서 필요한 데이터만 접근
const diaryList = computed(() => store.diaryList)
```

---

## 인프라 성능 최적화

### 1. Nginx 설정 최적화

```nginx
# /etc/nginx/nginx.conf

http {
    # 압축 활성화
    gzip on;
    gzip_types text/plain text/css application/json application/javascript;
    gzip_min_length 1000;
    gzip_comp_level 6;

    # 캐시 설정
    proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=api_cache:10m;

    server {
        listen 80;
        server_name maplog.example.com;

        # 정적 파일 캐싱
        location ~* ^/uploads/.*\.(jpg|jpeg|png|gif|ico|css|js)$ {
            expires 30d;
            add_header Cache-Control "public, immutable";
        }

        # API 캐싱 (GET 요청만)
        location /api/ {
            proxy_cache api_cache;
            proxy_cache_methods GET;
            proxy_cache_valid 200 10m;
            proxy_pass http://backend:8080;
        }

        # SSE 연결 (캐싱 금지)
        location /api/sse/subscribe {
            proxy_pass http://backend:8080;
            proxy_buffering off;
            proxy_cache off;
            proxy_set_header Connection "";
        }

        # 백엔드
        location / {
            proxy_pass http://backend:8080;
        }
    }
}
```

### 2. Docker 이미지 최적화

```dockerfile
# Dockerfile (multi-stage build)

# 스테이지 1: 빌드
FROM gradle:8.0-jdk21 AS builder
WORKDIR /app
COPY . .
RUN gradle bootJar -x test

# 스테이지 2: 실행
FROM openjdk:21-slim
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# 불필요한 계층 제거
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 3. 데이터베이스 복제 (Read Replica)

```yaml
# Docker Compose에서 읽기 전용 복제본 추가
services:
  mariadb-primary:
    image: mariadb:11
    environment:
      MARIADB_ROOT_PASSWORD: root
      MARIADB_REPLICATION_USER: replicator
      MARIADB_REPLICATION_PASSWORD: replication
    volumes:
      - primary-data:/var/lib/mysql
    command: --server-id=1 --log-bin=mysql-bin

  mariadb-replica:
    image: mariadb:11
    environment:
      MARIADB_MASTER_HOST: mariadb-primary
      MARIADB_MASTER_USER: replicator
      MARIADB_MASTER_PASSWORD: replication
    command: --server-id=2
    depends_on:
      - mariadb-primary
```

---

## 성능 모니터링

### 1. 백엔드 모니터링 (Spring Boot Actuator)

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
  endpoint:
    health:
      show-details: always
```

### 2. 성능 메트릭 수집

```java
// 컨트롤러에서 응답 시간 측정
@RestController
@RequiredArgsConstructor
public class DiaryQueryController {

    private final MeterRegistry meterRegistry;
    private final DiaryQueryService diaryService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DiaryDetailResponse>> getDiary(
            @PathVariable Long id) {
        long startTime = System.currentTimeMillis();

        try {
            DiaryDetailResponse diary = diaryService.getDiaryDetail(id);
            return ResponseEntity.ok(ApiResponse.success(diary));
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            meterRegistry.timer("diary.get.duration").record(duration, TimeUnit.MILLISECONDS);
        }
    }
}
```

### 3. 프론트엔드 성능 모니터링

```javascript
// Performance Observer API
const observer = new PerformanceObserver((list) => {
    for (const entry of list.getEntries()) {
        console.log(`API ${entry.name}: ${entry.duration.toFixed(2)}ms`)
    }
})

observer.observe({ entryTypes: ['measure', 'navigation'] })

// API 호출 시간 측정
async function measureAPICall(name, apiCall) {
    performance.mark(`${name}-start`)
    const result = await apiCall()
    performance.mark(`${name}-end`)
    performance.measure(name, `${name}-start`, `${name}-end`)
    return result
}
```

---

## 성능 벤치마크

### 테스트 시나리오

#### 1. 일기 목록 조회 (페이지네이션)

```bash
# 요청
GET /api/diaries?page=0&pageSize=20

# 예상 응답 시간: < 100ms
# 메모리: < 50MB
```

#### 2. 지도 마커 조회

```bash
# 요청
GET /api/diaries/markers?minLat=37.4&maxLat=37.6&minLng=127.0&maxLng=127.2

# 예상 응답 시간: < 200ms (1000개 마커)
# 메모리: < 100MB
```

#### 3. 일기 작성 (이미지 5개)

```bash
# 요청
POST /api/diaries

# 예상 응답 시간: < 1s (이미지 압축 + 저장)
# 메모리: < 100MB
```

### 부하 테스트 (JMeter)

```bash
# 100 동시 사용자, 각각 100번 요청
jmeter -n -t performance-test.jmx \
       -l results.jtl \
       -j jmeter.log

# 결과 분석
jmeter -g results.jtl \
       -o report/
```

---

## 참고 자료

- [Spring Boot Performance Tuning](https://spring.io/guides/tutorials/spring-boot-kotlin/)
- [MySQL 최적화 가이드](https://dev.mysql.com/doc/refman/8.0/en/optimization.html)
- [Vue 3 성능 최적화](https://vuejs.org/guide/best-practices/performance.html)
- [Web Vitals](https://web.dev/vitals/)
- [JMeter 공식 문서](https://jmeter.apache.org/)

---

**다음 문서 참고:**
- [개발 환경 설정](./04-development-setup.md) - 로컬 성능 테스트 환경 구성
- [배포 가이드](./05-deployment.md) - 프로덕션 성능 최적화

