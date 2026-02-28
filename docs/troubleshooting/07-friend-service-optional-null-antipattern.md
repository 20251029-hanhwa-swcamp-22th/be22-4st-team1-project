# FriendCommandService orElse(null) 안티패턴으로 인한 잠재적 NPE

## 증상

현재 코드에서는 실제 `NullPointerException`이 발생하지 않으나, 잠재적인 결함 위험 존재.

**문제 상황:**
- `Optional<Friend>` 반환값을 `.orElse(null)`로 변환하여 Optional의 null-safe 보장이 사라짐
- `Friend existingFriend = ...` 선언만 보면 null 가능성을 타입으로 알 수 없음
- 같은 클래스의 다른 메서드들은 모두 `.orElseThrow()`를 사용하는데 이 메서드만 패턴이 불일치
- 향후 코드 유지보수 시 null 체크 누락으로 인한 NPE 발생 가능성 높음

---

## 원인 분석

### 에러 발생 시나리오

```
FriendCommandService.requestFriend() 메서드
  → friendCommandRepository.findByUsers(requesterId, receiverId)
  → Optional<Friend> 반환
      ↓
.orElse(null) 호출
  → null이 가능한 Friend 변수로 변환
      ↓
Friend existingFriend = ...
  → 타입 시스템에서 null 위험이 드러나지 않음
      ↓
if (existingFriend != null) { ... }
  → 현재는 null 체크가 있어서 작동함
      ↓
미래 개발자가 if 블록 밖에서 existingFriend 참조 추가
  → existingFriend.getStatus()  // 실수로 추가
      ↓
NullPointerException 발생
```

### 핵심 문제 3가지

**문제 1. Optional의 의도 소실**

`Optional<T>`는 "값이 없을 수도 있다"는 의도를 **타입 시스템으로 표현**하는 디자인.

```java
// Optional 사용 의도
Optional<Friend> existingFriendOpt = repository.findByUsers(...)
// 읽는 사람: "아, 없을 수도 있는데?"

// orElse(null) 사용 후
Friend existingFriend = repository.findByUsers(...).orElse(null)
// 읽는 사람: "Friend 객체가 있는 건가?" (null 가능성이 숨겨짐)
```

**문제 2. 변수 선언만으로 null 가능성을 알 수 없음**

```java
// 변경 전 - null 가능성이 코드에 보이지 않음
Friend existingFriend = friendCommandRepository.findByUsers(...).orElse(null);

// 변경 후 - 타입에서 null 가능성이 명시됨
Optional<Friend> existingFriendOpt = friendCommandRepository.findByUsers(...);
```

IDE의 인텔리센스나 코드 리뷰에서도 null 가능성이 드러나지 않음.

**문제 3. 클래스 내 메서드 간 패턴 불일치**

같은 `FriendCommandService` 클래스의 다른 메서드들:

```java
// respondToRequest() 메서드
Friend friend = friendCommandRepository.findById(friendId)
    .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_NOT_FOUND));
// → Optional 패턴 유지, 예외 처리

// deleteFriend() 메서드
Friend friend = friendCommandRepository.findById(friendId)
    .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_NOT_FOUND));
// → Optional 패턴 유지, 예외 처리

// requestFriend() 메서드 (문제 코드)
Friend existingFriend = friendCommandRepository.findByUsers(...).orElse(null);
// → Optional 패턴 깨짐, null 체크 필요
```

일관성이 없으면 코드 리뷰어도 놓치기 쉽고, 유지보수 시 실수할 확률이 높아짐.

---

## 해결 방법

### 변경 전 (안티패턴 코드)

**`map-log-backend/src/main/java/com/maplog/friend/command/service/FriendCommandService.java`**

```java
// import가 Optional을 포함하지 않음
import org.springframework.stereotype.Service;
import com.maplog.common.exception.BusinessException;
import com.maplog.friend.command.domain.Friend;
import com.maplog.friend.command.domain.FriendStatus;
import com.maplog.friend.command.repository.FriendCommandRepository;

@Service
public class FriendCommandService {
    private final FriendCommandRepository friendCommandRepository;

    public void requestFriend(String requesterId, String receiverId) {
        Friend existingFriend = friendCommandRepository.findByUsers(requesterId, receiverId).orElse(null);

        if (existingFriend != null) {
            if (existingFriend.isPending()) {
                throw new BusinessException(ErrorCode.ALREADY_FRIEND_REQUESTED);
            }
            if (existingFriend.getStatus() == FriendStatus.ACCEPTED) {
                throw new BusinessException(ErrorCode.ALREADY_FRIEND);
            }
            existingFriend.reRequest(requesterId, receiverId);
            friendCommandRepository.save(existingFriend);
            return;
        }

        // 새로운 친구 관계 생성
        Friend newFriend = Friend.createRequest(requesterId, receiverId);
        friendCommandRepository.save(newFriend);
    }

    public void respondToRequest(String friendId, boolean accepted) {
        Friend friend = friendCommandRepository.findById(friendId)
            .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_NOT_FOUND));

        if (accepted) {
            friend.accept();
        } else {
            friend.reject();
        }
        friendCommandRepository.save(friend);
    }

    public void deleteFriend(String friendId) {
        Friend friend = friendCommandRepository.findById(friendId)
            .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_NOT_FOUND));
        friendCommandRepository.delete(friend);
    }
}
```

**문제점:**
- `requestFriend()` 메서드만 `.orElse(null)` 사용
- 다른 메서드는 `.orElseThrow()` 사용
- null 가능성이 타입에 드러나지 않음
- IDE의 null-check 경고 기능이 작동하지 않음

### 변경 후 (안전한 코드)

**`map-log-backend/src/main/java/com/maplog/friend/command/service/FriendCommandService.java`**

```java
// java.util.Optional 명시적으로 import
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.maplog.common.exception.BusinessException;
import com.maplog.friend.command.domain.Friend;
import com.maplog.friend.command.domain.FriendStatus;
import com.maplog.friend.command.repository.FriendCommandRepository;

@Service
public class FriendCommandService {
    private final FriendCommandRepository friendCommandRepository;

    public void requestFriend(String requesterId, String receiverId) {
        // Optional 타입을 유지하여 null 가능성을 명시적으로 표현
        Optional<Friend> existingFriendOpt = friendCommandRepository.findByUsers(requesterId, receiverId);

        if (existingFriendOpt.isPresent()) {
            Friend existingFriend = existingFriendOpt.get();

            if (existingFriend.isPending()) {
                throw new BusinessException(ErrorCode.ALREADY_FRIEND_REQUESTED);
            }
            if (existingFriend.getStatus() == FriendStatus.ACCEPTED) {
                throw new BusinessException(ErrorCode.ALREADY_FRIEND);
            }
            existingFriend.reRequest(requesterId, receiverId);
            friendCommandRepository.save(existingFriend);
            return;
        }

        // 새로운 친구 관계 생성
        Friend newFriend = Friend.createRequest(requesterId, receiverId);
        friendCommandRepository.save(newFriend);
    }

    public void respondToRequest(String friendId, boolean accepted) {
        Friend friend = friendCommandRepository.findById(friendId)
            .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_NOT_FOUND));

        if (accepted) {
            friend.accept();
        } else {
            friend.reject();
        }
        friendCommandRepository.save(friend);
    }

    public void deleteFriend(String friendId) {
        Friend friend = friendCommandRepository.findById(friendId)
            .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_NOT_FOUND));
        friendCommandRepository.delete(friend);
    }
}
```

**개선 사항:**
- `Optional` 타입을 명시적으로 유지
- `isPresent()` 블록 내에서만 `.get()` 호출 → 안전성 보장
- 모든 메서드에서 Optional 패턴 일관성 확보
- IDE의 null-check 경고 시스템이 정상 작동
- 미래 개발자도 null 가능성을 타입에서 바로 인지

---

## 구현 상세

### 변경 로직 설명

**변경 전:**
```java
Friend existingFriend = ... .orElse(null);
if (existingFriend != null) {
    // null 체크가 필요하지만, 타입에는 그 이유가 드러나지 않음
}
```

**변경 후:**
```java
Optional<Friend> existingFriendOpt = ... ;
if (existingFriendOpt.isPresent()) {
    Friend existingFriend = existingFriendOpt.get();
    // Optional 타입이 null 가능성을 명시적으로 표현
    // isPresent() 블록 내에서만 get() 호출 → 안전성 보장
}
```

### 대체 Optional 메서드

상황에 따라 다른 Optional 메서드 사용 가능:

```java
// 1. 존재 시에만 처리 (현재 방식)
optional.ifPresent(friend -> { ... });

// 2. 존재/미존재 둘 다 처리
optional.ifPresentOrElse(
    friend -> { ... },  // 존재
    () -> { ... }       // 미존재
);

// 3. 변환 후 처리 (map)
optional.map(Friend::getStatus)
    .ifPresent(status -> { ... });

// 4. 조건부 필터링
optional.filter(friend -> friend.isPending())
    .ifPresent(friend -> { ... });
```

현재 코드는 **존재 시에만 처리**하므로 `ifPresent()` 메서드도 고려 가능:

```java
// 변경 후 - ifPresent() 사용 버전
friendCommandRepository.findByUsers(requesterId, receiverId).ifPresent(existingFriend -> {
    if (existingFriend.isPending()) {
        throw new BusinessException(ErrorCode.ALREADY_FRIEND_REQUESTED);
    }
    if (existingFriend.getStatus() == FriendStatus.ACCEPTED) {
        throw new BusinessException(ErrorCode.ALREADY_FRIEND);
    }
    existingFriend.reRequest(requesterId, receiverId);
    friendCommandRepository.save(existingFriend);
});

// 새로운 친구 관계 생성 (findByUsers 결과가 비어있을 때만 실행)
if (!friendCommandRepository.findByUsers(requesterId, receiverId).isPresent()) {
    Friend newFriend = Friend.createRequest(requesterId, receiverId);
    friendCommandRepository.save(newFriend);
}
```

다만 로직이 복잡해지므로, 제시된 **`isPresent() + get()` 방식**이 가독성이 더 우수함.

---

## Optional Best Practice

### Optional 사용 규칙

| 상황 | 권장 방법 | 이유 |
|---|---|---|
| 값이 없으면 예외 발생 | `.orElseThrow()` | 호출자에게 예외 전파 |
| 값이 없으면 기본값 사용 | `.orElse(defaultValue)` | null이 아닌 안전한 기본값 제공 |
| 값이 있을 때만 처리 | `.ifPresent()` 또는 `if (opt.isPresent())` | null 체크 필요 없음 |
| 값이 없으면 null 허용 | ~~`.orElse(null)`~~ (금지) | Optional을 사용하는 의미 상실 |

### 안티패턴 목록

```java
// ❌ 안티패턴 1: orElse(null)
Optional<Friend> opt = ...;
Friend friend = opt.orElse(null);  // Optional을 사용하는 의미 없음

// ❌ 안티패턴 2: orElseThrow()에 null 전달
Optional<Friend> opt = ...;
Friend friend = opt.orElseThrow(null);  // NullPointerException 발생

// ❌ 안티패턴 3: .get() 호출 전 isPresent() 없음
Optional<Friend> opt = ...;
Friend friend = opt.get();  // NoSuchElementException 위험

// ✅ 올바른 패턴 1
Optional<Friend> opt = ...;
if (opt.isPresent()) {
    Friend friend = opt.get();
    // 안전하게 사용
}

// ✅ 올바른 패턴 2
Optional<Friend> opt = ...;
opt.ifPresent(friend -> { ... });

// ✅ 올바른 패턴 3
Optional<Friend> opt = ...;
Friend friend = opt.orElseThrow(() -> new CustomException());
```

---

## 관련 파일

- `map-log-backend/src/main/java/com/maplog/friend/command/service/FriendCommandService.java`
- `map-log-backend/src/main/java/com/maplog/friend/command/repository/FriendCommandRepository.java`
