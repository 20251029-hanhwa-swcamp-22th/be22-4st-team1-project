package com.maplog.notification.query.mapper;

import com.maplog.notification.query.dto.NotificationResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationQueryMapper {

    List<NotificationResponse> findNotifications(@Param("userId") Long userId,
                                                 @Param("readFilter") Boolean readFilter,
                                                 @Param("offset") int offset,
                                                 @Param("size") int size);

    long countNotifications(@Param("userId") Long userId,
                            @Param("readFilter") Boolean readFilter);
}