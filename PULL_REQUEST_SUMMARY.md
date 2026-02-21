## PR Title
[Feat] 선택적 일기 공유 시스템 및 Amazon S3 이미지 스토리지 연동

---

## What (작업 내용)

### 1. 테스트 환경 및 자동화 구축
- **백엔드**: JUnit 5, Mockito 기반의 단위 테스트 및 H2 In-memory DB를 활용한 통합 테스트(`UserFlowIntegrationTest`) 구축
- **프론트엔드**: Vitest 및 @vue/test-utils를 이용한 Store, Component, API Interceptor 테스트 환경 구축

### 2. 선택적 일기 공유 및 피드 기능
- **일기 가시성 정책 변경**: `PUBLIC` 가시성을 제거하고 `PRIVATE`, `FRIENDS_ONLY` 체계로 전환
- **공유 로직 구현**: 일기 생성/수정 시 특정 친구를 선택하여 공유할 수 있는 기능 추가 (`diary_shares` 테이블 연동)
- **개인화된 피드**: 내가 공유받은 친구의 일기만 모아보는 피드 기능 구현 (MyBatis 쿼리 최적화)

### 3. 알림 시스템 개선
- **상세 메시지**: 친구 요청/수락 시 닉네임이 포함되도록 알림 메시지 보완
- **실시간 반응성**: Pinia `notificationStore`를 도입하여 알림 읽음/삭제 시 사이드바 배지가 즉시 업데이트되도록 수정
- **버그 수정**: MyBatis 알림 조회 쿼리의 컬럼명 매핑 오류 해결 (`is_read`)

### 4. Amazon S3 스토리지 연동 및 보안 강화
- **S3 업로드/삭제**: 모든 이미지(일기, 프로필)를 AWS S3에 저장하고 관리하도록 전환 (AWS SDK v1 안정 버전 사용)
- **보안 조회 (Presigned URL)**: Private S3 버킷 환경에서 보안을 유지하기 위해 1시간 유효한 임시 보안 URL 생성 로직 적용
- **자격 증명 분리**: AWS 키 정보를 별도의 설정 파일(`application-aws.yml`)로 격리하고 Git 관리에서 제외

### 5. Docker 배포 환경 고도화
- **docker-compose 업데이트**: S3 연동을 위한 환경 변수(`CLOUD_AWS_S3_*`) 설정 추가
- **프론트엔드 최적화**: Vite 개발 서버 방식에서 Nginx를 이용한 멀티 스테이지 빌드 배포 방식으로 변경
- **환경 변수 관리**: 민감 정보 관리를 위한 `.env.example` 파일 추가

---

## Key Points (중점 사항)
- **보안성**: AWS Access Key가 프론트엔드에 절대 노출되지 않으며, 이미지 조회 시에도 백엔드에서 서명된 URL을 생성하여 반환합니다.
- **반응형 알림**: Pinia 스토어 도입으로 알림 상태 변화가 전체 레이아웃에 실시간으로 반영됩니다.
- **배포 편의성**: Docker Compose를 통해 백엔드, 프론트엔드(Nginx), DB를 원클릭으로 실행할 수 있는 환경을 마련했습니다.

---

## Additional Notes (기타 참고사항)

### 1. 실행 방법 (Docker 사용 시)
1. 프로젝트 루트에 `.env` 파일을 생성하고 `.env.example`의 내용을 복사합니다.
2. `secret.md`에 기재된 S3 키 정보를 `.env` 파일에 입력합니다.
3. `docker-compose up --build -d` 명령어로 전체 시스템을 실행합니다.

### 2. 실행 방법 (로컬 개발 환경)
**백엔드 (Backend)**
- `map-log-backend/src/main/resources/application-aws.yml` 파일을 생성하고 AWS S3 정보를 입력합니다.
- `./gradlew bootRun`으로 실행합니다.

**프론트엔드 (Frontend)**
- `cd map-log-frontend` -> `npm install` -> `npm run dev`

### 3. .gitignore 처리 내역
- 백엔드 AWS 설정 파일(`application-aws.yml`) 및 로컬 `.env` 파일이 Git에 포함되지 않도록 설정했습니다.

---

## Related Issues
- Related to #
