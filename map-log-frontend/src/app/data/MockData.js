// 백엔드 미연결 시 사용하는 개발용 목업 데이터
// API 응답 형식(ApiResponse)과 동일한 구조

export const mockDiaries = [
    {
        diaryId: 1,
        title: '경복궁에서의 하루',
        content: '오늘은 경복궁을 다녀왔다. 날씨가 맑아서 더욱 아름다웠다.',
        locationName: '경복궁',
        address: '서울특별시 종로구 사직로 161',
        latitude: 37.5796,
        longitude: 126.9770,
        images: [{ imageId: 1, imageUrl: 'https://picsum.photos/seed/1/400/300', imageOrder: 1 }],
        createdAt: '2026-02-10T14:00:00',
        updatedAt: '2026-02-10T14:00:00',
        userId: 1
    },
    {
        diaryId: 2,
        title: '한강 피크닉',
        content: '친구들과 한강에서 피크닉을 즐겼다.',
        locationName: '뚝섬한강공원',
        address: '서울특별시 광진구 강변북로 139',
        latitude: 37.5311,
        longitude: 127.0674,
        images: [{ imageId: 2, imageUrl: 'https://picsum.photos/seed/2/400/300', imageOrder: 1 }],
        createdAt: '2026-02-12T11:00:00',
        updatedAt: '2026-02-12T11:00:00',
        userId: 1
    },
    {
        diaryId: 3,
        title: '성수동 카페 탐방',
        content: '성수동의 핫한 카페를 돌아다녔다.',
        locationName: '성수동',
        address: '서울특별시 성동구 성수동',
        latitude: 37.5446,
        longitude: 127.0553,
        images: [],
        createdAt: '2026-02-15T16:00:00',
        updatedAt: '2026-02-15T16:00:00',
        userId: 1
    }
]

export const mockUser = {
    userId: 1,
    email: 'test@maplog.com',
    nickname: '홍길동',
    profileImageUrl: null,
    role: 'USER',
    status: 'ACTIVE',
    createdAt: '2026-01-01T09:00:00'
}

export const mockFriends = [
    { friendId: 1, userId: 2, nickname: '김여행', profileImageUrl: null, respondedAt: '2026-01-15T10:00:00' },
    { friendId: 2, userId: 3, nickname: '이탐험', profileImageUrl: null, respondedAt: '2026-01-20T14:00:00' }
]

export const mockPending = [
    { friendId: 3, requesterId: 4, nickname: '박모험', profileImageUrl: null, requestedAt: '2026-02-18T09:00:00' }
]

export const mockNotifications = [
    {
        notificationId: 1,
        notificationType: 'FRIEND',
        title: '김여행님이 친구 요청을 수락했습니다.',
        content: null,
        linkUrl: '/friends',
        isRead: 'N',
        createdAt: '2026-02-19T10:00:00',
        readAt: null
    },
    {
        notificationId: 2,
        notificationType: 'DIARY',
        title: '이탐험님이 일기를 공유했습니다.',
        content: '한강 피크닉',
        linkUrl: '/diaries/2',
        isRead: 'Y',
        createdAt: '2026-02-18T15:00:00',
        readAt: '2026-02-18T15:05:00'
    }
]

export const mockFeed = [
    {
        diaryId: 4,
        title: '이탐험의 북한산 등반',
        locationName: '북한산',
        thumbnailUrl: 'https://picsum.photos/seed/4/400/300',
        sharedAt: '2026-02-17T09:00:00',
        author: { userId: 3, nickname: '이탐험', profileImageUrl: null }
    }
]

export const mockMarkers = [
    { diaryId: 1, latitude: 37.5796, longitude: 126.9770, locationName: '경복궁', title: '경복궁에서의 하루' },
    { diaryId: 2, latitude: 37.5311, longitude: 127.0674, locationName: '뚝섬한강공원', title: '한강 피크닉' },
    { diaryId: 3, latitude: 37.5446, longitude: 127.0553, locationName: '성수동', title: '성수동 카페 탐방' }
]
