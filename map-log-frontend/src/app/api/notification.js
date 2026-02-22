import api from './axios.js'

export const notificationApi = {
    /** 알림 목록 조회 - GET /api/notifications */
    getNotifications({ isRead } = {}) {
        return api.get('/notifications', { params: isRead !== undefined ? { isRead } : {} })
    },

    /** 알림 단건 읽음 - PATCH /api/notifications/:id/read */
    readOne(notificationId) {
        return api.patch(`/notifications/${notificationId}/read`)
    },

    /** 알림 전체 읽음 - PATCH /api/notifications/read-all */
    readAll() {
        return api.patch('/notifications/read-all')
    },

    /** 알림 일괄 삭제 - DELETE /api/notifications?isRead=Y|N */
    deleteAll(isRead) {
        return api.delete('/notifications', { params: isRead !== undefined ? { isRead } : {} })
    }
}
