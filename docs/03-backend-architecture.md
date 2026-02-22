# 03. Backend Architecture (백엔드 아키텍처)

MapLog 백엔드는 Spring Boot 3.5 기반으로 설계되었으며, 도메인 주도 설계(DDD)와 CQRS 패턴을 지향합니다.

## 1. 패키지 구조 및 패턴 (CQRS)
각 도메인은 명확한 역할 분리를 위해 **Command(쓰기)**와 **Query(읽기)** 패키지로 분리되어 있습니다.

```mermaid
graph LR
    subgraph "Domain Package"
        C[Command] -->|State Change| DB[(MariaDB)]
        Q[Query] -->|Read Only| DB
    end
    Controller --> C
    Controller --> Q
```

- **Command:** 비즈니스 로직 처리, 데이터 생성/수정/삭제를 담당합니다.
- **Query:** 복잡한 조회 로직, DTO 매핑, 성능 최적화(MyBatis 연동 등)를 담당합니다.
- **장점:** 읽기와 쓰기의 모델을 분리함으로써 시스템의 복잡도를 낮추고 각 요청의 특성에 맞는 최적화가 가능합니다.

## 2. 파일 저장 전략 (AWS S3)
이미지 첨부 기능을 위해 AWS S3 인프라를 활용하며, 보안을 위해 **Presigned URL** 방식을 채택했습니다.

### Presigned URL 워크플로우
1. **업로드:** 클라이언트가 파일을 전송하면 서버가 S3에 저장합니다.
2. **조회 요청:** 클라이언트가 이미지 URL을 요청합니다.
3. **URL 생성:** 서버는 AWS SDK를 사용하여 해당 객체에 접근할 수 있는 **임시 서명된 URL**을 생성합니다.
4. **유효 시간:** 보안을 위해 생성된 URL은 **1시간 동안만** 유효하며, 이후에는 접근이 차단됩니다.

## 3. 공통 인프라 레이어 (Common)
- **Security:** JWT 기반 인증/인가를 수행하며, `JwtAuthenticationFilter`가 모든 요청의 토큰을 검증합니다.
- **Exception Handling:** `GlobalExceptionHandler`를 통해 비즈니스 예외와 시스템 예외를 표준화된 `ApiResponse` 포맷으로 응답합니다.
- **Storage Service:** 인터페이스화를 통해 `dev` 프로필에서는 로컬 저장소를, `aws` 프로필에서는 S3 저장소를 사용하도록 유연하게 설계되었습니다.

## 4. DB 관리
별도의 데이터 마이그레이션 도구(Flyway 등) 대신 **JPA ddl-auto**를 활용하여 엔티티 모델과 데이터베이스 스키마의 일관성을 유지합니다.
- 개발 단계의 민첩성을 확보하기 위해 `update` 모드를 기본으로 사용합니다.
