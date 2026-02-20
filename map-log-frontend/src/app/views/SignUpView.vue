<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/app/stores/auth.js'

const auth = useAuthStore()
const router = useRouter()

const form = ref({ nickname: '', email: '', password: '', passwordConfirm: '' })
const error = ref('')
const loading = ref(false)

async function handleSignup() {
  error.value = ''

  // 유효성 검사
  if (form.value.nickname.length < 2 || form.value.nickname.length > 20) {
    error.value = '닉네임은 2~20자 사이여야 합니다.'
    return
  }
  if (form.value.password.length < 6) {
    error.value = '비밀번호는 6자 이상이어야 합니다.'
    return
  }
  if (form.value.password !== form.value.passwordConfirm) {
    error.value = '비밀번호가 일치하지 않습니다.'
    return
  }

  loading.value = true
  try {
    await auth.signup({
      email: form.value.email,
      password: form.value.password,
      nickname: form.value.nickname
    })
    router.push('/map')
  } catch (e) {
    if (e?.code === 'DUPLICATE_EMAIL' || e?.message?.includes('409')) {
      error.value = '이미 사용 중인 이메일 또는 닉네임입니다.'
    } else {
      error.value = '회원가입에 실패했습니다. 다시 시도해주세요.'
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-wrapper">
    <div class="auth-card">
      <div class="auth-logo">
        <div class="auth-logo-icon">📍</div>
        <h1 class="auth-title">회원가입</h1>
        <p class="auth-subtitle">MapLog와 함께 여행을 기록하세요</p>
      </div>

      <form @submit.prevent="handleSignup">
        <div class="form-group">
          <label class="form-label">닉네임 <span style="color:var(--color-text-3)">(2~20자)</span></label>
          <input
            v-model="form.nickname"
            type="text"
            class="form-input"
            placeholder="닉네임을 입력하세요"
            minlength="2"
            maxlength="20"
            required
          />
        </div>

        <div class="form-group">
          <label class="form-label">이메일</label>
          <input
            v-model="form.email"
            type="email"
            class="form-input"
            placeholder="이메일을 입력하세요"
            required
          />
        </div>

        <div class="form-group">
          <label class="form-label">비밀번호 <span style="color:var(--color-text-3)">(6자 이상)</span></label>
          <input
            v-model="form.password"
            type="password"
            class="form-input"
            placeholder="비밀번호를 입력하세요"
            minlength="6"
            required
          />
        </div>

        <div class="form-group">
          <label class="form-label">비밀번호 확인</label>
          <input
            v-model="form.passwordConfirm"
            type="password"
            class="form-input"
            placeholder="비밀번호를 다시 입력하세요"
            required
          />
        </div>

        <p v-if="error" class="text-danger text-sm" style="margin-bottom:12px">{{ error }}</p>

        <button type="submit" class="btn btn-primary btn-block" :disabled="loading">
          <span v-if="loading" class="spinner" style="width:16px;height:16px;border-width:2px"></span>
          <span v-else>가입하기</span>
        </button>
      </form>

      <p class="text-center text-sm text-muted mt-4">
        이미 계정이 있으신가요?
        <RouterLink to="/login" style="color:var(--color-primary);font-weight:600">로그인</RouterLink>
      </p>
    </div>
  </div>
</template>