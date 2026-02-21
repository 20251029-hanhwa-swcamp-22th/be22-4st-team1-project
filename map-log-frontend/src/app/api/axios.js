import axios from 'axios'
import router from '@/app/router/index.js'

/**
 * 전역 Axios 인스턴스 설정
 * 1. 모든 요청에 Access Token 자동 첨부
 * 2. 401(Unauthorized) 에러 발생 시 Refresh Token으로 토큰 자동 갱신
 * 3. 여러 요청이 동시에 401을 받을 경우를 대비한 큐(pendingQueue) 관리
 */
const api = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
    timeout: 10000,
    headers: { 'Content-Type': 'application/json' }
})

// ── 요청 인터셉터: 로컬 스토리지의 토큰을 모든 헤더에 주입 ──
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('ml_access_token')
    if (token) {
        config.headers.Authorization = `Bearer ${token}`
    }
    return config
})

// 토큰 갱신 중 여부 및 대기 중인 요청 큐
let isRefreshing = false
let pendingQueue = []

// ── 응답 인터셉터: 에러 핸들링 및 토큰 재발급 로직 ──
api.interceptors.response.use(
    (res) => res.data, // 성공 시 데이터 레이어만 반환
    async (error) => {
        const original = error.config

        // 401 에러 발생 시 토큰 갱신 시도
        if (error.response?.status === 401 && !original._retry) {
            original._retry = true // 무한 루프 방지 플래그

            // 이미 다른 요청에 의해 갱신 프로세스가 진행 중인 경우 큐에 추가
            if (isRefreshing) {
                return new Promise((resolve, reject) => {
                    pendingQueue.push({ resolve, reject })
                }).then(() => api(original))
            }

            isRefreshing = true
            const refreshToken = localStorage.getItem('ml_refresh_token')

            try {
                // 백엔드 /api/auth/refresh 호출
                const res = await axios.post(
                    `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api/auth/refresh`,
                    { refreshToken }
                )
                const newToken = res.data?.data?.accessToken
                const newRefreshToken = res.data?.data?.refreshToken

                if (!newToken) throw new Error('토큰 갱신 실패')

                // 새 토큰 저장
                localStorage.setItem('ml_access_token', newToken)
                if (newRefreshToken) {
                    localStorage.setItem('ml_refresh_token', newRefreshToken)
                }

                // 대기 중이던 모든 요청 실행
                pendingQueue.forEach(p => p.resolve())
                pendingQueue = []
                
                // 원래 실패했던 요청 재시도
                original.headers.Authorization = `Bearer ${newToken}`
                return api(original)
            } catch (_) {
                // 갱신 실패 시 로그아웃 처리 및 로그인 페이지 이동
                pendingQueue.forEach(p => p.reject())
                pendingQueue = []
                localStorage.clear()
                router.push('/login')
                return Promise.reject(error)
            } finally {
                isRefreshing = false
            }
        }

        return Promise.reject(error.response?.data || error)
    }
)

export default api
