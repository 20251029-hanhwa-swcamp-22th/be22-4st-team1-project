# 01. Getting Started (시작 가이드)

이 문서는 MapLog 프로젝트를 로컬 환경에서 구축하고 실행하기 위한 가이드를 제공합니다.

## 1. 전제 조건 (Prerequisites)
프로젝트 실행을 위해 다음 도구들이 설치되어 있어야 합니다.
- **Java:** JDK 21
- **Node.js:** v20 이상 (npm 포함)
- **Docker:** Desktop 버전 (Kubernetes 활성화 권장)
- **Database:** MariaDB 11 (로컬 실행 시)

## 2. 환경 변수 설정 (Environment Variables)
프로젝트는 민감한 정보를 환경 변수로 관리합니다. 로컬 실행과 K8s 실행 방식에 따라 설정 방법이 다릅니다.

### 2.1 로컬 실행 (Local Development)
`.env.example` 파일을 참고하여 각 프로젝트 루트에 환경 변수 파일을 생성합니다.

- **Backend (`map-log-backend/`):** `src/main/resources/application-dev.yml`의 기본값을 사용하거나 시스템 환경 변수를 설정합니다.
- **Frontend (`map-log-frontend/`):** `.env.local` 파일을 생성합니다.
  ```bash
  VITE_API_BASE_URL=http://localhost:8080
  VITE_KAKAO_MAP_KEY=your_kakao_javascript_key
  ```

### 2.2 Kubernetes 실행 (K8s Deployment)
필수 키값은 외부 레포지토리(`k8s-manifests`)의 `secret.yaml`과 **Jenkins Credentials**를 통해 주입됩니다. 배포 전 다음 항목들이 올바르게 설정되어 있는지 확인하십시오.

#### 🔐 관리 대상 필수 키 (Required Keys)
| 구분 | Secret 키 이름 (K8s) | 환경 변수명 (App) | 용도 |
| :--- | :--- | :--- | :--- |
| **Database** | `DB_PASSWORD` | `SPRING_DATASOURCE_PASSWORD` | MariaDB 루트 비밀번호 |
| **AWS S3** | `AMAZON_S3_ACCESS_KEY` | `CLOUD_AWS_CREDENTIALS_ACCESS_KEY` | S3 접근용 Access Key |
| **AWS S3** | `AMAZON_S3_SECRET_KEY` | `CLOUD_AWS_CREDENTIALS_SECRET_KEY` | S3 접근용 Secret Key |
| **AWS S3** | `AMAZON_S3_BUCKET_NAME` | `CLOUD_AWS_S3_BUCKET` | S3 버킷 이름 |
| **AWS S3** | `AMAZON_S3_REGION` | `CLOUD_AWS_REGION_STATIC` | S3 리전 설정 (예: ap-southeast-2) |
| **Auth** | `JWT_SECRET` | `JWT_SECRET` | JWT 서명용 비밀키 (최소 32자) |
| **Maps** | `(Jenkins Secret)` | `VITE_KAKAO_MAP_KEY` | 카카오 맵 JavaScript 키 |

#### ⚙️ 설정 방법
1.  **K8s Secrets:** `k8s-manifests/secret.yaml`의 `stringData` 영역에 실제 값을 입력하고 클러스터에 적용합니다.
2.  **Jenkins:** CI 파이프라인 빌드 시 `KAKAO_MAP_KEY` 이름의 Secret Text 자격 증명이 등록되어 있어야 프론트엔드 빌드 시 정상 주입됩니다.

## 3. 데이터베이스 초기화
현재 프로젝트는 **Spring Data JPA**의 `ddl-auto: update` 설정을 사용합니다.
- 애플리케이션 실행 시 엔티티 구조에 맞춰 테이블이 자동으로 생성 또는 업데이트됩니다.
- 별도의 SQL 스크립트 실행 없이도 초기 스키마 구성이 가능합니다.

## 4. 로컬 실행 방법 (Manual Run)

### Backend 실행
```bash
cd map-log-backend
./gradlew bootRun
```

### Frontend 실행
```bash
cd map-log-frontend
npm install
npm run dev
```

## 5. 전체 스택 실행 (Docker Compose)
데이터베이스를 포함한 전체 환경을 한 번에 실행하려면 프로젝트 루트에서 다음 명령어를 실행합니다.
```bash
docker compose up -d
```
- **Frontend:** http://localhost:80
- **Backend API:** http://localhost:8080
- **Database:** localhost:3306
