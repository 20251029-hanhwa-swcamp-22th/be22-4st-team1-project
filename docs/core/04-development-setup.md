# MapLog 개발 환경 설정 가이드

> **한 줄 요약**: MapLog 로컬 개발을 위해 Java 21, Docker, Node.js를 설치하고 환경 변수를 설정한 후, Docker Compose로 MariaDB를 실행하고 백엔드/프론트엔드 서버를 각각 실행합니다.

## 목차 (Table of Contents)

- [사전 요구사항](#사전-요구사항)
- [설치 단계](#설치-단계)
- [프로젝트 실행](#프로젝트-실행)
- [개발 기본 명령어](#개발-기본-명령어)
- [문제 해결 (Troubleshooting)](#문제-해결-troubleshooting)
- [IDE 설정](#ide-설정)

---

## 사전 요구사항

### 필수 소프트웨어

| 항목 | 최소 버전 | 권장 버전 | 용도 |
|------|----------|----------|------|
| Java | 17 | 21 | Spring Boot 3.5 |
| Node.js | 16 | 20 LTS | Vue 3, npm |
| Docker | 20.10 | 최신 | MariaDB 컨테이너 |
| Docker Compose | 1.29 | 2.0+ | 다중 컨테이너 관리 |
| Git | 2.20 | 최신 | 버전 관리 |

### 선택사항

- **IDE**: IntelliJ IDEA (Backend), VS Code (Frontend)
- **AWS 계정** (S3 파일 업로드 테스트용)
- **Postman/Thunder Client** (API 테스트)

---

## 설치 단계

### 1단계: Java 21 설치

#### Windows (Chocolatey)

```bash
choco install openjdk21
java -version
```

**출력 예**:
```
openjdk version "21.0.1" 2023-10-17
OpenJDK Runtime Environment (build 21.0.1+12-39)
```

#### macOS (Homebrew)

```bash
brew install openjdk@21
# 심볼릭 링크 설정
sudo ln -sfn /usr/local/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk

java -version
```

#### Ubuntu/Debian

```bash
sudo apt update
sudo apt install openjdk-21-jdk
java -version
```

### 2단계: Node.js 설치

#### Windows (Node.js 공식 설치관리자)

https://nodejs.org 에서 20 LTS 버전 다운로드 후 설치

```bash
node --version
npm --version
```

#### macOS (Homebrew)

```bash
brew install node@20
node --version
npm --version
```

#### Ubuntu/Debian

```bash
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs
node --version
npm --version
```

### 3단계: Docker 설치

#### Windows

https://www.docker.com/products/docker-desktop 에서 Docker Desktop 설치

```bash
docker --version
docker-compose --version
```

#### macOS

```bash
brew install docker docker-compose
# 또는 Docker Desktop 설치
docker --version
docker-compose --version
```

#### Ubuntu

```bash
sudo apt update
sudo apt install docker.io docker-compose
sudo usermod -aG docker $USER
newgrp docker
docker --version
docker-compose --version
```

### 4단계: Git 설치

각 OS별로 Git을 설치합니다.

```bash
git --version
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"
```

---

## 프로젝트 설정

### 프로젝트 클론

```bash
git clone https://github.com/20251029-hanhwa-swcamp-22th/be22-4st-team1-project.git
cd be22-4st-team1-project
```

### 환경 변수 설정

프로젝트 루트에 `.env` 파일 생성:

```bash
cp .env.example .env
```

`.env` 파일 내용 작성:

```bash
# 데이터베이스
DB_PASSWORD=your_mariadb_password

# JWT (최소 32자)
JWT_SECRET=your_very_long_jwt_secret_at_least_32_characters_long_here

# AWS S3 (운영 환경에서만 필요)
AMAZON_S3_ACCESS_KEY=
AMAZON_S3_SECRET_KEY=
AMAZON_S3_BUCKET_NAME=
AMAZON_S3_REGION=
```

**JWT_SECRET 생성 (Linux/macOS)**:

```bash
openssl rand -base64 32
```

**JWT_SECRET 생성 (Windows PowerShell)**:

```powershell
[Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes((1..32 | ForEach-Object { [char](Get-Random -Minimum 65 -Maximum 122) } | Join-String)))
```

### 프론트엔드 환경 설정

`map-log-frontend/.env.local` 파일 생성:

```bash
VITE_API_BASE_URL=http://localhost:8080
```

---

## 프로젝트 실행

### 방법 1: Docker Compose를 이용한 완전 통합 실행

**모든 서비스 시작**:

```bash
docker-compose up -d
```

**로그 확인**:

```bash
# 전체 로그
docker-compose logs -f

# 특정 서비스 로그
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f mariadb
```

**모든 서비스 종료**:

```bash
docker-compose down
```

**데이터베이스 초기화** (모든 데이터 삭제):

```bash
docker-compose down -v
```

---

### 방법 2: 로컬 개발 환경 (권장)

각 서비스를 별도의 터미널에서 실행합니다.

#### 터미널 1: MariaDB 실행

```bash
docker-compose up -d mariadb
```

**연결 확인**:

```bash
docker-compose exec mariadb mariadb -uroot -p${DB_PASSWORD} -e "SHOW DATABASES;"
```

---

#### 터미널 2: 백엔드 실행

```bash
cd map-log-backend
./gradlew bootRun
```

**Windows PowerShell**:

```bash
cd map-log-backend
./gradlew.bat bootRun
```

**로그 확인**:

```
Started Be224st1teamProjectApplication in X.XXX seconds
```

**API 엔드포인트**: http://localhost:8080

---

#### 터미널 3: 프론트엔드 실행

```bash
cd map-log-frontend
npm install
npm run dev
```

**출력**:

```
VITE v5.x.x  ready in XXX ms

➜  Local:   http://localhost:5173/
```

**웹브라우저**: http://localhost:5173

---

## 개발 기본 명령어

### 백엔드

```bash
cd map-log-backend

# 빌드 (테스트 포함)
./gradlew build

# 빌드 (테스트 제외)
./gradlew bootJar -x test

# 테스트 실행
./gradlew test

# 특정 도메인 테스트
./gradlew test --tests "com.maplog.diary.*"

# 특정 테스트 클래스
./gradlew test --tests "com.maplog.diary.command.service.DiaryCommandServiceTest"

# 코드 정적 분석
./gradlew checkstyleMain

# 의존성 확인
./gradlew dependencies

# IDE 설정 생성 (IntelliJ)
./gradlew idea
```

### 프론트엔드

```bash
cd map-log-frontend

# 개발 서버 실행
npm run dev

# 프로덕션 빌드
npm run build

# 빌드된 파일 미리보기
npm run preview

# 단위 테스트 실행
npm test

# 테스트 커버리지
npm run test:coverage

# 린트 확인
npm run lint

# 린트 자동 수정
npm run lint:fix
```

---

## 데이터베이스 관리

### MariaDB 접속

```bash
docker-compose exec mariadb mariadb -uroot -p${DB_PASSWORD}
```

또는 GUI 클라이언트 (DBeaver, MySQL Workbench) 사용:

```
Host: localhost
Port: 3306
User: root
Password: ${DB_PASSWORD}
Database: maplog_dev
```

### 데이터베이스 초기화

```bash
# 컨테이너 중지
docker-compose stop mariadb

# 데이터 볼륨 삭제
docker-compose down -v

# 다시 시작 (자동으로 데이터베이스 재생성)
docker-compose up -d mariadb
```

### SQL 쿼리 실행

```bash
docker-compose exec mariadb mariadb -uroot -p${DB_PASSWORD} maplog_dev < query.sql
```

---

## IDE 설정

### IntelliJ IDEA (Backend)

1. **프로젝트 열기**:
   - File → Open
   - `map-log-backend` 폴더 선택

2. **JDK 설정**:
   - Settings → Project Structure → Project
   - SDK: Java 21 선택

3. **Gradle 설정**:
   - Settings → Build, Execution, Deployment → Build Tools → Gradle
   - Gradle JVM: Java 21

4. **Spring Boot 실행 구성**:
   - Run → Edit Configurations
   - + → Spring Boot
   - Main class: `com.maplog.Be224st1teamProjectApplication`
   - Environment variables: `SPRING_PROFILES_ACTIVE=dev`

5. **플러그인 설치** (권장):
   - Lombok
   - Spring Boot Assistant
   - MyBatis

---

### VS Code (Frontend)

1. **확장 프로그램 설치**:
   - Volar (Vue 3 official extension)
   - TypeScript Vue Plugin
   - Prettier
   - ESLint
   - Thunder Client (API 테스트)

2. **작업공간 설정** (`.vscode/settings.json`):

```json
{
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "editor.formatOnSave": true,
  "[javascript]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  },
  "vetur.validation.template": true,
  "volar.enable": true
}
```

3. **디버깅**:
   - Debug → Add Configuration
   - Chrome 디버거로 Vue 앱 디버깅 가능

---

## 문제 해결 (Troubleshooting)

### Java 버전 문제

**증상**: `error: invalid source release 21`

**해결**:
```bash
java -version  # Java 21 확인
echo $JAVA_HOME
```

**Windows에서 JAVA_HOME 설정**:
```bash
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-21.0.1+12"
```

---

### 포트 이미 사용 중

**증상**: `Address already in use :8080`

**해결** (Windows):
```powershell
# 포트 8080을 사용하는 프로세스 찾기
netstat -ano | findstr :8080

# PID로 프로세스 종료
taskkill /PID <PID> /F
```

**해결** (Linux/macOS):
```bash
lsof -i :8080
kill -9 <PID>
```

---

### MariaDB 연결 실패

**증상**: `Connection refused :3306`

**해결**:
```bash
# 컨테이너 실행 확인
docker-compose ps

# 컨테이너 로그 확인
docker-compose logs mariadb

# 컨테이너 재시작
docker-compose restart mariadb
```

---

### 이미지 업로드 실패

**증상**: `413 Payload Too Large`

**원인**: Nginx 기본 `client_max_body_size` 제한

**해결** (Docker Compose에서 실행 시):
- `map-log-frontend/nginx.conf`에서 설정됨
- 로컬 개발 시는 크기 제한 없음

---

### npm install 속도 느림

**해결** (NPM 캐시 초기화):
```bash
npm cache clean --force
npm install
```

**또는 yarn 사용**:
```bash
npm install -g yarn
yarn install
yarn dev
```

---

### Gradle 빌드 실패

**해결** (Gradle 캐시 초기화):
```bash
./gradlew clean build --no-build-cache
```

---

### SSE 연결 실패 (개발 시)

**증상**: EventSource 3초 후 자동 재연결 반복

**원인**: CORS 설정, JWT 토큰 문제

**해결**:

1. 브라우저 개발자 도구 → Network 탭에서 `/api/sse/subscribe` 요청 확인
2. Response 헤더에 `Content-Type: text/event-stream` 확인
3. 쿼리 파라미터 `?token=...` 에 유효한 JWT 포함 확인

---

## 개발 팁

### Hot Reload 활성화

**백엔드** (Spring Boot):
```bash
./gradlew bootRun --args='--spring.devtools.restart.enabled=true'
```

**프론트엔드** (Vite):
```bash
npm run dev  # 자동으로 핫 리로드 지원
```

---

### 데이터베이스 쿼리 로깅

`application-dev.yml` 이미 설정됨:

```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.orm.jdbc.bind: TRACE
```

### API 테스트

Postman/Thunder Client에서 컬렉션 import:

```bash
# 프로젝트 루트에서 제공되는 포스트맨 컬렉션 (있다면)
docs/postman/MapLog-API.postman_collection.json
```

---

## 개발 워크플로우 예시

### 새 기능 개발 절차

1. **브랜치 생성**:
   ```bash
   git checkout -b feature/new-feature
   ```

2. **백엔드 개발**:
   ```bash
   cd map-log-backend
   ./gradlew bootRun
   # 코드 수정 후 자동 리로드
   ```

3. **프론트엔드 개발**:
   ```bash
   cd map-log-frontend
   npm run dev
   # 코드 수정 후 자동 리로드
   ```

4. **테스트**:
   ```bash
   cd map-log-backend
   ./gradlew test

   cd map-log-frontend
   npm test
   ```

5. **커밋 및 푸시**:
   ```bash
   git add .
   git commit -m "feat: add new feature"
   git push origin feature/new-feature
   ```

6. **Pull Request 생성**:
   - GitHub에서 main 브랜치로 PR 생성

---

## 참고 자료

- [Java 21 공식 문서](https://docs.oracle.com/en/java/javase/21/)
- [Spring Boot 3.5 설정](https://spring.io/projects/spring-boot)
- [Vue 3 개발 가이드](https://vuejs.org/guide/quick-start.html)
- [Gradle 가이드](https://gradle.org/guides/)
- [Docker Compose 문서](https://docs.docker.com/compose/)
- [MariaDB 공식 문서](https://mariadb.com/docs/)

---

**다음 문서 참고:**
- [아키텍처 개요](./01-architecture-overview.md)
- [배포 가이드](./05-deployment.md)
- [API 설계 문서](./02-api-design.md)
