import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { notificationApi } from '@/app/api/notification.js'

export const useNotificationStore = defineStore('notification', () => {
    const notifications = ref([])
    const loading = ref(false)

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
        fetchNotifications,
        markAsRead,
        markAllAsRead,
        deleteNotifications
    }
})
