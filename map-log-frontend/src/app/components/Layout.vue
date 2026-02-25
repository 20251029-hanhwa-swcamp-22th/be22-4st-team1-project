<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { MapPin, Rss, Users, Bell, User, LogOut, Shield } from 'lucide-vue-next'
import { useAuthStore } from '@/app/stores/auth.js'
import { useNotificationStore } from '@/app/stores/notification.js'

const auth = useAuthStore()
const notificationStore = useNotificationStore()
const router = useRouter()

const navItems = computed(() => {
  const items = [
    { to: '/map',           label: '지도',    icon: MapPin },
    { to: '/feed',          label: '피드',    icon: Rss },
    { to: '/friends',       label: '친구',    icon: Users },
    { 
      to: '/notifications', 
      label: '알림',    
      icon: Bell, 
      badge: notificationStore.unreadCount || null 
    },
    { to: '/mypage',        label: '마이페이지', icon: User }
  ]
  if (auth.isAdmin) {
    items.push({ to: '/admin', label: '관리자', icon: Shield })
  }
  return items
})

const avatarInitial = computed(() => {
  const n = auth.nickname
  return n ? n.charAt(0).toUpperCase() : '?'
})

async function handleLogout() {
  // 로그아웃 시 SSE 연결 해제
  notificationStore.disconnectSSE()
  await auth.logout()
  router.push('/login')
}

onMounted(() => {
  if (auth.isAuthenticated) {
    notificationStore.fetchNotifications()
    // 【SSE 실시간 알림 연결】로그인 상태면 SSE 자동 연결
    notificationStore.connectSSE()
  }
})

// 【SSE 연결 해제】컴포넌트 언마운트 시 정리
onUnmounted(() => {
  notificationStore.disconnectSSE()
})
</script>

<template>
  <div class="ml-layout">
    <!-- ── 사이드바 ── -->
    <aside class="ml-sidebar">
      <!-- 로고 -->
      <div class="ml-logo">
        <div class="ml-logo-icon">📍</div>
        <span class="ml-logo-text">MapLog</span>
      </div>

      <!-- 네비게이션 -->
      <nav class="ml-nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="ml-nav-item"
          active-class="active"
        >
          <component :is="item.icon" :size="18" />
          {{ item.label }}
          <span v-if="item.badge" class="ml-nav-badge">{{ item.badge > 99 ? '99+' : item.badge }}</span>
        </RouterLink>
      </nav>

      <!-- 하단: 유저 정보 + 로그아웃 -->
      <div class="ml-sidebar-footer">
        <div class="ml-user-info">
          <div class="ml-avatar">
            <img v-if="auth.profileImageUrl" :src="auth.profileImageUrl" :alt="auth.nickname" />
            <span v-else>{{ avatarInitial }}</span>
          </div>
          <div>
            <div class="ml-user-name truncate" style="max-width:140px">{{ auth.nickname }}</div>
            <div v-if="auth.isAdmin" class="text-sm" style="color:var(--color-accent)">관리자</div>
          </div>
        </div>
        <button class="btn btn-ghost btn-block" style="justify-content:flex-start;gap:8px" @click="handleLogout">
          <LogOut :size="15" />
          로그아웃
        </button>
      </div>
    </aside>

    <!-- ── 메인 콘텐츠 ── -->
    <main class="ml-main">
      <slot />
    </main>
  </div>
</template>
