<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { diaryApi } from '@/app/api/diary.js'
import { MapPin } from 'lucide-vue-next'
import { mockFeed } from '@/app/data/MockData.js'

const router = useRouter()

const feed = ref([])
const loading = ref(true)
const page = ref(0)
const totalPages = ref(0)

async function load(p = 0) {
  loading.value = true
  try {
    const res = await diaryApi.getFeed({ page: p, size: 10 })
    feed.value = res?.data?.content || []
    totalPages.value = res?.data?.totalPages || 1
    page.value = p
  } catch {
    feed.value = mockFeed
  } finally {
    loading.value = false
  }
}

function formatDate(dt) {
  if (!dt) return ''
  return new Date(dt).toLocaleDateString('ko-KR', { month:'short', day:'numeric' })
}

onMounted(() => load(0))
</script>

<template>
  <div class="page">
    <h1 class="page-title">피드</h1>
    <p class="page-subtitle">친구들이 공유한 일기를 확인하세요</p>

    <div v-if="loading" class="loading-wrap"><div class="spinner"></div></div>

    <div v-else-if="!feed.length" class="empty">
      <div class="empty-icon">🌿</div>
      <p class="empty-text">아직 공유받은 일기가 없습니다.<br>친구를 추가해보세요!</p>
    </div>

    <div v-else style="display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:20px;margin-bottom:24px">
      <div
        v-for="item in feed"
        :key="item.id"
        class="card"
        style="cursor:pointer;padding:0;overflow:hidden;display:flex;flex-direction:column"
        @click="router.push(`/diaries/${item.id}`)"
      >
        <div style="height:160px;background:var(--color-bg-3);display:flex;align-items:center;justify-content:center;font-size:40px">
          📖
        </div>
        <div style="padding:16px">
          <div style="font-weight:700;font-size:16px;margin-bottom:6px" class="truncate">{{ item.title }}</div>
          <div v-if="item.locationName" style="font-size:12px;color:var(--color-text-2);display:flex;align-items:center;gap:4px;margin-bottom:12px">
            <MapPin :size="12" /> {{ item.locationName }}
          </div>
          <div style="display:flex;align-items:center;justify-content:space-between;margin-top:auto">
            <div style="display:flex;align-items:center;gap:6px;font-size:12px;font-weight:600">
              <div class="ml-avatar" style="width:24px;height:24px;font-size:11px">
                {{ item.authorNickname?.charAt(0) }}
              </div>
              {{ item.authorNickname }}
            </div>
            <span style="font-size:11px;color:var(--color-text-3)">{{ formatDate(item.createdAt) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 페이징 -->
    <div v-if="totalPages > 1" class="pagination" style="margin-top:20px">
      <button class="page-btn" :disabled="page===0" @click="load(page-1)">‹</button>
      <button
        v-for="p in totalPages" :key="p"
        class="page-btn"
        :class="{ current: p-1 === page }"
        @click="load(p-1)"
      >{{ p }}</button>
      <button class="page-btn" :disabled="page===totalPages-1" @click="load(page+1)">›</button>
    </div>
  </div>
</template>