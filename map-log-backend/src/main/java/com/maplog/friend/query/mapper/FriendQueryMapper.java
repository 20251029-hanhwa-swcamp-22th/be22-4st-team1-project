package com.maplog.friend.query.mapper;

import com.maplog.diary.query.dto.DiarySummaryResponse;
import com.maplog.friend.query.dto.FriendRequestResponse;
import com.maplog.friend.query.dto.FriendSummaryResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FriendQueryMapper {

    List<FriendSummaryResponse> findFriends(@Param("userId") Long userId);

    List<FriendRequestResponse> findPendingRequests(@Param("userId") Long userId,
                                                    @Param("offset") int offset,
                                                    @Param("size") int size);

    long countPendingRequests(@Param("userId") Long userId);

    List<DiarySummaryResponse> findFeed(@Param("userId") Long userId,
                                        @Param("offset") int offset,
                                        @Param("size") int size);

    long countFeed(@Param("userId") Long userId);

    boolean isFriend(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}