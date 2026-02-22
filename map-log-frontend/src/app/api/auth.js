import api from './axios.js'

export const authApi = {
    /** 회원가입 - POST /api/auth/signup */
    signup(payload) {
        return api.post('/auth/signup', payload)
    },

    /** 로그인 - POST /api/auth/login */
    login(payload) {
        return api.post('/auth/login', payload)
    },

    /** 토큰 재발급 - POST /api/auth/refresh */
    refresh() {
        const refreshToken = localStorage.getItem('ml_refresh_token')
        return api.post('/auth/refresh', { refreshToken })
    },

    /** 로그아웃 - POST /api/auth/logout */
    logout() {
        return api.post('/auth/logout')
    }
}
