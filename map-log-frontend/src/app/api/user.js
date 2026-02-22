import api from './axios.js'

export const userApi = {
    /** 마이페이지 조회 - GET /api/users/me */
    getMe() {
        return api.get('/users/me')
    },

    /** 프로필 수정 - PATCH /api/users/me (multipart) */
    updateMe(formData) {
        return api.patch('/users/me', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
    },

    /** 내 일기 목록 - GET /api/users/me/diaries */
    getMyDiaries({ page = 0, size = 10 } = {}) {
        return api.get('/users/me/diaries', { params: { page, size } })
    },

    /** 내 스크랩 목록 - GET /api/users/me/scraps */
    getMyScraps({ page = 0, size = 10 } = {}) {
        return api.get('/users/me/scraps', { params: { page, size } })
    },

    /** 회원 탈퇴 - DELETE /api/users/me */
    deleteMe() {
        return api.delete('/users/me')
    },

    /** 닉네임 중복 확인 - GET /api/users/check-nickname?nickname=... */
    checkNickname(nickname) {
        return api.get('/users/check-nickname', { params: { nickname } })
    },

    /** 사용자 검색 - GET /api/users/search?nickname=keyword */
    searchUsers(nickname) {
        return api.get('/users/search', { params: { nickname } })
    }
}
