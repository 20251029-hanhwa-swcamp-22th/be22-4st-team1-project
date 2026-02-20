import axios from 'axios'
import router from '@/app/router/index.js'

const api = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
    timeout: 10000,
    headers: { 'Content-Type': 'application/json' }
})

// ── 요청 인터셉터: accessToken 자동 첨부 ──
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('ml_access_token')
    if (token) {
        config.headers.Authorization = `Bearer ${token}`
    }
    return config
})

let isRefreshing = false
let pendingQueue = []

// ── 응답 인터셉터: 401 시 토큰 재발급 ──
api.interceptors.response.use(
    (res) => res.data, // ApiResponse 자체를 반환
    async (error) => {
        const original = error.config

        if (error.response?.status === 401 && !original._retry) {
            original._retry = true

            if (isRefreshing) {
                return new Promise((resolve, reject) => {
                    pendingQueue.push({ resolve, reject })
                }).then(() => api(original))
            }

            isRefreshing = true
            const refreshToken = localStorage.getItem('ml_refresh_token')

            try {
                const res = await axios.post(
                    `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api/auth/refresh`,
                    { refreshToken }
                )
                const newToken = res.data?.data?.accessToken
                localStorage.setItem('ml_access_token', newToken)
                pendingQueue.forEach(p => p.resolve())
                pendingQueue = []
                original.headers.Authorization = `Bearer ${newToken}`
                return api(original)
            } catch (_) {
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
