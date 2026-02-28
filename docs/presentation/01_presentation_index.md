1. 도입: 서비스 기획 배경 및 문제 정의 (2분)
    - 핵심 메시지: "나의 소중한 추억, 텍스트를 넘어 '공간'에 기록하다."
    - 발표 포인트:
        - Pain Point: 기존 SNS나 다이어리 앱은 '시간' 순 나열에는 강하지만, '내가 어디에 있었는지'에 대한 공간적 맥락을 직관적으로 보여주기 어렵다는 점 지적.
        - Solution: 지도를 인터페이스 전면에 내세워, 사용자의 발자취를 시각화하고 친구들과 공유할 수 있는 '위치 기반 소셜 다이어리' 제시.
2. 기술 스택 및 서비스 아키텍처 (3분)
    - 핵심 메시지: "안정성과 확장성을 고려한 Full-Stack & DevOps 환경."
    - 발표 포인트:
        - Frontend: Vue 3 (Composition API) + Pinia를 활용한 반응형 상태 관리.
        - Backend: Spring Boot 3.5 & Java 21 기반의 현대적 아키텍처 (CQRS 패턴 적용 - JPA/MyBatis 분리).
        - Infrastructure (중요): Docker/K8s 환경에서 Jenkins와 ArgoCD를 이용한 GitOps 기반의 자동 배포 파이프라인 강조.
3. 주요 기능 시연 (3분)
    - 핵심 메시지: "지도 위에서 펼쳐지는 직관적인 사용자 경험."
    - 발표 포인트:
        - Map View: Daum Postcode API 및 지도 라이브러리를 통한 장소 검색 및 마커 표시.
        - Diary Record: 사진 업로드 및 위치 정보를 결합한 기록 생성 과정.
        - Social & Notification: 실시간 친구 맺기 및 SSE(Server-Sent Events)를 활용한 실시간 알림 시스템.
4. 핵심 트러블슈팅: 기술적 도전과 해결 (5분)
    - 핵심 메시지: "문제 해결 과정에서 증명된 개발 역량."
    - 발표 포인트 (세부 사례):
        - 사례 1: 인증 시스템의 견고함 (Axios Interceptor)
            - 문제: Access Token 만료 시 사용자 경험 저하.
            - 해결: Axios Interceptor를 이용해 401 에러 발생 시 Refresh Token으로 자동 갱신 및 실패 요청 재시도 로직 구현.
        - 사례 2: 환경별 파일 저장 전략 (Profile Strategy)
            - 문제: 로컬 개발 환경과 클라우드 배포 환경의 저장소 차이.
            - 해결: @Profile 어노테이션을 활용해 로컬은 File System, 운영(AWS)은 S3를 사용하도록
            FileStorageService 구현체 동적 주입.
        - 사례 3: K8s 환경에서의 인그레스 및 권한 이슈
            - 문제: 쿠버네티스 배포 후 이미지 접근 시 401 권한 문제 발생.
            - 해결: Nginx Ingress 설정 및 Spring Security의 CORS/인증 필터 체인 디버깅을 통한 트래픽 흐름 최적화.
5. 마무리: 성과 및 향후 발전 방향 (2분)
    - 핵심 메시지: "실전 개발 프로세스를 경험하며 성장한 팀."
    - 발표 포인트:
        - 배운 점: CI/CD 파이프라인 구축을 통한 DevOps 문화 이해, 대규모 데이터 처리를 고려한 MyBatis/JPA 혼용 전략의 이점.
        - 개선 방향: 방문 장소 통계(데이터 시각화), 친구 간 실시간 위치 공유 기능(Websocket), 모바일 앱(PWA) 지원 확장.
        - 마지막 멘트: "단순한 기록을 넘어, 사용자의 삶의 지도를 완성해가는 MapLog입니다."