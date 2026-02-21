<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { diaryApi } from '@/app/api/diary.js'
import { friendApi } from '@/app/api/friend.js'
import { useAuthStore } from '@/app/stores/auth.js'
import { MapPin, Bookmark, BookmarkCheck, Pencil, Trash2, ArrowLeft, Image, Users, Lock, Users2 } from 'lucide-vue-next'
import { mockDiaries, mockFriends } from '@/app/data/MockData.js'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const diary = ref(null)
const loading = ref(true)
const isScrapped = ref(false)
const imgIdx = ref(0)

// 수정 모달
const showEdit = ref(false)
const friends = ref([])
const editForm = ref({ title:'', content:'', address:'', locationName:'', newImages:[], deleteImageIds:[], imagePreviews:[], visibility:'PRIVATE', sharedUserIds:[] })
const editLoading = ref(false)

const isOwner = computed(() => diary.value?.userId === auth.userId)

async function load() {
  loading.value = true
  try {
    const res = await diaryApi.getDiary(route.params.diaryId)
    diary.value = res?.data || mockDiaries[0]
  } catch {
    diary.value = mockDiaries[0]
  } finally {
    loading.value = false
  }
}

async function loadFriends() {
  try {
    const res = await friendApi.getFriends()
    friends.value = Array.isArray(res?.data) ? res.data : mockFriends
  } catch {
    friends.value = mockFriends
  }
}

async function toggleScrap() {
  try {
    if (isScrapped.value) {
      await diaryApi.removeScrap(diary.value.id)
    } else {
      await diaryApi.addScrap(diary.value.id)
    }
    isScrapped.value = !isScrapped.value
  } catch (e) {
    alert(e?.message || '스크랩 처리 실패')
  }
}

async function deleteDiary() {
  if (!confirm('정말 삭제하시겠습니까?')) return
  try {
    await diaryApi.deleteDiary(diary.value.id)
    router.push('/map')
  } catch (e) {
    alert(e?.message || '삭제 실패')
  }
}

// 수정
async function openEdit() {
  await loadFriends()
  editForm.value = {
    title: diary.value.title,
    content: diary.value.content,
    address: diary.value.address || '',
    locationName: diary.value.locationName || '',
    visitedAt: diary.value.visitedAt || new Date().toISOString().slice(0, 19),
    visibility: diary.value.visibility || 'PRIVATE',
    // 기존 공유 목록이 현재 DTO에 포함되어 있지 않으므로, 백엔드 로직에 따라 초기화
    sharedUserIds: [], 
    newImages: [], deleteImageIds: [], imagePreviews: []
  }
  showEdit.value = true
}

async function saveEdit() {
  if (!editForm.value.title || !editForm.value.content) { alert('제목/내용 필수'); return }
  editLoading.value = true
  try {
    const fd = new FormData()
    fd.append('title', editForm.value.title)
    fd.append('content', editForm.value.content)
    if (editForm.value.address) fd.append('address', editForm.value.address)
    if (editForm.value.locationName) fd.append('locationName', editForm.value.locationName)
    fd.append('visitedAt', editForm.value.visitedAt)
    
    const visibility = editForm.value.sharedUserIds.length > 0 ? 'FRIENDS_ONLY' : 'PRIVATE'
    fd.append('visibility', visibility)
    
    editForm.value.sharedUserIds.forEach(id => fd.append('sharedUserIds', id))
    editForm.value.deleteImageIds.forEach(id => fd.append('deleteImageIds', id))
    editForm.value.newImages.forEach(img => fd.append('images', img))

    await diaryApi.updateDiary(diary.value.id, fd)
    showEdit.value = false
    await load()
    alert('수정되었습니다.')
  } catch (e) {
    alert(e?.message || '수정 실패')
  } finally {
    editLoading.value = false
  }
}

function onEditImage(e) {
  const files = Array.from(e.target.files)
  files.forEach(file => {
    editForm.value.newImages.push(file)
    const r = new FileReader()
    r.onload = ev => editForm.value.imagePreviews.push(ev.target.result)
    r.readAsDataURL(file)
  })
}

function formatDate(dt) {
  if (!dt) return ''
  return new Date(dt).toLocaleDateString('ko-KR', { year:'numeric', month:'long', day:'numeric', hour:'2-digit', minute:'2-digit' })
}

function toImageUrl(path) {
  if (!path) return ''
  if (path.startsWith('http://') || path.startsWith('https://')) return path
  const base = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  return `${base}${path.startsWith('/') ? '' : '/'}${path}`
}

onMounted(load)
</script>

<template>
  <div v-if="loading" class="loading-wrap"><div class="spinner"></div></div>

  <div v-else-if="diary" style="max-width:720px;margin:0 auto;padding:28px 24px">
    <!-- 뒤로가기 -->
    <button class="btn btn-ghost btn-sm" style="margin-bottom:16px" @click="router.back()">
      <ArrowLeft :size="14" /> 뒤로
    </button>

    <!-- 제목 / 위치 / 가시성 -->
    <div style="margin-bottom:16px">
      <div style="display:flex;align-items:center;justify-content:space-between">
        <h1 style="font-size:22px;font-weight:700;margin-bottom:6px">{{ diary.title }}</h1>
        <div style="display:flex;align-items:center;gap:4px;font-size:12px;color:var(--color-text-3)">
          <template v-if="diary.visibility === 'PRIVATE'">
            <Lock :size="12" /> 나만보기
          </template>
          <template v-else>
            <Users2 :size="12" /> 친구공유
          </template>
        </div>
      </div>
      <div v-if="diary.locationName" style="display:flex;align-items:center;gap:6px;color:var(--color-text-2);font-size:13px">
        <MapPin :size="13" /> {{ diary.locationName }}
        <span v-if="diary.address" style="color:var(--color-text-3)">· {{ diary.address }}</span>
      </div>
      <div style="font-size:12px;color:var(--color-text-3);margin-top:4px">{{ formatDate(diary.createdAt) }}</div>
    </div>

    <!-- 이미지 갤러리 -->
    <div v-if="diary.images?.length" style="border-radius:var(--radius-lg);overflow:hidden;margin-bottom:20px;position:relative;background:var(--color-bg-3)">
      <img :src="toImageUrl(diary.images[imgIdx]?.imageUrl)" style="width:100%;max-height:360px;object-fit:cover" />
      <div v-if="diary.images.length > 1" style="position:absolute;bottom:10px;left:0;right:0;display:flex;justify-content:center;gap:6px">
        <button
          v-for="(_, i) in diary.images" :key="i"
          :style="`width:8px;height:8px;border-radius:50%;border:none;cursor:pointer;background:${i===imgIdx?'#fff':'rgba(255,255,255,.5)'}`"
          @click="imgIdx=i"
        />
      </div>
    </div>
    <div v-else style="height:120px;border-radius:var(--radius-lg);background:var(--color-bg-3);display:flex;align-items:center;justify-content:center;margin-bottom:20px;color:var(--color-text-3)">
      <Image :size="32" />
    </div>

    <!-- 본문 -->
    <div class="card" style="margin-bottom:20px">
      <p style="white-space:pre-wrap;line-height:1.8;font-size:14px">{{ diary.content }}</p>
    </div>

    <!-- 액션 버튼 -->
    <div style="display:flex;gap:10px;flex-wrap:wrap">
      <template v-if="isOwner">
        <button class="btn btn-ghost btn-sm" @click="openEdit"><Pencil :size="14" /> 수정</button>
        <button class="btn btn-danger btn-sm" @click="deleteDiary"><Trash2 :size="14" /> 삭제</button>
      </template>
      <template v-else>
        <button class="btn btn-sm" :class="isScrapped ? 'btn-success' : 'btn-ghost'" @click="toggleScrap">
          <component :is="isScrapped ? BookmarkCheck : Bookmark" :size="14" />
          {{ isScrapped ? '스크랩됨' : '스크랩' }}
        </button>
      </template>
    </div>
  </div>

  <!-- 수정 모달 -->
  <Teleport to="body">
    <div v-if="showEdit" class="modal-backdrop" @click.self="showEdit=false">
      <div class="modal" style="max-width:560px">
        <div class="modal-header">
          <span class="modal-title">📝 일기 수정</span>
          <button class="modal-close" @click="showEdit=false">✕</button>
        </div>
        <div class="modal-body" style="max-height:70vh;overflow-y:auto">
          <div class="form-group">
            <label class="form-label">제목</label>
            <input v-model="editForm.title" type="text" class="form-input" />
          </div>
          <div class="form-group">
            <label class="form-label">내용</label>
            <textarea v-model="editForm.content" class="form-input" style="min-height:120px"></textarea>
          </div>
          
          <!-- 친구 공유 선택 -->
          <div class="form-group">
            <label class="form-label" style="display:flex;align-items:center;gap:6px">
              <Users :size="14" /> 친구와 공유하기
            </label>
            <div style="display:flex;flex-direction:column;gap:6px;max-height:160px;overflow-y:auto;padding:8px;border:1px solid var(--color-border);border-radius:var(--radius-md)">
              <label v-for="f in friends" :key="f.userId" style="display:flex;align-items:center;gap:10px;padding:6px;cursor:pointer">
                <input type="checkbox" :value="f.userId" v-model="editForm.sharedUserIds" />
                <div class="ml-avatar" style="width:24px;height:24px;font-size:10px">{{ f.nickname.charAt(0) }}</div>
                <span style="font-size:13px">{{ f.nickname }}</span>
              </label>
            </div>
            <p class="text-xs text-muted mt-2">* 공유 중인 친구를 체크 해제하면 해당 친구의 접근 권한이 삭제됩니다.</p>
          </div>

          <div class="form-group">
            <label class="form-label">사진 추가</label>
            <div class="img-upload-zone" @click="$refs.editFileInput.click()">클릭하여 추가</div>
            <input ref="editFileInput" type="file" accept="image/*" multiple style="display:none" @change="onEditImage" />
            <div class="img-preview-list">
              <div v-for="(src,i) in editForm.imagePreviews" :key="i" class="img-preview-item">
                <img :src="src" />
              </div>
            </div>
          </div>

          <!-- 기존 이미지 삭제 선택 -->
          <div v-if="diary.images?.length" class="form-group">
            <label class="form-label">기존 사진 삭제 선택</label>
            <div style="display:flex;gap:8px;flex-wrap:wrap">
              <label v-for="img in diary.images" :key="img.imageId" style="position:relative;cursor:pointer">
                <input type="checkbox" :value="img.imageId" v-model="editForm.deleteImageIds" style="position:absolute;top:4px;left:4px;accent-color:var(--color-danger)" />
                <img :src="toImageUrl(img.imageUrl)" style="width:60px;height:60px;object-fit:cover;border-radius:var(--radius-sm);opacity:.8" />
              </label>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-ghost" @click="showEdit=false">취소</button>
          <button class="btn btn-primary" :disabled="editLoading" @click="saveEdit">
            <span v-if="editLoading" class="spinner" style="width:14px;height:14px;border-width:2px"></span>
            <span v-else>저장</span>
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
