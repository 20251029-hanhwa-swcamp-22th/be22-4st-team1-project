import api from './axios.js'

export const adminApi = {
    /** 회원 목록 조회 - GET /api/admin/users (ADMIN) */
    getUsers({ page = 0, size = 20, status } = {}) {
        const params = { page, size }
        if (status) params.status = status
        return api.get('/admin/users', { params })
    },

    /** 회원 상태 변경 - PATCH /api/admin/users/:userId/status (ADMIN) */
    changeStatus(userId, { status, suspensionReason, suspensionExpiresAt }) {
        return api.patch(`/admin/users/${userId}/status`, {
            status,
            suspensionReason,
            suspensionExpiresAt
        })
    }
}
