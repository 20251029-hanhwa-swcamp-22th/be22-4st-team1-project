import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { notificationApi } from '@/app/api/notification.js'

export const useNotificationStore = defineStore('notification', () => {
    const notifications = ref([])
    const loading = ref(false)

    // SSE 연결 관련 상태
    let abortController = null      // fetch AbortController (SSE 연결 제어)
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
     * fetch + ReadableStream을 사용하여 SSE 엔드포인트에 연결합니다.
     *
     * EventSource는 Authorization 헤더를 설정할 수 없어 토큰이 URL에 노출됩니다.
     * fetch를 사용하면 Authorization 헤더로 안전하게 토큰을 전달할 수 있습니다.
     *
     * 이벤트 수신 시:
     * - 'notification' 이벤트 → 알림 목록을 서버에서 다시 조회하여 최신 상태 반영
     * - 연결 종료/에러 시 3초 후 자동 재연결 시도
     */
    function connectSSE() {
        if (abortController) return

        const token = localStorage.getItem('ml_access_token')
        if (!token) return

        let baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
        if (baseUrl.endsWith('/api')) {
            baseUrl = baseUrl.slice(0, -4)
        }

        abortController = new AbortController()

        fetch(`${baseUrl}/api/sse/connect`, {
            headers: {
                Authorization: `Bearer ${token}`,
                Accept: 'text/event-stream',
            },
            signal: abortController.signal,
        })
            .then(response => {
                const reader = response.body.getReader()
                const decoder = new TextDecoder()
                let buffer = ''
                let eventType = 'message'
                let dataLines = []

                function read() {
                    reader.read().then(({ done, value }) => {
                        if (done) { scheduleReconnect(); return }

                        buffer += decoder.decode(value, { stream: true })
                        const lines = buffer.split('\n')
                        buffer = lines.pop() // 미완성 마지막 라인 보존

                        for (const line of lines) {
                            if (line === '') {
                                // 빈 줄 = 이벤트 완성
                                if (dataLines.length > 0) {
                                    handleSSEEvent(eventType, dataLines.join('\n'))
                                }
                                eventType = 'message'
                                dataLines = []
                            } else if (line.startsWith('event:')) {
                                eventType = line.slice(6).trim()
                            } else if (line.startsWith('data:')) {
                                dataLines.push(line.slice(5).trim())
                            }
                        }

                        read()
                    }).catch(err => {
                        if (err.name !== 'AbortError') scheduleReconnect()
                    })
                }
                read()
            })
            .catch(err => {
                if (err.name !== 'AbortError') scheduleReconnect()
            })
    }

    function handleSSEEvent(type, data) {
        if (type === 'connect') {
            console.log('[SSE] 연결 성공:', data)
            return
        }
        if (type === 'notification') {
            fetchNotifications()
            try {
                const parsed = JSON.parse(data)
                if (parsed.type === 'FRIEND_REQUEST' || parsed.type === 'FRIEND_ACCEPTED' || parsed.type === 'FRIEND_DELETED') {
                    friendEventTrigger.value++
                }
            } catch {
                // 파싱 실패해도 알림 갱신은 이미 처리됨
            }
        }
    }

    function scheduleReconnect() {
        abortController = null
        console.warn('[SSE] 연결 끊김, 3초 후 재연결...')
        reconnectTimer = setTimeout(connectSSE, 3000)
    }

    /**
     * 【SSE 연결 해제】
     * 페이지 이동이나 로그아웃 시 호출하여 SSE 연결을 정리합니다.
     */
    function disconnectSSE() {
        if (abortController) {
            abortController.abort()
            abortController = null
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
