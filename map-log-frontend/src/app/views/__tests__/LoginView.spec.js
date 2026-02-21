import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import LoginView from '../LoginView.vue'
import { useAuthStore } from '../../stores/auth.js'
import { useRouter, useRoute } from 'vue-router'

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    useRouter: vi.fn(),
    useRoute: vi.fn()
  }
})

describe('LoginView Integration', () => {
  let routerPush

  beforeEach(() => {
    routerPush = vi.fn()
    useRouter.mockReturnValue({ push: routerPush })
    useRoute.mockReturnValue({ query: {} })
  })

  it('이메일과 비밀번호 입력 후 로그인 버튼 클릭 시 auth.login이 호출되어야 함', async () => {
    const wrapper = mount(LoginView, {
      global: {
        plugins: [createTestingPinia({ stubActions: false })],
        stubs: ['RouterLink']
      }
    })

    const authStore = useAuthStore()
    // mock api calls within the store action
    vi.spyOn(authStore, 'login').mockResolvedValue({})

    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('test@email.com')
    await inputs[1].setValue('password123')

    await wrapper.find('form').trigger('submit.prevent')

    expect(authStore.login).toHaveBeenCalledWith({
      email: 'test@email.com',
      password: 'password123'
    })
    expect(routerPush).toHaveBeenCalledWith('/map')
  })

  it('로그인 실패 시 에러 메시지가 표시되어야 함', async () => {
    const wrapper = mount(LoginView, {
      global: {
        plugins: [createTestingPinia({ stubActions: false })],
        stubs: ['RouterLink']
      }
    })

    const authStore = useAuthStore()
    vi.spyOn(authStore, 'login').mockRejectedValue({ code: 'USER_NOT_FOUND' })

    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('wrong@email.com')
    await inputs[1].setValue('wrongpassword')

    await wrapper.find('form').trigger('submit.prevent')
    await new Promise(resolve => setTimeout(resolve, 0)) // flush promises

    expect(wrapper.text()).toContain('존재하지 않는 이메일입니다.')
  })
})
