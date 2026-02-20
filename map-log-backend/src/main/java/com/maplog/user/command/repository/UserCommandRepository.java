package com.maplog.user.command.repository;

import com.maplog.user.command.domain.User;
import com.maplog.user.command.domain.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCommandRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    boolean existsByNicknameAndDeletedAtIsNull(String nickname);

    boolean existsByNicknameAndIdNotAndDeletedAtIsNull(String nickname, Long id);

    List<User> findByNicknameContainingAndDeletedAtIsNull(String nickname);

    Page<User> findAllByDeletedAtIsNull(Pageable pageable);

    Page<User> findAllByStatusAndDeletedAtIsNull(UserStatus status, Pageable pageable);
}
