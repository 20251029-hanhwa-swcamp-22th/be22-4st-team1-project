import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/app/api/auth.js'

export const useAuthStore = defineStore('auth', () => {
    // ── State ──
    const user = ref(JSON.parse(localStorage.getItem('ml_user') || 'null'))
    const accessToken = ref(localStorage.getItem('ml_access_token') || '')
    const refreshToken = ref(localStorage.getItem('ml_refresh_token') || '')

    // ── Getters ──
    const isAuthenticated = computed(() => !!accessToken.value)
    const isAdmin = computed(() => user.value?.role === 'ADMIN')
    const userId = computed(() => user.value?.userId)
    const nickname = computed(() => user.value?.nickname || '')
    const profileImageUrl = computed(() => user.value?.profileImageUrl || null)

    // ── Actions ──
    function setTokens({ accessToken: at, refreshToken: rt }) {
        accessToken.value = at
        refreshToken.value = rt
        localStorage.setItem('ml_access_token', at)
        localStorage.setItem('ml_refresh_token', rt)
    }

    function setUser(userData) {
        user.value = userData
        localStorage.setItem('ml_user', JSON.stringify(userData))
    }

    async function login(credentials) {
        const res = await authApi.login(credentials)
        const { accessToken: at, refreshToken: rt, userId: id, nickname: nick } = res.data
        setTokens({ accessToken: at, refreshToken: rt })
        setUser({ userId: id, nickname: nick, role: res.data.role })
        return res
    }

    async function signup(payload) {
        const res = await authApi.signup(payload)
        const { accessToken: at, refreshToken: rt, userId: id, nickname: nick, role } = res.data
        setTokens({ accessToken: at, refreshToken: rt })
        setUser({ userId: id, nickname: nick, role })
        return res
    }

    async function logout() {
        try {
            await authApi.logout()
        } catch (_) {
            // 실패해도 로컬 정리
        } finally {
            clear()
        }
    }

    function clear() {
        user.value = null
        accessToken.value = ''
        refreshToken.value = ''
        localStorage.removeItem('ml_access_token')
        localStorage.removeItem('ml_refresh_token')
        localStorage.removeItem('ml_user')
    }

    function updateUser(userData) {
        const updated = { ...user.value, ...userData }
        setUser(updated)
    }

    return {
        user, accessToken, refreshToken,
        isAuthenticated, isAdmin, userId, nickname, profileImageUrl,
        setTokens, setUser, updateUser,
        login, signup, logout, clear
    }
})
