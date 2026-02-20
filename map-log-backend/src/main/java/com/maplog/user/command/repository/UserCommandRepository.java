package com.maplog.user.command.repository;

import com.maplog.user.command.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCommandRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    List<User> findByNicknameContainingAndDeletedAtIsNull(String nickname);

    Page<User> findAllByDeletedAtIsNull(Pageable pageable);
}
