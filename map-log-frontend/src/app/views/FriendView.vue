<script setup>
import { ref, onMounted } from 'vue'
import { friendApi } from '@/app/api/friend.js'
import { userApi } from '@/app/api/user.js'
import { Search, UserCheck, UserPlus, X, Check } from 'lucide-vue-next'
import { mockFriends, mockPending } from '@/app/data/MockData.js'

const tab = ref('friends') // 'friends' | 'pending'
const friends = ref([])
const pending = ref([])
const loading = ref(true)

// 검색
const searchQuery = ref('')
const searchResults = ref([])
const searching = ref(false)

async function load() {
  loading.value = true
  try {
    const [fRes, pRes] = await Promise.all([friendApi.getFriends(), friendApi.getPending()])
    friends.value = Array.isArray(fRes?.data) ? fRes.data : mockFriends
    pending.value = Array.isArray(pRes?.data?.content) ? pRes.data.content : (Array.isArray(pRes?.data) ? pRes.data : mockPending)
  } catch {
    friends.value = mockFriends
    pending.value = mockPending
  } finally {
    loading.value = false
  }
}

async function searchUsers() {
  if (!searchQuery.value.trim()) return
  searching.value = true
  try {
    const res = await userApi.searchUsers(searchQuery.value)
    searchResults.value = Array.isArray(res?.data) ? res.data : [res?.data].filter(Boolean)
  } catch {
    searchResults.value = []
  } finally {
    searching.value = false
  }
}

async function sendRequest(userId) {
  try {
    await friendApi.sendRequest(userId)
    alert('친구 요청을 보냈습니다!')
    searchResults.value = searchResults.value.map(u =>
      u.id === userId ? { ...u, friendStatus: 'PENDING' } : u
    )
  } catch (e) {
    alert(e?.message || '요청 실패')
  }
}

async function respond(friendId, action) {
  try {
    await friendApi.respond(friendId, action)
    await load()
  } catch (e) {
    alert(e?.message || '처리 실패')
  }
}

function statusLabel(status) {
  if (status === 'ACCEPTED') return '친구'
  if (status === 'PENDING') return '요청 중'
  return '친구 추가'
}

onMounted(load)
</script>

<template>
  <div class="page">
    <h1 class="page-title">친구</h1>
    <p class="page-subtitle">친구를 추가하고 관리하세요</p>

    <!-- 사용자 검색 -->
    <div class="card" style="margin-bottom:20px">
      <p style="font-size:13px;font-weight:600;margin-bottom:10px">닉네임으로 친구 찾기</p>
      <div style="display:flex;gap:8px">
        <input v-model="searchQuery" type="text" class="form-input" placeholder="닉네임 검색" @keyup.enter="searchUsers" />
        <button class="btn btn-primary" @click="searchUsers" :disabled="searching">
          <Search :size="15" />
        </button>
      </div>
      <div v-if="searchResults.length" style="margin-top:12px;display:flex;flex-direction:column;gap:8px">
        <div v-for="u in searchResults" :key="u.id" style="display:flex;align-items:center;gap:10px;padding:10px;border-radius:var(--radius-md);background:var(--color-bg-3)">
          <div class="ml-avatar" style="width:34px;height:34px">{{ u.nickname.charAt(0) }}</div>
          <div style="flex:1">
            <div style="font-size:13px;font-weight:600">{{ u.nickname }}</div>
          </div>
          <button
            class="btn btn-sm"
            :class="u.friendStatus === 'ACCEPTED' ? 'btn-success' : u.friendStatus === 'PENDING' ? 'btn-ghost' : 'btn-primary'"
            :disabled="u.friendStatus === 'ACCEPTED' || u.friendStatus === 'PENDING'"
            @click="sendRequest(u.id)"
          >
            <UserCheck v-if="u.friendStatus==='ACCEPTED'" :size="13" />
            <UserPlus v-else :size="13" />
            {{ statusLabel(u.friendStatus) }}
          </button>
        </div>
      </div>
    </div>

    <!-- 탭 -->
    <div class="tabs">
      <div class="tab" :class="{ active: tab==='friends' }" @click="tab='friends'">
        친구 목록 ({{ friends.length }})
      </div>
      <div class="tab" :class="{ active: tab==='pending' }" @click="tab='pending'">
        받은 요청 <span v-if="pending.length" class="badge badge-danger" style="margin-left:6px">{{ pending.length }}</span>
      </div>
    </div>

    <div v-if="loading" class="loading-wrap"><div class="spinner"></div></div>

    <!-- 친구 목록 -->
    <template v-else-if="tab==='friends'">
      <div v-if="!friends.length" class="empty">
        <div class="empty-icon">👥</div>
        <p class="empty-text">아직 친구가 없습니다</p>
      </div>
      <div v-else style="display:flex;flex-direction:column;gap:8px">
        <div v-for="f in friends" :key="f.friendId" class="card" style="display:flex;align-items:center;gap:12px">
          <div class="ml-avatar" style="width:40px;height:40px">
            <img v-if="f.profileImageUrl" :src="f.profileImageUrl" :alt="f.nickname" />
            <span v-else>{{ f.nickname.charAt(0) }}</span>
          </div>
          <div>
            <div style="font-size:14px;font-weight:600">{{ f.nickname }}</div>
            <div style="font-size:11px;color:var(--color-text-3)">친구 수락: {{ new Date(f.respondedAt).toLocaleDateString('ko-KR') }}</div>
          </div>
          <span class="badge badge-success ml-auto">친구</span>
        </div>
      </div>
    </template>

    <!-- 받은 친구 요청 -->
    <template v-else>
      <div v-if="!pending.length" class="empty">
        <div class="empty-icon">📭</div>
        <p class="empty-text">받은 친구 요청이 없습니다</p>
      </div>
      <div v-else style="display:flex;flex-direction:column;gap:8px">
        <div v-for="p in pending" :key="p.friendId" class="card" style="display:flex;align-items:center;gap:12px">
          <div class="ml-avatar" style="width:40px;height:40px">
            <span>{{ (p.requesterNickname || p.nickname || '?').charAt(0) }}</span>
          </div>
          <div style="flex:1">
            <div style="font-size:14px;font-weight:600">{{ p.requesterNickname || p.nickname }}</div>
            <div style="font-size:11px;color:var(--color-text-3)">{{ new Date(p.requestedAt).toLocaleDateString('ko-KR') }}</div>
          </div>
          <button class="btn btn-success btn-sm" @click="respond(p.friendId, 'ACCEPTED')">
            <Check :size="13" /> 수락
          </button>
          <button class="btn btn-ghost btn-sm" @click="respond(p.friendId, 'REJECTED')">
            <X :size="13" /> 거절
          </button>
        </div>
      </div>
    </template>
  </div>
</template>