# MapLog 기여 가이드

> **한 줄 요약**: MapLog에 기여하려면 이슈 검토, 브랜치 생성, 코드 작성, 테스트, PR 제출의 5단계 프로세스를 따릅니다.

## 목차 (Table of Contents)

- [개요](#개요)
- [개발 환경 설정](#개발-환경-설정)
- [새로운 기능 추가 프로세스](#새로운-기능-추가-프로세스)
- [Pull Request 가이드](#pull-request-가이드)
- [코드 리뷰 가이드](#코드-리뷰-가이드)
- [배포 프로세스](#배포-프로세스)

---

## 개요

### 기여 방식

MapLog는 다음과 같은 방식으로 기여할 수 있습니다:

1. **버그 리포트**: 발견한 버그를 이슈로 등록
2. **기능 개선**: 새로운 기능 또는 기존 기능 개선 제안
3. **문서 작성**: 문서 추가 또는 수정
4. **테스트 작성**: 기존 코드의 테스트 커버리지 향상

### 행동 수칙

모든 기여자는 존경과 전문성을 바탕으로 상호작용합니다:
- 다양한 의견 존중
- 건설적인 피드백 제공
- 팀의 의사결정 따르기
- 보안 및 개인정보 보호 우선

---

## 개발 환경 설정

### 1단계: 로컬 환경 준비

```bash
# 1. 저장소 클론
git clone https://github.com/20251029-hanhwa-swcamp-22th/be22-4st-team1-project.git
cd be22-4st-team1-project

# 2. 기본 브랜치로 이동
git checkout main

# 3. 최신 코드 내려받기
git pull origin main

# 4. 환경변수 설정
cp .env.example .env
# .env 파일을 열어 필요한 값 입력

# 5. Docker Compose로 MariaDB 시작
docker compose up -d mariadb

# 6. 백엔드 의존성 설치 및 실행
cd map-log-backend
./gradlew bootRun

# 7. (다른 터미널) 프론트엔드 의존성 설치 및 실행
cd map-log-frontend
npm install
npm run dev
```

### 2단계: IDE 설정

#### IntelliJ IDEA (Backend)

1. **프로젝트 열기**
   ```
   File → Open → map-log-backend 폴더 선택
   ```

2. **JDK 설정**
   ```
   File → Project Structure → Project
   SDK: openjdk-21
   Language level: Java 21
   ```

3. **Gradle 설정**
   ```
   Settings → Build, Execution, Deployment → Gradle
   Gradle JVM: openjdk-21
   ```

4. **코드 스타일 설정**
   ```
   Settings → Editor → Code Style → Java
   Import Scheme → Choose Google Java Style Guide
   ```

#### VS Code (Frontend)

1. **확장 프로그램 설치**
   ```
   Vetur (Vue 3)
   ESLint
   Prettier - Code formatter
   ```

2. **.vscode/settings.json** 생성
   ```json
   {
     "editor.defaultFormatter": "esbenp.prettier-vscode",
     "editor.formatOnSave": true,
     "[vue]": {
       "editor.defaultFormatter": "esbenp.prettier-vscode"
     }
   }
   ```

---

## 새로운 기능 추가 프로세스

### 단계 1: 이슈 생성 및 검토

```markdown
### 제목
[기능 요청] 사용자 프로필 이미지 업로드 기능

### 설명
사용자가 프로필 사진을 업로드할 수 있는 기능을 추가합니다.

### 요구사항
- 프로필 이미지 업로드 (최대 5MB)
- 이미지 자동 리사이징 (200x200px)
- AWS S3 저장
- 기존 이미지 자동 삭제

### 제안되는 구현
- User 엔티티에 profileImageUrl 필드 추가
- PUT /api/users/me/profile-image 엔드포인트 추가
- FileStorageService 사용

### 체크리스트
- [ ] 백엔드 구현 완료
- [ ] 프론트엔드 구현 완료
- [ ] 단위 테스트 작성
- [ ] 통합 테스트 작성
- [ ] 문서 업데이트
```

### 단계 2: 브랜치 생성

#### 브랜치 네이밍 규칙

```
<domain>/<feature-name>

예:
- user/profile-image-upload       # 사용자 기능
- diary/sharing-feature           # 일기 기능
- friend/request-notification     # 친구 기능
- common/optimize-database-query  # 공통/인프라
```

#### 브랜치 생성

```bash
# 최신 main에서 시작
git checkout main
git pull origin main

# 새로운 브랜치 생성
git checkout -b user/profile-image-upload

# 원격 저장소에 푸시 (설정)
git push -u origin user/profile-image-upload
```

### 단계 3: 백엔드 구현

#### 패키지 구조 따르기

```
src/main/java/com/maplog/user/
├── command/
│   ├── controller/UserCommandController.java     (새로 추가)
│   ├── service/UserCommandService.java           (수정)
│   ├── domain/User.java                          (수정)
│   └── dto/
│       └── UpdateProfileImageRequest.java        (새로 추가)
└── query/
    └── ...
```

#### 구현 예시

```java
// 1. Request DTO
public record UpdateProfileImageRequest(
    @NotNull(message = "이미지 파일은 필수입니다.")
    MultipartFile profileImage
) {
    public UpdateProfileImageRequest {
        if (profileImage.isEmpty()) {
            throw new IllegalArgumentException("빈 파일입니다.");
        }
        if (profileImage.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("파일 크기가 5MB를 초과합니다.");
        }
    }
}

// 2. Entity 수정
public class User {
    // ... 기존 필드 ...

    private String profileImageUrl;

    // 새 메서드 추가
    public void updateProfileImage(String profileImageUrl) {
        // 기존 이미지 삭제
        if (this.profileImageUrl != null) {
            // fileStorageService.delete(this.profileImageUrl);
        }
        this.profileImageUrl = profileImageUrl;
    }
}

// 3. Service 메서드 추가
@Service
@RequiredArgsConstructor
public class UserCommandService {

    private final UserCommandRepository userRepository;
    private final FileStorageService fileStorageService;

    public void updateProfileImage(Long userId, MultipartFile profileImage) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 이미지 저장
        String imageUrl = fileStorageService.store(profileImage);

        // 사용자 정보 업데이트
        user.updateProfileImage(imageUrl);
        userRepository.save(user);
    }
}

// 4. Controller 엔드포인트 추가
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserCommandController {

    private final UserCommandService userCommandService;

    @PutMapping("/me/profile-image")
    public ResponseEntity<ApiResponse<Void>> updateProfileImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("profileImage") MultipartFile profileImage) {
        Long userId = getUserIdFromDetails(userDetails);
        userCommandService.updateProfileImage(userId, profileImage);
        return ResponseEntity.ok(ApiResponse.success("프로필 이미지가 업데이트되었습니다.", null));
    }
}
```

### 단계 4: 프론트엔드 구현

#### 구현 예시

```vue
<!-- src/app/views/ProfileEditView.vue -->
<template>
  <div class="profile-edit">
    <h1>프로필 수정</h1>

    <!-- 프로필 이미지 미리보기 -->
    <div class="image-preview">
      <img
        v-if="previewUrl"
        :src="previewUrl"
        :alt="user.nickname"
        class="profile-image"
      >
      <div v-else class="placeholder">이미지 선택</div>
    </div>

    <!-- 파일 입력 -->
    <div class="form-group">
      <label for="profileImage">프로필 이미지</label>
      <input
        id="profileImage"
        ref="fileInput"
        type="file"
        accept="image/*"
        @change="onFileSelected"
      >
      <small>최대 5MB, JPG/PNG 형식</small>
    </div>

    <!-- 제출 버튼 -->
    <button
      @click="handleUpload"
      :disabled="!selectedFile || isLoading"
    >
      {{ isLoading ? '업로드 중...' : '업로드' }}
    </button>

    <div v-if="error" class="error-message">{{ error }}</div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useAuthStore } from '@/app/stores/auth.js'
import { userApi } from '@/app/api/user.js'

// 상태
const authStore = useAuthStore()
const fileInput = ref(null)
const selectedFile = ref(null)
const previewUrl = ref(null)
const isLoading = ref(false)
const error = ref(null)

// 계산된 속성
const user = computed(() => authStore.user)

// 메서드
function onFileSelected(event) {
  const file = event.target.files?.[0]
  if (!file) return

  // 파일 유효성 검사
  if (file.size > 5 * 1024 * 1024) {
    error.value = '파일 크기가 5MB를 초과합니다.'
    return
  }

  if (!file.type.startsWith('image/')) {
    error.value = '이미지 파일만 업로드 가능합니다.'
    return
  }

  selectedFile.value = file

  // 미리보기 생성
  const reader = new FileReader()
  reader.onload = (e) => {
    previewUrl.value = e.target?.result
  }
  reader.readAsDataURL(file)
  error.value = null
}

async function handleUpload() {
  if (!selectedFile.value) return

  isLoading.value = true
  error.value = null

  try {
    // FormData 생성
    const formData = new FormData()
    formData.append('profileImage', selectedFile.value)

    // API 호출
    await userApi.updateProfileImage(formData)

    // 사용자 정보 갱신
    await authStore.hydrateUser()

    // 성공 메시지
    alert('프로필 이미지가 업데이트되었습니다.')

    // 입력 초기화
    fileInput.value.value = ''
    selectedFile.value = null
    previewUrl.value = null
  } catch (err) {
    error.value = err.message || '업로드에 실패했습니다.'
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.profile-edit {
  max-width: 500px;
  margin: 0 auto;
  padding: 20px;
}

.image-preview {
  margin-bottom: 20px;
  width: 200px;
  height: 200px;
  border: 2px dashed #ccc;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.profile-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.placeholder {
  color: #999;
  text-align: center;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
}

.form-group input {
  display: block;
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.form-group small {
  display: block;
  margin-top: 4px;
  color: #666;
}

button {
  width: 100%;
  padding: 12px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
}

button:hover:not(:disabled) {
  background: #0056b3;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.error-message {
  margin-top: 12px;
  padding: 12px;
  background: #f8d7da;
  color: #721c24;
  border-radius: 4px;
}
</style>
```

### 단계 5: 테스트 작성

#### 백엔드 테스트

```java
// src/test/java/com/maplog/user/command/service/UserCommandServiceTest.java

@DisplayName("사용자 프로필 이미지 업데이트")
@Nested
class UpdateProfileImage {
    @Test
    @DisplayName("프로필 이미지를 정상적으로 업로드한다")
    void shouldUpdateProfileImageSuccessfully() throws IOException {
        // Given
        User testUser = TestDataFactory.createTestUser("test@example.com", "testuser");
        ReflectionTestUtils.setField(testUser, "id", 1L);

        MockMultipartFile mockFile = new MockMultipartFile(
            "profileImage",
            "profile.jpg",
            "image/jpeg",
            "image content".getBytes()
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(fileStorageService.store(mockFile)).thenReturn("https://s3.example.com/profile-1.jpg");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userCommandService.updateProfileImage(1L, mockFile);

        // Then
        assertThat(testUser.getProfileImageUrl()).isEqualTo("https://s3.example.com/profile-1.jpg");
        verify(fileStorageService).store(mockFile);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("5MB 이상의 파일은 업로드 실패한다")
    void shouldFailWhenFileSizeExceedsLimit() {
        // Given
        User testUser = TestDataFactory.createTestUser("test@example.com", "testuser");
        ReflectionTestUtils.setField(testUser, "id", 1L);

        byte[] largeContent = new byte[6 * 1024 * 1024];  // 6MB
        MockMultipartFile largeFile = new MockMultipartFile(
            "profileImage",
            "large.jpg",
            "image/jpeg",
            largeContent
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> userCommandService.updateProfileImage(1L, largeFile))
            .isInstanceOf(BusinessException.class);
    }
}
```

#### 프론트엔드 테스트

```javascript
// src/app/views/__tests__/ProfileEditView.spec.js

describe('ProfileEditView', () => {
  it('파일 선택 시 미리보기를 표시한다', async () => {
    // Given
    const wrapper = mount(ProfileEditView)
    const file = new File(['image content'], 'profile.jpg', { type: 'image/jpeg' })

    // When
    const fileInput = wrapper.find('input[type="file"]')
    await fileInput.setValue([file])
    await wrapper.vm.$nextTick()

    // Then
    expect(wrapper.vm.selectedFile).toBe(file)
    expect(wrapper.vm.previewUrl).toBeTruthy()
  })

  it('5MB 이상의 파일은 거절한다', async () => {
    // Given
    const wrapper = mount(ProfileEditView)
    const largeFile = new File(
      [new Uint8Array(6 * 1024 * 1024)],
      'large.jpg',
      { type: 'image/jpeg' }
    )

    // When
    const fileInput = wrapper.find('input[type="file"]')
    await fileInput.setValue([largeFile])
    await wrapper.vm.$nextTick()

    // Then
    expect(wrapper.vm.error).toBe('파일 크기가 5MB를 초과합니다.')
  })

  it('업로드 버튼 클릭 시 API를 호출한다', async () => {
    // Given
    const wrapper = mount(ProfileEditView)
    const file = new File(['image'], 'profile.jpg', { type: 'image/jpeg' })

    userApi.updateProfileImage = vi.fn().mockResolvedValue({ data: {} })

    // When
    wrapper.vm.selectedFile = file
    await wrapper.find('button').trigger('click')
    await wrapper.vm.$nextTick()

    // Then
    expect(userApi.updateProfileImage).toHaveBeenCalled()
  })
})
```

### 단계 6: 문서 업데이트

API 변경 시 다음 문서 업데이트:

```markdown
# docs/core/02-api-design.md

## User Resource (사용자)

### 프로필 이미지 업로드

**엔드포인트**: `PUT /api/users/me/profile-image`

**요청**:
```
multipart/form-data
- profileImage: File (최대 5MB)
```

**응답**:
```json
{
  "code": "SUCCESS",
  "message": "프로필 이미지가 업데이트되었습니다.",
  "data": null
}
```

**예시**:
```bash
curl -X PUT http://localhost:8080/api/users/me/profile-image \
  -H "Authorization: Bearer {access_token}" \
  -F "profileImage=@profile.jpg"
```
```

### 단계 7: Git 커밋

```bash
# 변경사항 확인
git status

# 파일 스테이징
git add map-log-backend/src/...
git add map-log-frontend/src/...
git add docs/core/02-api-design.md

# 커밋 (커밋 컨벤션 따르기)
git commit -m "feat(user): 프로필 이미지 업로드 기능 추가

- User 엔티티에 profileImageUrl 필드 추가
- PUT /api/users/me/profile-image 엔드포인트 구현
- FileStorageService를 통한 S3 저장
- 프론트엔드 프로필 편집 페이지 추가
- 백엔드/프론트엔드 단위 테스트 작성

이 기능으로 사용자가 프로필 이미지를 업로드할 수 있습니다.

Closes #42"

# 원격 저장소로 푸시
git push origin user/profile-image-upload
```

---

## Pull Request 가이드

### PR 생성

GitHub 웹에서 PR을 생성할 때:

```markdown
## 변경 요약

프로필 이미지 업로드 기능을 추가합니다.
- 백엔드: 파일 업로드 API 구현
- 프론트엔드: 프로필 편집 UI 추가
- 테스트: 단위/통합 테스트 작성

## 이슈 해결

Closes #42

## 변경 사항

### 백엔드
- [x] User 엔티티 수정 (profileImageUrl 필드 추가)
- [x] UserCommandService.updateProfileImage() 구현
- [x] PUT /api/users/me/profile-image 엔드포인트 추가
- [x] 단위 테스트 작성 (UserCommandServiceTest)

### 프론트엔드
- [x] ProfileEditView.vue 추가
- [x] userApi.updateProfileImage() 메서드 추가
- [x] 파일 업로드 에러 처리
- [x] 컴포넌트 테스트 작성

### 문서
- [x] API 설계 문서 업데이트
- [x] 개발 가이드 업데이트

## 테스트 계획

### 수동 테스트
1. 프로필 이미지 업로드 성공
2. 5MB 이상 파일 거절 확인
3. 기존 이미지 자동 삭제 확인
4. S3 이미지 URL 정상 반환

### 자동 테스트
- 백엔드: 8개 테스트 통과
- 프론트엔드: 6개 테스트 통과

## 스크린샷 (UI 변경 시)

[프로필 이미지 선택 화면]
[업로드 진행 화면]
[업로드 완료 화면]

## 체크리스트

- [x] 코드 스타일 규칙 준수
- [x] 테스트 작성 및 통과
- [x] 기존 테스트 여전히 통과
- [x] 문서 업데이트
- [x] 커밋 메시지 명확함
- [x] PR 제목이 명확함

## 추가 노트

로컬 테스트 방법:
```bash
# 백엔드 테스트
./gradlew test --tests "*ProfileImage*"

# 프론트엔드 테스트
npm test -- ProfileEditView.spec.js
```
```

### PR 제목 규칙

```
[Domain] Brief description

[A] 사용자 프로필 이미지 업로드 기능 추가
[B] 일기 공유 기능 개선
[C] 친구 요청 실시간 알림 추가
[D] 데이터베이스 성능 최적화

Domain:
- [A]: user, auth
- [B]: diary, scrap
- [C]: friend, notification, feed
- [D]: common, infra, database
```

---

## 코드 리뷰 가이드

### 리뷰어 체크리스트

```markdown
## 코드 품질
- [ ] 명명 규칙 준수
- [ ] 함수/클래스 크기 적절한가
- [ ] 중복 코드 없는가
- [ ] 주석 명확한가

## 기능 정확성
- [ ] 요구사항 충족하는가
- [ ] 엣지 케이스 처리하는가
- [ ] 에러 처리 적절한가
- [ ] 성능 문제 없는가

## 테스트
- [ ] 테스트 커버리지 충분한가
- [ ] 테스트가 의미 있는가
- [ ] 모든 테스트 통과하는가

## 문서
- [ ] README 또는 가이드 업데이트했는가
- [ ] API 문서 최신인가
- [ ] 변경사항 설명 명확한가

## 보안
- [ ] SQL Injection 위험 없는가
- [ ] XSS 대비 있는가
- [ ] 접근 제어 올바른가
- [ ] 민감한 데이터 로깅되지 않는가
```

### 건설적 피드백 예시

```markdown
❌ 나쁜 예:
"이 코드는 별로 좋지 않습니다."

✅ 좋은 예:
"이 메서드는 50줄을 넘고 있습니다.
다음과 같이 분리하면 가독성이 향상될 것 같습니다:
- validateInput() 메서드 추출
- processData() 메서드 추출

예시:
```java
private void validateInput(CreateDiaryRequest request) {
    // 검증 로직
}
```
"

❌ 나쁜 예:
"테스트를 다 작성해야 합니다."

✅ 좋은 예:
"UserCommandService의 updateProfileImage 메서드에 다음 테스트가 추가되면 좋을 것 같습니다:
- 5MB 이상 파일 거절
- 이미지 형식 검증
- 기존 이미지 자동 삭제 확인

이렇게 하면 테스트 커버리지가 90%에서 95%로 향상됩니다."
```

---

## 배포 프로세스

### 프로덕션 배포 체크리스트

```markdown
## 배포 전 확인사항

### 코드 검토
- [ ] 모든 PR 승인됨
- [ ] 모든 테스트 통과
- [ ] 코드 커버리지 >= 80%
- [ ] 보안 취약점 스캔 완료

### 문서
- [ ] README 최신
- [ ] API 문서 최신
- [ ] 마이그레이션 가이드 작성됨
- [ ] 알려진 이슈 문서화됨

### 데이터베이스
- [ ] 마이그레이션 스크립트 작성됨
- [ ] 백업 계획 수립됨
- [ ] 롤백 계획 수립됨

### 성능
- [ ] 성능 테스트 완료
- [ ] 부하 테스트 완료
- [ ] 메모리/CPU 사용량 확인

### 배포
- [ ] 스테이징 환경 배포 완료
- [ ] 스테이징 테스트 완료
- [ ] 배포 시간대 확인 (트래픽 적은 시간)
- [ ] 모니터링 알림 설정됨

### 롤백
- [ ] 롤백 계획 확인
- [ ] 롤백 스크립트 테스트됨
- [ ] 담당자 지정됨
```

---

## 참고 자료

- [GitHub 브랜칭 모델](https://git-flow.readthedocs.io/)
- [Conventional Commits](https://www.conventionalcommits.org/ko/)
- [코드 리뷰 모범 사례](https://google.github.io/eng-practices/review/)

---

**다음 문서 참고:**
- [코딩 스타일 가이드](./07-coding-style-guide.md) - 명명 규칙, 작성 패턴
- [테스트 가이드](./08-testing-guide.md) - 테스트 작성 방법

