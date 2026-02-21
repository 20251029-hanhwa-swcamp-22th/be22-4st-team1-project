import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/app/api/auth.js'
import { userApi } from '@/app/api/user.js'

function toUserState(me) {
    return {
        userId: me.id,
        email: me.email,
        nickname: me.nickname,
        role: me.role,
        profileImageUrl: me.profileImageUrl,
        createdAt: me.createdAt
    }
}

export const useAuthStore = defineStore('auth', () => {
    // State
    const user = ref(JSON.parse(localStorage.getItem('ml_user') || 'null'))
    const accessToken = ref(localStorage.getItem('ml_access_token') || '')
    const refreshToken = ref(localStorage.getItem('ml_refresh_token') || '')

    // Getters
    const isAuthenticated = computed(() => !!accessToken.value)
    const isAdmin = computed(() => user.value?.role === 'ADMIN')
    const userId = computed(() => user.value?.userId)
    const nickname = computed(() => user.value?.nickname || '')
    const profileImageUrl = computed(() => user.value?.profileImageUrl || null)

    // Actions
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

    async function hydrateUser() {
        if (!accessToken.value) return null

        const res = await userApi.getMe()
        const me = res?.data

        if (!me || me.id == null) {
            throw new Error('INVALID_USER_PAYLOAD')
        }

        const mappedUser = toUserState(me)
        setUser(mappedUser)
        return mappedUser
    }

    async function login(credentials) {
        const res = await authApi.login(credentials)
        const { accessToken: at, refreshToken: rt } = res.data || {}

        if (!at || !rt) {
            throw new Error('INVALID_LOGIN_PAYLOAD')
        }

        setTokens({ accessToken: at, refreshToken: rt })

        try {
            await hydrateUser()
        } catch (e) {
            clear()
            throw e
        }

        return res
    }

    async function signup(payload) {
        return authApi.signup(payload)
    }

    async function logout() {
        try {
            await authApi.logout()
        } catch (_) {
            // ignore and clear local state
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
        user,
        accessToken,
        refreshToken,
        isAuthenticated,
        isAdmin,
        userId,
        nickname,
        profileImageUrl,
        setTokens,
        setUser,
        updateUser,
        hydrateUser,
        login,
        signup,
        logout,
        clear
    }
})
