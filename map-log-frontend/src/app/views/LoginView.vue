<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/app/stores/auth.js'
import { mockUser } from '@/app/data/MockData.js'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const form = ref({ email: '', password: '' })
const error = ref('')
const loading = ref(false)

// ──────────────────────────────────────────
// ⚠️ 임시 Mock 로그인 (백엔드 미연결 시)
// test@maplog.com / test1234  → 일반 USER
// admin@maplog.com / admin1234 → ADMIN
// ──────────────────────────────────────────
const MOCK_ACCOUNTS = {
  'test@maplog.com':  { password: 'test1234',  user: { ...mockUser } },
  'admin@maplog.com': { password: 'admin1234', user: { ...mockUser, userId: 99, email: 'admin@maplog.com', nickname: '관리자', role: 'ADMIN' } }
}

function mockLogin(email, password) {
  const account = MOCK_ACCOUNTS[email]
  if (!account) throw { code: 'USER_NOT_FOUND' }
  if (account.password !== password) throw { code: 'INVALID_PASSWORD' }
  auth.setTokens({ accessToken: 'mock-access-token', refreshToken: 'mock-refresh-token' })
  auth.setUser(account.user)
}

async function handleLogin() {
  error.value = ''
  if (!form.value.email || !form.value.password) {
    error.value = '이메일과 비밀번호를 입력해주세요.'
    return
  }
  loading.value = true
  try {
    // 실제 백엔드 호출 시도 → 실패하면 Mock으로 대체
    try {
      await auth.login(form.value)
    } catch {
      mockLogin(form.value.email, form.value.password)
    }
    const redirect = route.query.redirect || '/map'
    router.push(redirect)
  } catch (e) {
    if (e?.code === 'USER_NOT_FOUND') {
      error.value = '존재하지 않는 이메일입니다.'
    } else if (e?.code === 'INVALID_PASSWORD') {
      error.value = '비밀번호가 올바르지 않습니다.'
    } else if (e?.code === 'SUSPENDED') {
      error.value = '정지된 계정입니다. 관리자에게 문의하세요.'
    } else {
      error.value = '로그인에 실패했습니다. 다시 시도해주세요.'
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-wrapper">
    <div class="auth-card">
      <!-- 로고 -->
      <div class="auth-logo">
        <div class="auth-logo-icon">📍</div>
        <h1 class="auth-title">MapLog</h1>
        <p class="auth-subtitle">지도 위의 나만의 일기</p>
      </div>

      <!-- 폼 -->
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label class="form-label">이메일</label>
          <input
            v-model="form.email"
            type="email"
            class="form-input"
            placeholder="이메일을 입력하세요"
            autocomplete="email"
            required
          />
        </div>

        <div class="form-group">
          <label class="form-label">비밀번호</label>
          <input
            v-model="form.password"
            type="password"
            class="form-input"
            placeholder="비밀번호를 입력하세요"
            autocomplete="current-password"
            required
          />
        </div>

        <p v-if="error" class="text-danger text-sm mt-2" style="margin-bottom:12px">{{ error }}</p>

        <button type="submit" class="btn btn-primary btn-block" :disabled="loading" style="margin-top:4px">
          <span v-if="loading" class="spinner" style="width:16px;height:16px;border-width:2px"></span>
          <span v-else>로그인</span>
        </button>
      </form>

      <p class="text-center text-sm text-muted mt-4">
        계정이 없으신가요?
        <RouterLink to="/signup" style="color:var(--color-primary);font-weight:600">회원가입</RouterLink>
      </p>
    </div>
  </div>
</template>