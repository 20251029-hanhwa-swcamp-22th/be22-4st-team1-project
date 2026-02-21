import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { notificationApi } from '@/app/api/notification.js'

/**
 * 시스템 전체의 알림 상태를 관리하는 Store입니다.
 * 사이드바의 알림 배지(unreadCount)와 실시간 동기화를 보장합니다.
 */
export const useNotificationStore = defineStore('notification', () => {
    // ── State ──
    const notifications = ref([]) // 전체 알림 목록
    const loading = ref(false)    // 로딩 상태

    // ── Getters ──
    /** 읽지 않은 알림 개수를 계산하여 반환합니다. (반응형) */
    const unreadCount = computed(() => {
        return notifications.value.filter(n => !n.read).length
    })

    // ── Actions ──
    /** 서버로부터 최신 알림 목록을 가져와 상태를 동기화합니다. */
    async function fetchNotifications() {
        loading.value = true
        try {
            const res = await notificationApi.getNotifications()
            // 백엔드에서 넘어온 is_read 값을 n.read로 매핑하여 저장
            notifications.value = res?.data?.content || []
        } catch (e) {
            console.error('알림 목록 조회 실패:', e)
        } finally {
            loading.value = false
        }
    }

    /** 특정 알림을 읽음 처리하고 로컬 상태를 즉시 업데이트합니다. */
    async function markAsRead(notificationId) {
        try {
            await notificationApi.readOne(notificationId)
            const noti = notifications.value.find(n => n.id === notificationId)
            if (noti) noti.read = true // 배지 숫자가 즉시 감소함
        } catch (e) {
            console.error('알림 읽음 처리 실패:', e)
        }
    }

    /** 모든 알림을 일괄 읽음 처리합니다. */
    async function markAllAsRead() {
        try {
            await notificationApi.readAll()
            notifications.value = notifications.value.map(n => ({ ...n, read: true }))
        } catch (e) {
            console.error('전체 읽음 처리 실패:', e)
        }
    }

    /** 읽음 여부에 따라 알림을 삭제합니다. */
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
            console.error('알림 삭제 실패:', e)
        }
    }

    return {
        notifications,
        loading,
        unreadCount,
        fetchNotifications,
        markAsRead,
        markAllAsRead,
        deleteNotifications
    }
})
