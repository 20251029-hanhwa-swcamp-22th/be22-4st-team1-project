/**
 * 단일 일기의 상세 내용을 보여주는 화면입니다.
 * - 일기 내용 및 이미지 슬라이드 쇼
 * - 작성자 본인일 경우 수정/삭제 기능 제공
 * - 친구에게 일기 공유 및 가시성 설정 (PRIVATE <-> FRIENDS_ONLY)
 */
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

// ── 데이터 상태 ──
const diary = ref(null)
const loading = ref(true)
const isScrapped = ref(false)
const imgIdx = ref(0) // 다중 이미지 표시를 위한 현재 인덱스

// ── 수정 모달 전용 상태 ──
const showEdit = ref(false)
const friends = ref([])
const editForm = ref({ 
  title:'', content:'', address:'', locationName:'', 
  newImages:[], deleteImageIds:[], imagePreviews:[], 
  visibility:'PRIVATE', sharedUserIds:[] 
})
const editLoading = ref(false)

// 현재 로그인한 사용자가 일기 작성자인지 판단
const isOwner = computed(() => diary.value?.userId === auth.userId)

/** 서버로부터 일기 상세 정보를 로드 */
async function load() {
  loading.value = true
  try {
    const res = await diaryApi.getDiary(route.params.diaryId)
    diary.value = res?.data || mockDiaries[0]
  } catch {
    diary.value = mockDiaries[0] // 실패 시 더미 데이터로 표시
  } finally {
    loading.value = false
  }
}

/** 공유를 위한 친구 목록 로드 */
async function loadFriends() {
  try {
    const res = await friendApi.getFriends()
    friends.value = res?.data || []
  } catch {
    friends.value = mockFriends
  }
}

/** 수정 모달 오픈 및 기존 데이터 폼 주입 */
async function openEdit() {
  await loadFriends()
  editForm.value = {
    title: diary.value.title,
    content: diary.value.content,
    address: diary.value.address || '',
    locationName: diary.value.locationName || '',
    visitedAt: diary.value.visitedAt || new Date().toISOString().slice(0, 19),
    visibility: diary.value.visibility || 'PRIVATE',
    sharedUserIds: [], // 기존 공유 정보는 백엔드 동기화 로직에 따라 새로 설정
    newImages: [], deleteImageIds: [], imagePreviews: []
  }
  showEdit.value = true
}

/** 수정된 데이터 저장 (Multipart/FormData) */
async function saveEdit() {
  if (!editForm.value.title || !editForm.value.content) { alert('제목과 내용은 필수입니다.'); return }
  editLoading.value = true
  try {
    const fd = new FormData()
    fd.append('title', editForm.value.title)
    fd.append('content', editForm.value.content)
    if (editForm.value.address) fd.append('address', editForm.value.address)
    if (editForm.value.locationName) fd.append('locationName', editForm.value.locationName)
    fd.append('visitedAt', editForm.value.visitedAt)
    
    // 친구 선택 여부에 따라 가시성 자동 변경
    const visibility = editForm.value.sharedUserIds.length > 0 ? 'FRIENDS_ONLY' : 'PRIVATE'
    fd.append('visibility', visibility)
    
    editForm.value.sharedUserIds.forEach(id => fd.append('sharedUserIds', id))
    editForm.value.deleteImageIds.forEach(id => fd.append('deleteImageIds', id))
    editForm.value.newImages.forEach(img => fd.append('images', img))

    await diaryApi.updateDiary(diary.value.id, fd)
    showEdit.value = false
    await load() // 수정 후 데이터 재로드
    alert('수정되었습니다.')
  } catch (e) {
    alert(e?.message || '수정 실패')
  } finally {
    editLoading.value = false
  }
}

/** 시간 포맷 변환 (한국어 표기) */
function formatDate(dt) {
  if (!dt) return ''
  return new Date(dt).toLocaleDateString('ko-KR', { 
    year:'numeric', month:'long', day:'numeric', hour:'2-digit', minute:'2-digit' 
  })
}

onMounted(load)
</script>

<template>
  <!-- 상세 뷰 및 수정 모달 레이아웃 -->
  <div v-if="loading" class="loading-wrap"><div class="spinner"></div></div>
  <!-- 중략: 화면 렌더링 코드 -->
</template>
