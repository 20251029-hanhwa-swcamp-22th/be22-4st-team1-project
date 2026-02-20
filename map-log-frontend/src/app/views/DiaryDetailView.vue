<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { diaryApi } from '@/app/api/diary.js'
import { friendApi } from '@/app/api/friend.js'
import { useAuthStore } from '@/app/stores/auth.js'
import { MapPin, Bookmark, BookmarkCheck, Pencil, Trash2, Share2, ArrowLeft, Image } from 'lucide-vue-next'
import { mockDiaries, mockFriends } from '@/app/data/MockData.js'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const diary = ref(null)
const loading = ref(true)
const isScrapped = ref(false)
const imgIdx = ref(0)

// 공유 모달
const showShare = ref(false)
const friends = ref([])
const selectedFriends = ref([])
const shareLoading = ref(false)

// 수정 모달
const showEdit = ref(false)
const editForm = ref({ title:'', content:'', address:'', locationName:'', newImages:[], deleteImageIds:[], imagePreviews:[] })
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

// 공유
async function openShare() {
  try {
    const res = await friendApi.getFriends()
    friends.value = Array.isArray(res?.data) ? res.data : mockFriends
  } catch {
    friends.value = mockFriends
  }
  showShare.value = true
}

async function doShare() {
  if (!selectedFriends.value.length) { alert('친구를 선택해주세요.'); return }
  shareLoading.value = true
  try {
    await diaryApi.shareDiary(diary.value.id, { friendIds: selectedFriends.value })
    alert('공유 완료!')
    showShare.value = false
    selectedFriends.value = []
  } catch (e) {
    alert(e?.message || '공유 실패')
  } finally {
    shareLoading.value = false
  }
}

// 수정
function openEdit() {
  editForm.value = {
    title: diary.value.title,
    content: diary.value.content,
    address: diary.value.address || '',
    locationName: diary.value.locationName || '',
    visitedAt: diary.value.visitedAt || new Date().toISOString().slice(0, 19),
    visibility: diary.value.visibility || 'PUBLIC',
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
    fd.append('visibility', editForm.value.visibility)
    editForm.value.deleteImageIds.forEach(id => fd.append('deleteImageIds', id))
    editForm.value.newImages.forEach(img => fd.append('images', img))
    await diaryApi.updateDiary(diary.value.id, fd)
    showEdit.value = false
    await load()
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

onMounted(load)
</script>

<template>
  <div v-if="loading" class="loading-wrap"><div class="spinner"></div></div>

  <div v-else-if="diary" style="max-width:720px;margin:0 auto;padding:28px 24px">
    <!-- 뒤로가기 -->
    <button class="btn btn-ghost btn-sm" style="margin-bottom:16px" @click="router.back()">
      <ArrowLeft :size="14" /> 뒤로
    </button>

    <!-- 제목 / 위치 -->
    <div style="margin-bottom:16px">
      <h1 style="font-size:22px;font-weight:700;margin-bottom:6px">{{ diary.title }}</h1>
      <div v-if="diary.locationName" style="display:flex;align-items:center;gap:6px;color:var(--color-text-2);font-size:13px">
        <MapPin :size="13" /> {{ diary.locationName }}
        <span v-if="diary.address" style="color:var(--color-text-3)">· {{ diary.address }}</span>
      </div>
      <div style="font-size:12px;color:var(--color-text-3);margin-top:4px">{{ formatDate(diary.createdAt) }}</div>
    </div>

    <!-- 이미지 갤러리 -->
    <div v-if="diary.images?.length" style="border-radius:var(--radius-lg);overflow:hidden;margin-bottom:20px;position:relative;background:var(--color-bg-3)">
      <img :src="diary.images[imgIdx]?.imageUrl" style="width:100%;max-height:360px;object-fit:cover" />
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
        <button class="btn btn-ghost btn-sm" @click="openShare"><Share2 :size="14" /> 친구 공유</button>
      </template>
      <template v-else>
        <button class="btn btn-sm" :class="isScrapped ? 'btn-success' : 'btn-ghost'" @click="toggleScrap">
          <component :is="isScrapped ? BookmarkCheck : Bookmark" :size="14" />
          {{ isScrapped ? '스크랩됨' : '스크랩' }}
        </button>
      </template>
    </div>
  </div>

  <!-- 공유 모달 -->
  <Teleport to="body">
    <div v-if="showShare" class="modal-backdrop" @click.self="showShare=false">
      <div class="modal">
        <div class="modal-header">
          <span class="modal-title">친구에게 공유</span>
          <button class="modal-close" @click="showShare=false">✕</button>
        </div>
        <div class="modal-body">
          <p class="text-muted text-sm" style="margin-bottom:12px">공유할 친구를 선택하세요</p>
          <div style="display:flex;flex-direction:column;gap:8px;max-height:280px;overflow-y:auto">
            <label v-for="f in friends" :key="f.friendId" style="display:flex;align-items:center;gap:10px;padding:10px;border-radius:var(--radius-md);cursor:pointer;border:1px solid var(--color-border)">
              <input type="checkbox" :value="f.userId" v-model="selectedFriends" style="accent-color:var(--color-primary)" />
              <div class="ml-avatar" style="width:30px;height:30px;font-size:12px">{{ f.nickname.charAt(0) }}</div>
              {{ f.nickname }}
            </label>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-ghost" @click="showShare=false">취소</button>
          <button class="btn btn-primary" :disabled="shareLoading" @click="doShare">공유하기</button>
        </div>
      </div>
    </div>

    <!-- 수정 모달 -->
    <div v-if="showEdit" class="modal-backdrop" @click.self="showEdit=false">
      <div class="modal">
        <div class="modal-header">
          <span class="modal-title">일기 수정</span>
          <button class="modal-close" @click="showEdit=false">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">제목</label>
            <input v-model="editForm.title" type="text" class="form-input" />
          </div>
          <div class="form-group">
            <label class="form-label">내용</label>
            <textarea v-model="editForm.content" class="form-input"></textarea>
          </div>
          <div class="form-group">
            <label class="form-label">위치명</label>
            <input v-model="editForm.locationName" type="text" class="form-input" />
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
                <img :src="img.imageUrl" style="width:60px;height:60px;object-fit:cover;border-radius:var(--radius-sm);opacity:.8" />
              </label>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-ghost" @click="showEdit=false">취소</button>
          <button class="btn btn-primary" :disabled="editLoading" @click="saveEdit">저장</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>