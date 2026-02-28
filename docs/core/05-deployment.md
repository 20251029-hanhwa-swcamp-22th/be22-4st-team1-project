# MapLog 배포 가이드

> **한 줄 요약**: MapLog는 Docker Compose (로컬), Kubernetes (프로덕션), 그리고 Jenkins CI + ArgoCD CD를 통해 자동화된 배포 파이프라인을 제공합니다.

## 목차 (Table of Contents)

- [배포 아키텍처](#배포-아키텍처)
- [Docker 배포](#docker-배포)
- [Docker Compose 배포](#docker-compose-배포)
- [Kubernetes 배포](#kubernetes-배포)
- [CI/CD 파이프라인](#cicd-파이프라인)
- [모니터링 및 로깅](#모니터링-및-로깅)

---

## 배포 아키텍처

### 개발 환경 (Docker Compose)

```
Internet
   ↓
Nginx (Port 80)
   ├─→ Frontend Service (Port 3000)
   └─→ Backend Service (Port 8080)
       └─→ MariaDB (Port 3306)
```

### 프로덕션 환경 (Kubernetes)

```
Internet
   ↓
Nginx Ingress Controller
   ├─→ Frontend Service (Pod replicas)
   └─→ Backend Service (Pod replicas)
       └─→ MariaDB StatefulSet (Persistent Volume)
           └─→ AWS S3 (Object Storage)
```

---

## Docker 배포

### 1. 백엔드 Dockerfile

**경로**: `map-log-backend/Dockerfile`

```dockerfile
# Build stage
FROM openjdk:21-jdk as builder

WORKDIR /app

# Gradle 래퍼 복사
COPY gradlew gradlew.bat ./
COPY gradle gradle

# 소스 코드 복사
COPY src src
COPY build.gradle settings.gradle ./

# 빌드 실행
RUN chmod +x ./gradlew && ./gradlew bootJar -x test

# Runtime stage
FROM openjdk:21-jdk-slim

WORKDIR /app

# JAR 파일 복사
COPY --from=builder /app/build/libs/application.jar app.jar

# 환경 변수 설정
ENV SPRING_PROFILES_ACTIVE=aws

# 포트 노출
EXPOSE 8080

# 헬스 체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 애플리케이션 실행
CMD ["java", "-jar", "app.jar"]
```

**멀티 스테이지 빌드 이점**:
- Build 스테이지에서 Gradle 이미지 사용 (크기 큼)
- Runtime 스테이지에서 slim 이미지 사용 (크기 작음)
- 최종 이미지 크기 약 50% 감소

### 2. 프론트엔드 Dockerfile

**경로**: `map-log-frontend/Dockerfile`

```dockerfile
# Build stage
FROM node:20-alpine as builder

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# Runtime stage
FROM nginx:alpine

# 빌드된 파일 복사
COPY --from=builder /app/dist /usr/share/nginx/html

# Nginx 설정 복사
COPY nginx.conf /etc/nginx/nginx.conf

# 포트 노출
EXPOSE 80

# 헬스 체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost/health || exit 1

CMD ["nginx", "-g", "daemon off;"]
```

### 3. Nginx 설정

**경로**: `map-log-frontend/nginx.conf`

```nginx
user nginx;
worker_processes auto;

events {
    worker_connections 1024;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    server {
        listen 80;
        server_name _;

        # 최대 업로드 파일 크기
        client_max_body_size 50M;

        # SPA routing
        location / {
            root /usr/share/nginx/html;
            try_files $uri $uri/ /index.html;
        }

        # API 프록시
        location /api {
            proxy_pass http://backend:8080;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;

            # SSE 설정
            proxy_buffering off;
            proxy_cache off;
            proxy_redirect off;

            # WebSocket 호환성
            proxy_http_version 1.1;
            proxy_set_header Connection "upgrade";
        }

        # 정적 파일
        location /uploads {
            proxy_pass http://backend:8080;
            proxy_set_header Host $host;
        }

        # 헬스 체크 엔드포인트
        location /health {
            access_log off;
            return 200 "healthy\n";
            add_header Content-Type text/plain;
        }
    }
}
```

---

## Docker Compose 배포

### 프로덕션 Docker Compose 설정

**경로**: `docker-compose.yml`

```yaml
version: '3.8'

services:
  mariadb:
    image: mariadb:11
    container_name: maplog-mariadb
    restart: unless-stopped
    ports:
      - "3306:3306"
    environment:
      MARIADB_ROOT_PASSWORD: ${DB_PASSWORD}
      MARIADB_DATABASE: maplog_dev
      MARIADB_CHARACTER_SET_SERVER: utf8mb4
      MARIADB_COLLATION_SERVER: utf8mb4_unicode_ci
    volumes:
      - maplog-mariadb-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "healthcheck.sh", "--connect", "--innodb_initialized"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - maplog-network

  backend:
    build:
      context: ./map-log-backend
      dockerfile: Dockerfile
    container_name: maplog-backend
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:mariadb://mariadb:3306/maplog_dev?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      # AWS S3 (선택)
      CLOUD_AWS_S3_BUCKET: ${AMAZON_S3_BUCKET_NAME}
      CLOUD_AWS_REGION_STATIC: ${AMAZON_S3_REGION}
      CLOUD_AWS_CREDENTIALS_ACCESS_KEY: ${AMAZON_S3_ACCESS_KEY}
      CLOUD_AWS_CREDENTIALS_SECRET_KEY: ${AMAZON_S3_SECRET_KEY}
    depends_on:
      mariadb:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
    networks:
      - maplog-network

  frontend:
    build:
      context: ./map-log-frontend
      dockerfile: Dockerfile
    container_name: maplog-frontend
    restart: unless-stopped
    ports:
      - "80:80"
    environment:
      VITE_API_BASE_URL: http://localhost:8080
    depends_on:
      - backend
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - maplog-network

volumes:
  maplog-mariadb-data:

networks:
  maplog-network:
    driver: bridge
```

### 이미지 빌드 및 실행

```bash
# 이미지 빌드
docker-compose build

# 서비스 시작
docker-compose up -d

# 로그 확인
docker-compose logs -f

# 서비스 상태 확인
docker-compose ps

# 서비스 종료
docker-compose down
```

---

## Kubernetes 배포

### Kubernetes 리소스 구조

```
k8s/
├── namespace.yaml           # Namespace 정의
├── configmap.yaml           # 환경 변수 저장소
├── secret.yaml              # 민감 정보 저장소
├── mariadb/
│   ├── statefulset.yaml     # DB 스테이트풀셋
│   └── service.yaml         # DB 서비스
├── backend/
│   ├── deployment.yaml      # 백엔드 배포
│   ├── service.yaml         # 백엔드 서비스
│   └── hpa.yaml             # 자동 스케일링
├── frontend/
│   ├── deployment.yaml      # 프론트엔드 배포
│   ├── service.yaml         # 프론트엔드 서비스
│   └── hpa.yaml             # 자동 스케일링
├── ingress.yaml             # Ingress 라우팅
└── pvc.yaml                 # 영구 볼륨
```

### 예시: Backend Deployment

```yaml
# k8s/backend/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: maplog-backend
  namespace: maplog
  labels:
    app: maplog-backend
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: maplog-backend
  template:
    metadata:
      labels:
        app: maplog-backend
    spec:
      serviceAccountName: maplog-backend
      containers:
      - name: maplog-backend
        image: docker.io/your-registry/maplog-backend:latest
        imagePullPolicy: Always
        ports:
        - containerPort: 8080
          name: http
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "aws"
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:mariadb://maplog-mariadb:3306/maplog?serverTimezone=Asia/Seoul&characterEncoding=UTF-8"
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: maplog-db-secret
              key: username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: maplog-db-secret
              key: password
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: maplog-jwt-secret
              key: secret
        - name: CLOUD_AWS_S3_BUCKET
          valueFrom:
            configMapKeyRef:
              name: maplog-config
              key: s3-bucket
        # AWS Credentials는 IAM Role 사용 (권장)
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 40
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 20
          periodSeconds: 5
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: maplog-backend
  namespace: maplog
spec:
  selector:
    app: maplog-backend
  type: ClusterIP
  ports:
  - port: 8080
    targetPort: 8080
    protocol: TCP
    name: http
```

### Ingress 설정

```yaml
# k8s/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: maplog-ingress
  namespace: maplog
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - maplog.example.com
    secretName: maplog-tls
  rules:
  - host: maplog.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: maplog-frontend
            port:
              number: 80
      - path: /api
        pathType: Prefix
        backend:
          service:
            name: maplog-backend
            port:
              number: 8080
```

### 배포 명령어

```bash
# 네임스페이스 생성
kubectl create namespace maplog

# 시크릿 생성
kubectl create secret generic maplog-db-secret \
  --from-literal=username=root \
  --from-literal=password=${DB_PASSWORD} \
  -n maplog

kubectl create secret generic maplog-jwt-secret \
  --from-literal=secret=${JWT_SECRET} \
  -n maplog

# 리소스 배포
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/mariadb/
kubectl apply -f k8s/backend/
kubectl apply -f k8s/frontend/
kubectl apply -f k8s/ingress.yaml

# 배포 상태 확인
kubectl get deployments -n maplog
kubectl get pods -n maplog
kubectl get svc -n maplog

# 로그 확인
kubectl logs -f deployment/maplog-backend -n maplog

# 포드 재시작
kubectl rollout restart deployment/maplog-backend -n maplog
```

---

## CI/CD 파이프라인

### Jenkins 파이프라인

**경로**: `Jenkinsfile`

```groovy
pipeline {
    agent any

    parameters {
        string(name: 'DOCKER_REGISTRY', defaultValue: 'docker.io', description: 'Docker Registry URL')
        string(name: 'IMAGE_TAG', defaultValue: 'latest', description: 'Docker Image Tag')
    }

    stages {
        stage('Checkout') {
            steps {
                git 'https://github.com/your-repo/be22-4st-team1-project.git'
            }
        }

        stage('Backend Build') {
            steps {
                dir('map-log-backend') {
                    sh './gradlew clean bootJar -x test'
                }
            }
        }

        stage('Frontend Build') {
            steps {
                dir('map-log-frontend') {
                    sh 'npm ci'
                    sh 'npm run build'
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                script {
                    sh 'docker build -t ${DOCKER_REGISTRY}/maplog-backend:${IMAGE_TAG} ./map-log-backend'
                    sh 'docker build -t ${DOCKER_REGISTRY}/maplog-frontend:${IMAGE_TAG} ./map-log-frontend'
                }
            }
        }

        stage('Push Docker Images') {
            steps {
                script {
                    withDockerRegistry(credentialsId: 'docker-hub-creds', url: "https://${DOCKER_REGISTRY}") {
                        sh 'docker push ${DOCKER_REGISTRY}/maplog-backend:${IMAGE_TAG}'
                        sh 'docker push ${DOCKER_REGISTRY}/maplog-frontend:${IMAGE_TAG}'
                    }
                }
            }
        }

        stage('Update K8s Manifest') {
            steps {
                script {
                    // GitOps 저장소에 이미지 태그 업데이트
                    git 'https://github.com/your-repo/maplog-k8s-manifests.git'

                    sh '''
                        sed -i "s|image:.*maplog-backend.*|image: ${DOCKER_REGISTRY}/maplog-backend:${IMAGE_TAG}|g" k8s/backend/deployment.yaml
                        sed -i "s|image:.*maplog-frontend.*|image: ${DOCKER_REGISTRY}/maplog-frontend:${IMAGE_TAG}|g" k8s/frontend/deployment.yaml
                    '''

                    sh '''
                        git config user.email "jenkins@example.com"
                        git config user.name "Jenkins"
                        git add .
                        git commit -m "chore: update image tag to ${IMAGE_TAG}"
                        git push
                    '''
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        failure {
            emailext(
                subject: "MapLog Build Failed: ${env.BUILD_NUMBER}",
                body: "Build Log: ${env.BUILD_URL}",
                to: "team@example.com"
            )
        }
    }
}
```

### ArgoCD 배포

ArgoCD가 GitOps 저장소를 감시하여 자동으로 K8s에 배포:

```bash
# ArgoCD Application 생성
argocd app create maplog-backend \
  --repo https://github.com/your-repo/maplog-k8s-manifests.git \
  --path k8s/backend \
  --dest-server https://kubernetes.default.svc \
  --dest-namespace maplog

# 배포 동기화
argocd app sync maplog-backend
```

---

## 모니터링 및 로깅

### 헬스 체크 엔드포인트

```
GET http://localhost:8080/actuator/health
```

**응답**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MariaDB",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP"
    }
  }
}
```

### 로깅 설정

**프로덕션 로깅 (application-aws.yml)**:

```yaml
logging:
  level:
    root: WARN
    com.maplog: INFO
    org.springframework.web: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: /var/log/maplog/application.log
    max-size: 10MB
    max-history: 30
```

### 컨테이너 로그 수집

Docker Compose 로그:
```bash
docker-compose logs -f --tail=100
```

Kubernetes 로그:
```bash
kubectl logs -f deployment/maplog-backend -n maplog
```

---

## 보안 고려사항

### 환경 변수 관리

`.env` 파일은 버전 관리 제외:

```bash
# .gitignore
.env
.env.local
*.key
*.pem
```

### 시크릿 관리 (Kubernetes)

```bash
# 시크릿 생성 (base64 인코딩)
kubectl create secret generic maplog-secret \
  --from-literal=db-password=${DB_PASSWORD} \
  --from-literal=jwt-secret=${JWT_SECRET} \
  -n maplog

# 시크릿 확인 (조회만, 값 숨김)
kubectl get secrets -n maplog
```

### SSL/TLS 설정

Let's Encrypt + cert-manager:

```yaml
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: admin@example.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
```

---

## 참고 자료

- [Docker 공식 문서](https://docs.docker.com/)
- [Docker Compose 설정](https://docs.docker.com/compose/compose-file/)
- [Kubernetes 공식 문서](https://kubernetes.io/docs/)
- [Jenkins 파이프라인](https://www.jenkins.io/doc/book/pipeline/)
- [ArgoCD 가이드](https://argo-cd.readthedocs.io/)

---

**다음 문서 참고:**
- [아키텍처 개요](./01-architecture-overview.md)
- [인증/보안 설계](./06-auth-security.md)
