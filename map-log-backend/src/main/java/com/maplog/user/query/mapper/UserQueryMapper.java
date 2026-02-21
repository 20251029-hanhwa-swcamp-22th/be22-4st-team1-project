package com.maplog.user.query.mapper;

import com.maplog.user.query.dto.UserProfileQueryResponse;
import com.maplog.user.query.dto.UserSummaryResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserQueryMapper {

    UserProfileQueryResponse findMyProfile(@Param("email") String email);

    List<UserSummaryResponse> searchUsers(@Param("keyword") String keyword);
}