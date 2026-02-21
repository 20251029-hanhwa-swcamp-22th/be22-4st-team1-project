/**
 * 사용자 인증(로그인, 회원가입, 로그아웃)과 관련된 API 호출을 관리하는 모듈입니다.
 * 모든 요청은 전역 Axios 인스턴스(api)를 통해 수행됩니다.
 */
import api from './axios.js'

export const authApi = {
    /**
     * 회원가입 요청
     * @param {Object} payload - email, password, nickname 포함
     */
    signup(payload) {
        return api.post('/api/auth/signup', payload)
    },

    /**
     * 로그인 요청
     * @param {Object} credentials - email, password 포함
     * @returns {Promise} accessToken, refreshToken 포함된 응답
     */
    login(credentials) {
        return api.post('/api/auth/login', credentials)
    },

    /**
     * 로그아웃 요청 (Access Token 만료 처리)
     */
    logout() {
        return api.post('/api/auth/logout')
    },

    /**
     * 닉네임 중복 확인
     * @param {string} nickname
     */
    checkNickname(nickname) {
        return api.get('/api/users/check-nickname', { params: { nickname } })
    }
}
