import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { notificationApi } from '@/app/api/notification.js'

export const useNotificationStore = defineStore('notification', () => {
    const notifications = ref([])
    const loading = ref(false)

    // SSE 연결 관련 상태
    let eventSource = null          // EventSource 인스턴스
    let reconnectTimer = null       // 재연결 타이머

    // 【SSE 친구 이벤트 트리거】
    // 친구 요청/수락 SSE 이벤트가 오면 이 값이 증가
    // → FriendView에서 watch하여 pending 목록 자동 재조회
    const friendEventTrigger = ref(0)

    const unreadCount = computed(() => {
        return notifications.value.filter(n => !n.read).length
    })

    async function fetchNotifications() {
        loading.value = true
        try {
            const res = await notificationApi.getNotifications()
            // 백엔드 필드명이 is_read AS read 이므로 n.read 확인
            notifications.value = res?.data?.content || []
        } catch (e) {
            console.error('Failed to fetch notifications:', e)
        } finally {
            loading.value = false
        }
    }

    /**
     * 【SSE 연결 시작】
     * EventSource를 사용하여 백엔드의 SSE 엔드포인트에 연결합니다.
     *
     * ⚠️ EventSource는 HTTP 헤더(Authorization)를 설정할 수 없으므로
     *    JWT 토큰을 쿼리 파라미터(?token=xxx)로 전달합니다.
     *
     * 이벤트 수신 시:
     * - 'notification' 이벤트 → 알림 목록을 서버에서 다시 조회하여 최신 상태 반영
     * - 연결 에러 시 3초 후 자동 재연결 시도
     */
    function connectSSE() {
        // 이미 연결 중이면 무시
        if (eventSource) return

        const token = localStorage.getItem('ml_access_token')
        if (!token) return

        // 백엔드 API 베이스 URL 가져오기
        // ⚠️ VITE_API_BASE_URL이 '/api'로 끝날 수 있으므로 (예: http://localhost:8080/api)
        //    SSE URL 생성 시 '/api'를 제거하여 중복 방지
        let baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
        if (baseUrl.endsWith('/api')) {
            baseUrl = baseUrl.slice(0, -4)  // '/api' 제거 → 'http://localhost:8080'
        }
        const sseUrl = `${baseUrl}/api/sse/connect?token=${token}`

        eventSource = new EventSource(sseUrl)

        // 【연결 성공】
        eventSource.addEventListener('connect', (e) => {
            console.log('[SSE] 연결 성공:', e.data)
        })

        // 【알림 수신】서버에서 알림 이벤트가 오면 알림 목록 갱신
        eventSource.addEventListener('notification', (e) => {
            // 실시간 이벤트를 받으면 전체 알림 목록을 다시 조회
            // → unreadCount가 자동으로 갱신되어 UI에 즉시 반영됨
            fetchNotifications()

            // 친구 관련 이벤트면 friendEventTrigger 증가
            // → FriendView의 watch가 감지하여 pending 목록도 자동 갱신
            try {
                const data = JSON.parse(e.data)
                if (data.type === 'FRIEND_REQUEST' || data.type === 'FRIEND_ACCEPTED' || data.type === 'FRIEND_DELETED') {
                    friendEventTrigger.value++
                }
            } catch {
                // 파싱 실패해도 알림 갱신은 이미 처리됨
            }
        })

        // 【에러/연결 끊김】3초 후 자동 재연결
        eventSource.onerror = () => {
            console.warn('[SSE] 연결 끊김, 3초 후 재연결...')
            disconnectSSE()
            reconnectTimer = setTimeout(() => {
                connectSSE()
            }, 3000)
        }
    }

    /**
     * 【SSE 연결 해제】
     * 페이지 이동이나 로그아웃 시 호출하여 SSE 연결을 정리합니다.
     */
    function disconnectSSE() {
        if (eventSource) {
            eventSource.close()
            eventSource = null
        }
        if (reconnectTimer) {
            clearTimeout(reconnectTimer)
            reconnectTimer = null
        }
    }

    async function markAsRead(notificationId) {
        try {
            await notificationApi.readOne(notificationId)
            const noti = notifications.value.find(n => n.id === notificationId)
            if (noti) noti.read = true
        } catch (e) {
            console.error('Failed to mark notification as read:', e)
        }
    }

    async function markAllAsRead() {
        try {
            await notificationApi.readAll()
            notifications.value = notifications.value.map(n => ({ ...n, read: true }))
        } catch (e) {
            console.error('Failed to mark all as read:', e)
        }
    }

    async function deleteNotifications(isRead) {
        try {
            await notificationApi.deleteAll(isRead)
            if (isRead === 'Y') {
                notifications.value = notifications.value.filter(n => !n.read)
            } else if (isRead === 'N') {
                notifications.value = notifications.value.filter(n => n.read)
            } else {
                notifications.value = []
            }
        } catch (e) {
            console.error('Failed to delete notifications:', e)
        }
    }

    return {
        notifications,
        loading,
        unreadCount,
        friendEventTrigger,
        fetchNotifications,
        connectSSE,
        disconnectSSE,
        markAsRead,
        markAllAsRead,
        deleteNotifications
    }
})
