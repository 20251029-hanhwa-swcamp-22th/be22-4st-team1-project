<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/app/stores/auth.js'
import { userApi } from '@/app/api/user.js'
import { MapPin, Camera } from 'lucide-vue-next'
import { mockUser, mockDiaries } from '@/app/data/MockData.js'

const auth = useAuthStore()
const router = useRouter()

const tab = ref('diaries') // 'diaries' | 'scraps'
const me = ref(null)
const diaries = ref([])
const scraps = ref([])
const loading = ref(true)

// 프로필 수정 모달
const showEdit = ref(false)
const editForm = ref({ nickname: '', profileImage: null, previewUrl: null })
const editLoading = ref(false)

// 탈퇴 확인
const showWithdraw = ref(false)

async function load() {
  loading.value = true
  try {
    const [meRes, dRes, sRes] = await Promise.all([
      userApi.getMe(),
      userApi.getMyDiaries(),
      userApi.getMyScraps()
    ])
    me.value = meRes?.data || mockUser
    diaries.value = dRes?.data?.content || mockDiaries
    scraps.value = sRes?.data?.content || []
  } catch {
    me.value = mockUser
    diaries.value = mockDiaries
    scraps.value = []
  } finally {
    loading.value = false
  }
}

function openEdit() {
  editForm.value = { nickname: me.value.nickname, profileImage: null, previewUrl: me.value.profileImageUrl }
  showEdit.value = true
}

function onProfileImage(e) {
  const file = e.target.files[0]
  if (!file) return
  editForm.value.profileImage = file
  const reader = new FileReader()
  reader.onload = ev => { editForm.value.previewUrl = ev.target.result }
  reader.readAsDataURL(file)
}

async function saveProfile() {
  editLoading.value = true
  try {
    const fd = new FormData()
    if (editForm.value.nickname !== me.value.nickname) fd.append('nickname', editForm.value.nickname)
    if (editForm.value.profileImage) fd.append('profileImage', editForm.value.profileImage)
    const res = await userApi.updateMe(fd)
    auth.updateUser({
      nickname: res?.data?.nickname || editForm.value.nickname,
      profileImageUrl: res?.data?.profileImageUrl || editForm.value.previewUrl
    })
    me.value = { ...me.value, ...res?.data }
    showEdit.value = false
  } catch (e) {
    alert(e?.message || '수정 실패')
  } finally {
    editLoading.value = false
  }
}

async function withdraw() {
  try {
    await userApi.deleteMe()
    auth.clear()
    router.push('/login')
  } catch (e) {
    alert(e?.message || '탈퇴 처리 실패')
  }
}

function formatDate(dt) {
  if (!dt) return ''
  return new Date(dt).toLocaleDateString('ko-KR', { month:'short', day:'numeric' })
}

onMounted(load)
</script>

<template>
  <div class="page">
    <h1 class="page-title">마이페이지</h1>

    <div v-if="loading" class="loading-wrap"><div class="spinner"></div></div>

    <template v-else-if="me">
      <!-- 프로필 카드 -->
      <div class="card" style="display:flex;align-items:center;gap:20px;margin-bottom:20px">
        <div class="ml-avatar" style="width:64px;height:64px;font-size:24px;flex-shrink:0">
          <img v-if="me.profileImageUrl" :src="me.profileImageUrl" :alt="me.nickname" />
          <span v-else>{{ me.nickname?.charAt(0) }}</span>
        </div>
        <div style="flex:1">
          <div style="font-size:18px;font-weight:700">{{ me.nickname }}</div>
          <div style="font-size:13px;color:var(--color-text-2)">{{ me.email }}</div>
          <div style="font-size:12px;color:var(--color-text-3);margin-top:4px">
            가입일 {{ new Date(me.createdAt).toLocaleDateString('ko-KR') }}
          </div>
        </div>
        <div style="display:flex;flex-direction:column;gap:6px">
          <button class="btn btn-ghost btn-sm" @click="openEdit"><Camera :size="13" /> 수정</button>
          <button class="btn btn-danger btn-sm" @click="showWithdraw=true">탈퇴</button>
        </div>
      </div>

      <!-- 탭 -->
      <div class="tabs">
        <div class="tab" :class="{ active: tab==='diaries' }" @click="tab='diaries'">
          내 일기 ({{ diaries.length }})
        </div>
        <div class="tab" :class="{ active: tab==='scraps' }" @click="tab='scraps'">
          스크랩 ({{ scraps.length }})
        </div>
      </div>

      <!-- 내 일기 -->
      <template v-if="tab==='diaries'">
        <div v-if="!diaries.length" class="empty"><div class="empty-icon">📝</div><p class="empty-text">아직 일기가 없습니다</p></div>
        <div v-else style="display:grid;grid-template-columns:repeat(auto-fill,minmax(220px,1fr));gap:12px">
          <div v-for="d in diaries" :key="d.diaryId" class="diary-card" @click="router.push(`/diaries/${d.diaryId}`)">
            <div class="diary-card-thumb">
              <img v-if="d.thumbnailUrl" :src="d.thumbnailUrl" />
              <div v-else class="diary-card-thumb-placeholder">📖</div>
            </div>
            <div class="diary-card-body">
              <div class="diary-card-title truncate">{{ d.title }}</div>
              <div v-if="d.locationName" class="diary-card-loc"><MapPin :size="11" />{{ d.locationName }}</div>
              <div class="diary-card-date" style="margin-top:6px">{{ formatDate(d.createdAt) }}</div>
            </div>
          </div>
        </div>
      </template>

      <!-- 스크랩 -->
      <template v-else>
        <div v-if="!scraps.length" class="empty"><div class="empty-icon">🔖</div><p class="empty-text">스크랩한 일기가 없습니다</p></div>
        <div v-else style="display:grid;grid-template-columns:repeat(auto-fill,minmax(220px,1fr));gap:12px">
          <div v-for="s in scraps" :key="s.scrapId" class="diary-card" @click="router.push(`/diaries/${s.diaryId}`)">
            <div class="diary-card-thumb">
              <img v-if="s.thumbnailUrl" :src="s.thumbnailUrl" />
              <div v-else class="diary-card-thumb-placeholder">📖</div>
            </div>
            <div class="diary-card-body">
              <div class="diary-card-title truncate">{{ s.title }}</div>
              <div class="diary-card-author"><div class="ml-avatar" style="width:18px;height:18px;font-size:9px">{{ s.authorNickname?.charAt(0) }}</div>{{ s.authorNickname }}</div>
              <div class="diary-card-date" style="margin-top:6px">{{ formatDate(s.scrappedAt) }}</div>
            </div>
          </div>
        </div>
      </template>
    </template>

    <!-- 프로필 수정 모달 -->
    <Teleport to="body">
      <div v-if="showEdit" class="modal-backdrop" @click.self="showEdit=false">
        <div class="modal">
          <div class="modal-header"><span class="modal-title">프로필 수정</span><button class="modal-close" @click="showEdit=false">✕</button></div>
          <div class="modal-body">
            <!-- 프로필 이미지 -->
            <div style="text-align:center;margin-bottom:16px">
              <div class="ml-avatar" style="width:72px;height:72px;font-size:28px;margin:0 auto 10px;cursor:pointer" @click="$refs.profileInput.click()">
                <img v-if="editForm.previewUrl" :src="editForm.previewUrl" />
                <span v-else>{{ editForm.nickname?.charAt(0) }}</span>
              </div>
              <button class="btn btn-ghost btn-sm" @click="$refs.profileInput.click()"><Camera :size="12" /> 사진 변경</button>
              <input ref="profileInput" type="file" accept="image/*" style="display:none" @change="onProfileImage" />
            </div>
            <div class="form-group">
              <label class="form-label">닉네임</label>
              <input v-model="editForm.nickname" type="text" class="form-input" minlength="2" maxlength="20" />
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn btn-ghost" @click="showEdit=false">취소</button>
            <button class="btn btn-primary" :disabled="editLoading" @click="saveProfile">저장</button>
          </div>
        </div>
      </div>

      <!-- 탈퇴 확인 모달 -->
      <div v-if="showWithdraw" class="modal-backdrop" @click.self="showWithdraw=false">
        <div class="modal" style="max-width:380px">
          <div class="modal-body" style="text-align:center;padding:32px 24px">
            <div style="font-size:40px;margin-bottom:12px">⚠️</div>
            <h3 style="margin-bottom:8px">정말 탈퇴하시겠습니까?</h3>
            <p style="color:var(--color-text-2);font-size:13px">탈퇴 후에는 모든 데이터가 삭제됩니다.</p>
          </div>
          <div class="modal-footer">
            <button class="btn btn-ghost" @click="showWithdraw=false">취소</button>
            <button class="btn btn-danger" @click="withdraw">탈퇴하기</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>