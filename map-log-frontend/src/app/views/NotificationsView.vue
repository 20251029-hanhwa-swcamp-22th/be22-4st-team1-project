<script setup>
import { ref, computed, onMounted } from 'vue'
import { useNotificationStore } from '@/app/stores/notification.js'
import { CheckCheck, Trash2 } from 'lucide-vue-next'

const notificationStore = useNotificationStore()
const filter = ref('ALL') // 'ALL' | 'N' | 'Y'

const filtered = computed(() => {
  const all = notificationStore.notifications
  if (filter.value === 'ALL') return all
  if (filter.value === 'N') return all.filter(n => !n.read)
  if (filter.value === 'Y') return all.filter(n => n.read)
  return all
})

async function load() {
  await notificationStore.fetchNotifications()
}

async function readOne(noti) {
  if (noti.read) return
  await notificationStore.markAsRead(noti.id)
}

async function readAll() {
  await notificationStore.markAllAsRead()
}

async function deleteFiltered() {
  if (!filtered.value.length) return
  if (!confirm('현재 목록의 알림을 모두 삭제하시겠습니까?')) return
  
  const isRead = filter.value === 'ALL' ? undefined : filter.value === 'Y' ? 'Y' : 'N'
  await notificationStore.deleteNotifications(isRead)
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

onMounted(load)
</script>

<template>
  <div class="page">
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:4px">
      <h1 class="page-title">알림</h1>
      <button v-if="notificationStore.unreadCount > 0" class="btn btn-ghost btn-sm" @click="readAll">
        <CheckCheck :size="13" /> 전체 읽음
      </button>
    </div>
    <p class="page-subtitle">읽지 않은 알림 {{ notificationStore.unreadCount }}개</p>

    <!-- 필터 -->
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px">
      <div class="tabs" style="margin-bottom:0">
        <div class="tab" :class="{ active: filter==='ALL' }" @click="filter='ALL'">전체</div>
        <div class="tab" :class="{ active: filter==='N' }" @click="filter='N'">읽지 않음</div>
        <div class="tab" :class="{ active: filter==='Y' }" @click="filter='Y'">읽음</div>
      </div>
      <button class="btn btn-ghost btn-sm" style="color:var(--color-danger)" :disabled="!filtered.length" @click="deleteFiltered">
        <Trash2 :size="13" /> 일괄 삭제
      </button>
    </div>

    <div v-if="notificationStore.loading" class="loading-wrap"><div class="spinner"></div></div>

    <div v-else-if="!filtered.length" class="empty">
      <div class="empty-icon">🔔</div>
      <p class="empty-text">알림이 없습니다</p>
    </div>

    <div v-else role="list">
      <div
        v-for="noti in filtered"
        :key="noti.id"
        class="noti-item"
        :class="{ unread: !noti.read, read: noti.read }"
        style="position:relative;cursor:pointer"
        @click="readOne(noti)"
      >
        <div class="noti-icon" :class="noti.type?.toLowerCase()">
          {{ noti.type === 'FRIEND_REQUEST' || noti.type === 'FRIEND_ACCEPTED' ? '👥' : '📖' }}
        </div>
        <div style="flex:1">
          <div class="noti-title">{{ noti.message }}</div>
          <div class="noti-time">{{ formatTime(noti.createdAt) }}</div>
        </div>
        <div v-if="!noti.read" class="noti-dot"></div>
      </div>
    </div>
  </div>
</template>