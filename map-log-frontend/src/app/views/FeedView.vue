<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { feedApi } from '@/app/api/feed.js'
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
    const res = await feedApi.getFeed({ page: p, size: 10 })
    feed.value = res?.data?.content || mockFeed
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

    <div v-else style="display:grid;grid-template-columns:repeat(auto-fill,minmax(260px,1fr));gap:16px;margin-bottom:24px">
      <div
        v-for="item in feed"
        :key="item.diaryId"
        class="diary-card"
        @click="router.push(`/diaries/${item.diaryId}`)"
      >
        <div class="diary-card-thumb">
          <img v-if="item.thumbnailUrl" :src="item.thumbnailUrl" :alt="item.title" />
          <div v-else class="diary-card-thumb-placeholder">📖</div>
        </div>
        <div class="diary-card-body">
          <div class="diary-card-title truncate">{{ item.title }}</div>
          <div v-if="item.locationName" class="diary-card-loc">
            <MapPin :size="11" /> {{ item.locationName }}
          </div>
          <div class="diary-card-meta">
            <div class="diary-card-author">
              <div class="ml-avatar" style="width:22px;height:22px;font-size:10px">
                {{ item.author?.nickname?.charAt(0) }}
              </div>
              {{ item.author?.nickname }}
            </div>
            <span class="diary-card-date">{{ formatDate(item.sharedAt) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 페이징 -->
    <div v-if="totalPages > 1" class="pagination">
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