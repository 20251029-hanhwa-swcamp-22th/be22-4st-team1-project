## PR Title
[Feat] 선택적 일기 공유 시스템, Amazon S3 연동 및 전역 코드 문서화

---

## What (작업 내용)

### 1. 테스트 환경 및 자동화 구축
- **백엔드**: JUnit 5, Mockito 기반의 단위 테스트 및 H2 In-memory DB를 활용한 통합 테스트(`UserFlowIntegrationTest`) 구축
- **프론트엔드**: Vitest 및 @vue/test-utils를 이용한 Store, Component, API Interceptor 테스트 환경 구축

### 2. 선택적 일기 공유 및 피드 기능
- **일기 가시성 정책 변경**: `PUBLIC` 가시성을 제거하고 `PRIVATE`, `FRIENDS_ONLY` 체계로 전환
- **공유 로직 구현**: 일기 생성/수정 시 특정 친구를 선택하여 공유할 수 있는 기능 추가 (`diary_shares` 테이블 연동)
- **개인화된 피드**: 내가 공유받은 친구의 일기만 모아보는 피드 기능 구현 (MyBatis 쿼리 최적화)

### 3. 알림 시스템 개선 및 버그 수정
- **상세 메시지**: 친구 요청/수락 시 상대방 닉네임이 포함되도록 알림 메시지 보완
- **실시간 반응성**: Pinia `notificationStore`를 도입하여 알림 읽음/삭제 시 사이드바 배지가 즉시 업데이트되도록 수정
- **DB 매핑 수정**: MyBatis 알림 조회 쿼리의 컬럼명 오류 해결 (`is_read`)

### 4. Amazon S3 스토리지 연동 및 보안 강화
- **S3 업로드/삭제**: 모든 이미지를 AWS S3에 저장하도록 전환 (AWS SDK v1 안정 버전 사용)
- **보안 조회 (Presigned URL)**: Private S3 버킷 보안 유지를 위해 1시간 유효한 임시 보안 URL 생성 로직 적용
- **자격 증명 보호**: AWS 키 정보를 `application-aws.yml`로 격리하고 Git 관리에서 제외

### 5. 코드 문서화 및 가이드 확충
- **전역 한글 주석**: 모든 핵심 서비스, 컨트롤러, 스토어, API 모듈에 한글 주석 및 Javadoc 추가
- **복잡 로직 설명**: S3 Presigned URL 생성, Axios 인터셉터의 토큰 갱신 큐잉 등 복잡한 로직에 단계별 가이드 작성

### 6. Docker 배포 환경 최적화
- **Nginx 도입**: 프론트엔드를 Nginx 기반 멀티 스테이지 빌드 방식으로 전환하여 배포 안정성 확보
- **환경 변수 구성**: `.env.example`을 통한 환경별 설정 가이드 제공

---

## Key Points (중점 사항)
- **보안 최우선**: AWS 키 노출 방지 및 이미지 보안 조회를 위한 Presigned URL 방식을 철저히 적용했습니다.
- **유지보수성**: 인터페이스 추상화를 통해 저장소 변경 시 비즈니스 로직 수정이 없도록 설계했습니다.
- **가독성**: 처음 프로젝트에 합류한 개발자도 코드를 즉시 이해할 수 있도록 상세한 한글 문서를 코드 내에 배치했습니다.

---

## Additional Notes (기타 참고사항)

### 1. 실행 방법 (Docker 사용 시)
1. 프로젝트 루트에 `.env` 파일을 생성하고 `.env.example`의 내용을 복사합니다.
2. `secret.md`에 기재된 S3 키 정보를 `.env` 파일에 입력합니다.
3. `docker-compose up --build -d` 명령어로 전체 시스템을 실행합니다.

### 2. 실행 방법 (로컬 개발 환경)
- **Backend**: `src/main/resources/application-aws.yml` 파일 생성 및 S3 정보 입력 후 실행
- **Frontend**: `npm install` -> `npm run dev`

---

## Related Issues
- Related to #
