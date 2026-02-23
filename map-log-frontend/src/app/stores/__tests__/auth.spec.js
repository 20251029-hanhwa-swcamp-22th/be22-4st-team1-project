import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../auth.js'
import { authApi } from '../../api/auth.js'
import { userApi } from '../../api/user.js'

vi.mock('../../api/auth.js', () => ({
  authApi: {
    login: vi.fn(),
    signup: vi.fn(),
    logout: vi.fn()
  }
}))

vi.mock('../../api/user.js', () => ({
  userApi: {
    getMe: vi.fn()
  }
}))

describe('Auth Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('로그인 성공 시 토큰과 유저 정보가 저장되어야 함', async () => {
    const authStore = useAuthStore()
    const mockTokens = { accessToken: 'at', refreshToken: 'rt' }
    const mockUser = { id: 1, email: 'test@email.com', nickname: 'nick' }

    authApi.login.mockResolvedValue({ data: mockTokens })
    userApi.getMe.mockResolvedValue({ data: mockUser })

    await authStore.login({ email: 'test@email.com', password: 'pw' })

    expect(authStore.accessToken).toBe('at')
    expect(authStore.refreshToken).toBe('rt')
    expect(authStore.user.userId).toBe(1)
    expect(localStorage.getItem('ml_access_token')).toBe('at')
  })

  it('로그아웃 시 상태와 저장소가 초기화되어야 함', async () => {
    const authStore = useAuthStore()
    authStore.setTokens({ accessToken: 'at', refreshToken: 'rt' })
    authStore.setUser({ userId: 1 })

    authApi.logout.mockResolvedValue({})

    await authStore.logout()

    expect(authStore.accessToken).toBe('')
    expect(authStore.user).toBeNull()
    expect(localStorage.getItem('ml_access_token')).toBeNull()
  })
})
