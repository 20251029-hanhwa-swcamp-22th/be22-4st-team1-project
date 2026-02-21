import { describe, it, expect, vi, beforeEach } from 'vitest'
import axios from 'axios'
import MockAdapter from 'axios-mock-adapter'
import api from '../axios.js'

describe('Axios Interceptor', () => {
  let mock

  beforeEach(() => {
    mock = new MockAdapter(api)
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('요청 헤더에 토큰이 포함되어야 함', async () => {
    localStorage.setItem('ml_access_token', 'test-token')
    mock.onGet('/test').reply(200, { success: true })

    await api.get('/test')

    expect(mock.history.get[0].headers['Authorization']).toBe('Bearer test-token')
  })

  it('401 에러 시 토큰 재발급 로직이 작동해야 함', async () => {
    localStorage.setItem('ml_access_token', 'old-token')
    localStorage.setItem('ml_refresh_token', 'refresh-token')

    const axiosMock = new MockAdapter(axios)
    axiosMock.onPost('http://localhost:8080/api/auth/refresh').reply(200, {
      data: { accessToken: 'new-token', refreshToken: 'new-refresh' }
    })

    mock.onGet('/test').replyOnce(401)
    mock.onGet('/test').reply(200, { success: true })

    const response = await api.get('/test')

    expect(response.success).toBe(true)
    expect(localStorage.getItem('ml_access_token')).toBe('new-token')

    axiosMock.restore()
  })
})
