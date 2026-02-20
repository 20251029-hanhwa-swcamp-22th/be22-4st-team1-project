package com.maplog.friend.command.repository;

import com.maplog.friend.command.domain.Friend;
import com.maplog.friend.command.domain.FriendStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendCommandRepository extends JpaRepository<Friend, Long> {

    @Query("SELECT f FROM Friend f WHERE (f.requesterId = :userId1 AND f.receiverId = :userId2) OR (f.requesterId = :userId2 AND f.receiverId = :userId1)")
    Optional<Friend> findByUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    Page<Friend> findByReceiverIdAndStatus(Long receiverId, FriendStatus status, Pageable pageable);

    @Query("SELECT f FROM Friend f WHERE f.status = 'ACCEPTED' AND (f.requesterId = :userId OR f.receiverId = :userId)")
    List<Friend> findAcceptedFriends(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) > 0 FROM Friend f WHERE f.status = 'ACCEPTED' AND ((f.requesterId = :userId1 AND f.receiverId = :userId2) OR (f.requesterId = :userId2 AND f.receiverId = :userId1))")
    boolean isFriend(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}