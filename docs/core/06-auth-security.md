# MapLog 인증 및 보안 설계

> **한 줄 요약**: MapLog는 Spring Security + JWT 기반의 stateless 인증, BCrypt 비밀번호 암호화, 역할 기반 접근 제어(RBAC), 그리고 CORS 정책을 통해 안전한 인증/보안 체계를 구현합니다.

## 목차 (Table of Contents)

- [인증 개요](#인증-개요)
- [JWT 토큰 관리](#jwt-토큰-관리)
- [비밀번호 보안](#비밀번호-보안)
- [접근 제어 (RBAC)](#접근-제어-rbac)
- [CORS 정책](#cors-정책)
- [보안 취약점 및 방어](#보안-취약점-및-방어)
- [API 보안](#api-보안)

---

## 인증 개요

### Stateless 인증 방식

MapLog는 전통적인 세션 기반 인증이 아닌 **JWT (JSON Web Token) 기반 stateless 인증**을 사용합니다.

**Stateless의 이점**:
- 서버 세션 저장소 불필요 (확장성 증대)
- 마이크로서비스/멀티 서버 환경에 최적화
- 모바일/SPA 클라이언트와의 연동 용이

### 인증 흐름도

```
┌─────────────────┐
│   1. 로그인       │
│  (이메일/비밀번호)│
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────┐
│ 2. Backend 검증                      │
│  - 이메일 존재 여부                  │
│  - 비밀번호 BCrypt 비교             │
│  - 사용자 상태 확인 (ACTIVE 등)    │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│ 3. 토큰 생성                         │
│  - Access Token (30분, JWT HS256)  │
│  - Refresh Token (14일, JWT HS256) │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│ 4. Frontend 저장                     │
│  - localStorage에 두 토큰 저장      │
│  - Axios 인터셉터에서 자동 헤더 추가│
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│ 5. API 요청 (매 요청마다)           │
│  Authorization: Bearer {accessToken}│
└────────┬────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────┐
│ 6. JwtAuthenticationFilter 검증      │
│  - 토큰 유효성 확인                  │
│  - 만료 여부 확인                    │
│  - SecurityContext에 사용자 정보 설정│
└──────────────────────────────────────┘
```

---

## JWT 토큰 관리

### JWT 구조

JWT는 3개의 점(`.`)으로 구분된 부분으로 구성됩니다:

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.
eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNjc2NDAwMDAwLCJleHAiOjE2NzY0MDMwMDB9.
SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c

│─────────────── Header ────────────────┤ │─────────────────── Payload ────────────────────────────┤ │──────────────── Signature ──────────────────┤
```

**Header**:
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

**Payload**:
```json
{
  "sub": "user@example.com",
  "iat": 1676400000,
  "exp": 1676403600
}
```

**Signature**:
```
HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)
```

### 토큰 설정

**JwtTokenProvider.java**:

```java
@Component
public class JwtTokenProvider {
    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        // 시크릿은 최소 32자 (HS256 요구사항)
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;  // 1800000ms = 30분
        this.refreshTokenExpiration = refreshTokenExpiration;  // 1209600000ms = 14일
    }

    public String generateAccessToken(String subject) {
        return buildToken(subject, accessTokenExpiration);
    }

    public String generateRefreshToken(String subject) {
        return buildToken(subject, refreshTokenExpiration);
    }

    private String buildToken(String subject, long expiration) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)  // 사용자 이메일
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public void validateToken(String token) {
        try {
            parseClaims(token);
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

**application.yml**:

```yaml
jwt:
  secret: ${JWT_SECRET:dev-secret-key-must-be-at-least-32-characters-long}
  access-token-expiration: 1800000      # 30분 (ms)
  refresh-token-expiration: 1209600000  # 14일 (ms)
```

### JWT 시크릿 보안

**중요**: 시크릿은 절대 노출되면 안 됩니다.

```bash
# .gitignore
.env
.env.local
application-local.yml
```

**환경변수로 관리**:

```bash
# Docker 실행 시
docker run -e JWT_SECRET="your-secret-key-here" ...

# Kubernetes Secret
kubectl create secret generic maplog-jwt-secret \
  --from-literal=secret="your-secret-key-here"
```

### Access Token과 Refresh Token 분리

**Access Token (30분)**:
- 짧은 유효 기간으로 보안 강화
- 매 API 요청마다 검증
- 토큰 탈취 시에도 30분 후 자동 무효화

**Refresh Token (14일)**:
- 긴 유효 기간으로 사용자 편의성 제공
- Access Token 갱신에만 사용
- localStorage에 별도 저장
- 탈취 시 서버에서 폐기 가능

---

## 비밀번호 보안

### BCrypt 해싱

**AuthService.java**:

```java
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserCommandRepository userRepository;
    private final PasswordEncoder passwordEncoder;  // Spring Security BCryptPasswordEncoder
    private final JwtTokenProvider jwtTokenProvider;

    public void signup(SignupRequest request) {
        // 1. 중복 검사
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 2. 비밀번호 해싱
        String encodedPassword = passwordEncoder.encode(request.password());

        // 3. 사용자 생성
        User user = User.create(
            request.email(),
            encodedPassword,
            request.nickname()
        );

        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 비밀번호 검증 (BCrypt)
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        // 사용자 상태 확인
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        return new LoginResponse(accessToken, refreshToken);
    }
}
```

**Spring Security Config**:

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCryptPasswordEncoder (기본값: strength = 10)
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            // CSRF 비활성화 (JWT 사용하므로 불필요)
            .csrf(csrf -> csrf.disable())
            // JWT 필터 추가
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            // 접근 제어
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/users/check-nickname").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            // 예외 처리
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new RestAuthenticationEntryPoint())
                .accessDeniedHandler(new RestAccessDeniedHandler())
            )
            .build();
    }
}
```

### BCrypt 강도

```
BCrypt Round 10 (기본값)
- 비밀번호 "password123" 해싱 시간: 약 0.1초
- 환경: Intel Core i7-9700K @ 3.6GHz

Round가 높을수록:
- 보안: ↑
- 성능: ↓
```

**강도 조정**:

```java
new BCryptPasswordEncoder(12)  // 더 강력 (0.2-0.3초)
new BCryptPasswordEncoder(10)  // 기본 (0.1초)
new BCryptPasswordEncoder(8)   // 낮음 (빠름, 권장 안 함)
```

---

## 접근 제어 (RBAC)

### 역할 정의

**Role.java (Enum)**:

```java
public enum Role {
    USER,    // 일반 사용자
    ADMIN    // 관리자
}
```

### 역할 기반 권한 검사

**어노테이션 기반**:

```java
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")  // 관리자만 접근
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> getUsers(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllUsers(pageable)));
    }
}
```

**프로그래매틱 방식**:

```java
@Service
@RequiredArgsConstructor
public class DiaryCommandService {

    public void deleteDiary(String email, Long diaryId) {
        User requestingUser = userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Diary diary = diaryRepository.findById(diaryId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_NOT_FOUND));

        // 작성자 본인 또는 관리자만 삭제 가능
        if (!diary.isOwner(requestingUser.getId()) && !requestingUser.getRole().equals(Role.ADMIN)) {
            throw new BusinessException(ErrorCode.DIARY_ACCESS_DENIED);
        }

        diary.softDelete();
        diaryRepository.save(diary);
    }
}
```

### 리소스 기반 접근 제어

**일기 공개 범위**:

```java
public class DiaryQueryService {

    public DiaryDetailResponse getDiaryDetail(String email, Long diaryId) {
        User requestingUser = getUser(email);
        Diary diary = getDiary(diaryId);

        // 접근 가능 여부 확인
        if (!canAccess(requestingUser, diary)) {
            throw new BusinessException(ErrorCode.DIARY_ACCESS_DENIED);
        }

        return diaryQueryMapper.findDiaryDetail(diaryId, requestingUser.getId());
    }

    private boolean canAccess(User requestingUser, Diary diary) {
        // 본인 일기
        if (diary.isOwner(requestingUser.getId())) {
            return true;
        }

        // 관리자
        if (requestingUser.getRole() == Role.ADMIN) {
            return true;
        }

        // 공개 범위에 따라 결정
        return switch (diary.getVisibility()) {
            case PUBLIC -> true;
            case PRIVATE -> false;
            case FRIENDS_ONLY -> isFriend(requestingUser.getId(), diary.getUserId());
        };
    }

    private boolean isFriend(Long userId1, Long userId2) {
        return friendRepository.findBySender_ReceiverAndStatus(userId1, userId2, FriendStatus.ACCEPTED)
            .isPresent();
    }
}
```

---

## CORS 정책

### CORS 설정

**WebConfig.java**:

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:5173", "http://localhost:3000", "https://maplog.example.com")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders("Authorization", "Content-Type")
            .allowCredentials(true)
            .maxAge(3600);  // 1시간 프리플라이트 캐시
    }
}
```

### 프리플라이트 요청

```
OPTIONS /api/diaries
Origin: http://localhost:5173
Access-Control-Request-Method: POST
Access-Control-Request-Headers: content-type, authorization

---

HTTP/1.1 200 OK
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Access-Control-Allow-Headers: content-type, authorization
Access-Control-Max-Age: 3600
```

---

## 보안 취약점 및 방어

### 1. SQL Injection

**취약점 코드**:

```java
// 위험!
String query = "SELECT * FROM users WHERE email = '" + email + "'";
```

**방어 코드** (Prepared Statement):

```java
// Spring Data JPA 사용
User user = userRepository.findByEmail(email);

// MyBatis Parameterized Query
<select id="findByEmail" parameterType="string" resultType="User">
    SELECT * FROM users WHERE email = #{email}
</select>
```

### 2. XSS (Cross-Site Scripting)

**취약점 코드**:

```java
// 위험!
model.addAttribute("content", userInput);  // HTML 이스케이프 없음
```

**방어 코드**:

```java
// Jackson 자동 이스케이프 (JSON 응답)
return new ApiResponse<>("SUCCESS", message, dto);

// Vue 템플릿 자동 이스케이프
<div>{{ userInput }}</div>  <!-- HTML 이스케이프 -->

// 명시적 이스케이프
<div v-html="sanitizedContent"></div>  <!-- DomPurify 사용 권장 -->
```

### 3. CSRF (Cross-Site Request Forgery)

**방어**:

```java
// Spring Security CSRF 토큰 (폼 기반)
// JWT 사용 시 CSRF 불필요 (stateless)
.csrf(csrf -> csrf.disable())
```

### 4. Rate Limiting

**Spring Cloud Gateway 설정** (선택사항):

```yaml
spring:
  cloud:
    gateway:
      routes:
      - id: api
        uri: http://localhost:8080
        predicates:
        - Path=/api/**
        filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter:
              replenishRate: 100  # 초당 100 요청
              burstCapacity: 200  # 버스트 200 요청
```

### 5. 비밀번호 정책

**요구사항**:

```
- 최소 8자
- 영문, 숫자, 특수문자 혼합 (권장)
- 만료 정책 없음 (NIST 최신 권고)
```

**검증 로직**:

```java
@Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).{8,}$",
         message = "비밀번호는 최소 8자 이상이며 영문과 숫자를 포함해야 합니다.")
private String password;
```

### 6. SSL/TLS

**프로덕션 Nginx 설정**:

```nginx
server {
    listen 443 ssl http2;
    ssl_certificate /etc/nginx/certs/cert.pem;
    ssl_certificate_key /etc/nginx/certs/key.pem;

    # TLS 1.2 이상만 허용
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # HTTP 자동 HTTPS 리다이렉트
    if ($scheme != "https") {
        return 301 https://$server_name$request_uri;
    }
}
```

---

## API 보안

### 입력 검증

```java
@PostMapping("/auth/signup")
public ResponseEntity<ApiResponse<Void>> signup(
        @Valid @RequestBody SignupRequest request) {
    authService.signup(request);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("회원가입이 완료되었습니다.", null));
}
```

**SignupRequest.java**:

```java
public record SignupRequest(
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    String email,

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).{8,}$",
             message = "비밀번호는 최소 8자 이상이며 영문과 숫자를 포함해야 합니다.")
    String password,

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 20, message = "닉네임은 2~20자입니다.")
    @Pattern(regexp = "^[a-zA-Z0-9가-힣_]*$",
             message = "닉네임은 영문, 한글, 숫자, 언더스코어만 포함 가능합니다.")
    String nickname
) {}
```

### 보안 헤더

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                                    Object handler) throws Exception {
                response.setHeader("X-Content-Type-Options", "nosniff");
                response.setHeader("X-Frame-Options", "DENY");
                response.setHeader("X-XSS-Protection", "1; mode=block");
                response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
                return true;
            }
        });
    }
}
```

---

## 감사 로깅 (Audit Logging)

**주요 이벤트 로깅**:

```java
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(AuditLoggingAspect.class);

    @Before("@annotation(com.maplog.common.annotation.Auditable)")
    public void logAuditEvent(JoinPoint joinPoint) {
        String userEmail = SecurityContextHolder.getContext()
            .getAuthentication()
            .getName();

        logger.info("AUDIT: User={}, Action={}, Timestamp={}",
            userEmail,
            joinPoint.getSignature().getName(),
            LocalDateTime.now());
    }
}
```

**로깅 대상**:
- 로그인/로그아웃
- 일기 생성/수정/삭제
- 친구 요청/수락/거절
- 관리자 권한 변경

---

## 참고 자료

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security 공식 문서](https://spring.io/projects/spring-security)
- [JWT.io](https://jwt.io/)
- [NIST 비밀번호 권고](https://pages.nist.gov/800-63-3/sp800-63b.html)
- [CORS 명세](https://www.w3.org/TR/cors/)

---

**다음 문서 참고:**
- [아키텍처 개요](./01-architecture-overview.md)
- [API 설계 문서](./02-api-design.md)
- [개발 환경 설정](./04-development-setup.md)
