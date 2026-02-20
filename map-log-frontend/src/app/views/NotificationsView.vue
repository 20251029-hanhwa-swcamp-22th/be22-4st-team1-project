<script setup>
import { ref, computed, onMounted } from 'vue'
import { notificationApi } from '@/app/api/notification.js'
import { CheckCheck } from 'lucide-vue-next'
import { mockNotifications } from '@/app/data/MockData.js'

const all = ref([])
const loading = ref(true)
const filter = ref('ALL') // 'ALL' | 'N' | 'Y'

const filtered = computed(() =>
  filter.value === 'ALL' ? all.value
    : all.value.filter(n => n.isRead === filter.value)
)

async function load() {
  loading.value = true
  try {
    const res = await notificationApi.getNotifications()
    all.value = res?.data?.notifications || mockNotifications
  } catch {
    all.value = mockNotifications
  } finally {
    loading.value = false
  }
}

async function readOne(noti) {
  if (noti.isRead === 'Y') return
  try {
    await notificationApi.readOne(noti.notificationId)
    noti.isRead = 'Y'
  } catch { /* 무시 */ }
}

async function readAll() {
  try {
    await notificationApi.readAll()
    all.value = all.value.map(n => ({ ...n, isRead: 'Y' }))
  } catch (e) {
    alert(e?.message || '처리 실패')
  }
}

function formatTime(dt) {
  if (!dt) return ''
  const ms = Date.now() - new Date(dt).getTime()
  const m = Math.floor(ms / 60000)
  if (m < 1) return '방금 전'
  if (m < 60) return `${m}분 전`
  const h = Math.floor(m / 60)
  if (h < 24) return `${h}시간 전`
  return new Date(dt).toLocaleDateString('ko-KR')
}

const unreadCount = computed(() => all.value.filter(n => n.isRead === 'N').length)

onMounted(load)
</script>

<template>
  <div class="page">
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:4px">
      <h1 class="page-title">알림</h1>
      <button v-if="unreadCount > 0" class="btn btn-ghost btn-sm" @click="readAll">
        <CheckCheck :size="13" /> 전체 읽음
      </button>
    </div>
    <p class="page-subtitle">읽지 않은 알림 {{ unreadCount }}개</p>

    <!-- 필터 -->
    <div class="tabs" style="margin-bottom:16px">
      <div class="tab" :class="{ active: filter==='ALL' }" @click="filter='ALL'">전체</div>
      <div class="tab" :class="{ active: filter==='N' }" @click="filter='N'">읽지 않음</div>
      <div class="tab" :class="{ active: filter==='Y' }" @click="filter='Y'">읽음</div>
    </div>

    <div v-if="loading" class="loading-wrap"><div class="spinner"></div></div>

    <div v-else-if="!filtered.length" class="empty">
      <div class="empty-icon">🔔</div>
      <p class="empty-text">알림이 없습니다</p>
    </div>

    <div v-else role="list">
      <div
        v-for="noti in filtered"
        :key="noti.notificationId"
        class="noti-item"
        :class="{ unread: noti.isRead === 'N' }"
        style="position:relative;cursor:pointer"
        @click="readOne(noti)"
      >
        <div class="noti-icon" :class="noti.notificationType?.toLowerCase()">
          {{ noti.notificationType === 'FRIEND' ? '👥' : '📖' }}
        </div>
        <div style="flex:1">
          <div class="noti-title">{{ noti.title }}</div>
          <div v-if="noti.content" style="font-size:12px;color:var(--color-text-2);margin-top:2px">{{ noti.content }}</div>
          <div class="noti-time">{{ formatTime(noti.createdAt) }}</div>
        </div>
        <div v-if="noti.isRead === 'N'" class="noti-dot"></div>
      </div>
    </div>
  </div>
</template>