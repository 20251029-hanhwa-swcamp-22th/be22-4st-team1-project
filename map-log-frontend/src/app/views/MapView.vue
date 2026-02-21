<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { diaryApi } from '@/app/api/diary.js'
import { friendApi } from '@/app/api/friend.js'
import { X, Image, MapPin, Plus } from 'lucide-vue-next'
import { mockMarkers } from '@/app/data/MockData.js'

const router = useRouter()

// ── 지도 상태 ──
const mapContainer = ref(null)
let map = null
let markers = []
const kakaoKey = import.meta.env.VITE_KAKAO_MAP_KEY
const mapReady = ref(false)
const mapError = ref('')

// ── 일기 작성 모달 ──
const showModal = ref(false)
const selectedLocation = ref(null)
const form = ref({
  title: '',
  content: '',
  images: [],
  imagePreviews: [],
  latitude: null,
  longitude: null,
  locationName: '',
  address: '',
  visibility: 'PUBLIC'
})
const loading = ref(false)
const error = ref('')

// ── 마커 팝업 ──
const popup = ref(null)

// ── 카카오맵 로드 ──
function loadKakaoMap() {
  if (!kakaoKey || kakaoKey === '발급받은_카카오맵_JavaScript_키_입력') {
    mapError.value = '.env.local에 VITE_KAKAO_MAP_KEY를 입력해주세요.'
    return
  }

  return new Promise((resolve, reject) => {
    if (window.kakao?.maps) { resolve(); return }

    const script = document.createElement('script')
    script.src = `//dapi.kakao.com/v2/maps/sdk.js?appkey=${kakaoKey}&autoload=false`
    script.onload = () => {
      window.kakao.maps.load(() => resolve())
    }
    script.onerror = () => reject(new Error('카카오맵 SDK 로드 실패'))
    document.head.appendChild(script)
  })
}

function initMap() {
  const center = new window.kakao.maps.LatLng(37.5665, 126.9780)
  map = new window.kakao.maps.Map(mapContainer.value, {
    center,
    level: 5
  })

  // 지도 클릭 시 위치 선택
  window.kakao.maps.event.addListener(map, 'click', (e) => {
    const lat = e.latLng.getLat()
    const lng = e.latLng.getLng()
    selectedLocation.value = { lat, lng }
    form.value.latitude = lat
    form.value.longitude = lng
    // 간단히 좌표로 주소 표시 (실제는 좌표→주소 변환 API 사용)
    form.value.locationName = `위도 ${lat.toFixed(4)}, 경도 ${lng.toFixed(4)}`
    showModal.value = true
  })

  // 지도 영역 변경 시 마커 갱신
  window.kakao.maps.event.addListener(map, 'idle', loadMarkers)

  loadMarkers()
}

async function loadMarkers() {
  if (!map) return
  const bounds = map.getBounds()
  const sw = bounds.getSouthWest()
  const ne = bounds.getNorthEast()

  try {
    const res = await diaryApi.getMapMarkers({
      swLat: sw.getLat(), swLng: sw.getLng(),
      neLat: ne.getLat(), neLng: ne.getLng()
    })
    const list = Array.isArray(res?.data) ? res.data : []
    renderMarkers(list)
  } catch {
    renderMarkers(mockMarkers)
  }
}

function renderMarkers(list) {
  // 기존 마커 제거
  markers.forEach(m => m.setMap(null))
  markers = []

  list.forEach(item => {
    const pos = new window.kakao.maps.LatLng(item.latitude, item.longitude)
    const marker = new window.kakao.maps.Marker({ position: pos, map })

    window.kakao.maps.event.addListener(marker, 'click', () => {
      popup.value = item
    })

    markers.push(marker)
  })
}

// ── 이미지 업로드 ──
function onImageChange(e) {
  const files = Array.from(e.target.files)
  if (form.value.images.length + files.length > 5) {
    alert('이미지는 최대 5장까지 첨부할 수 있습니다.')
    return
  }
  files.forEach(file => {
    form.value.images.push(file)
    const reader = new FileReader()
    reader.onload = evt => form.value.imagePreviews.push(evt.target.result)
    reader.readAsDataURL(file)
  })
}

function removeImage(idx) {
  form.value.images.splice(idx, 1)
  form.value.imagePreviews.splice(idx, 1)
}

// ── 일기 저장 ──
async function saveDiary() {
  error.value = ''
  if (!form.value.title.trim() || !form.value.content.trim()) {
    error.value = '제목과 내용을 입력해주세요.'
    return
  }
  loading.value = true
  try {
    const fd = new FormData()
    fd.append('title', form.value.title)
    fd.append('content', form.value.content)
    if (form.value.latitude) fd.append('latitude', form.value.latitude)
    if (form.value.longitude) fd.append('longitude', form.value.longitude)
    if (form.value.locationName) fd.append('locationName', form.value.locationName)
    if (form.value.address) fd.append('address', form.value.address)
    fd.append('visitedAt', new Date().toISOString().slice(0, 19))
    fd.append('visibility', form.value.visibility)
    form.value.images.forEach(img => fd.append('images', img))

    const res = await diaryApi.createDiary(fd)
    closeModal()
    loadMarkers()
    alert(`일기가 작성되었습니다! (ID: ${res?.data})`)
  } catch (e) {
    error.value = e?.message || '일기 작성에 실패했습니다.'
  } finally {
    loading.value = false
  }
}

function closeModal() {
  showModal.value = false
  form.value = { title:'',content:'',images:[],imagePreviews:[],latitude:null,longitude:null,locationName:'',address:'',visibility:'PUBLIC' }
  error.value = ''
}

onMounted(async () => {
  try {
    await loadKakaoMap()
    initMap()
    mapReady.value = true
  } catch (e) {
    mapError.value = e.message
  }
})

onUnmounted(() => {
  markers.forEach(m => m.setMap(null))
})
</script>

<template>
  <div style="position:relative;width:100%;height:100vh;overflow:hidden;">

    <!-- 카카오맵 -->
    <div ref="mapContainer" style="width:100%;height:100%;background:var(--color-bg-3)"></div>

    <!-- 지도 키 없을 때 안내 -->
    <div v-if="mapError" style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center;background:var(--color-bg-2);flex-direction:column;gap:16px">
      <div style="font-size:48px">🗺️</div>
      <p style="color:var(--color-text-2);text-align:center;line-height:1.8">
        {{ mapError }}<br>
        <span style="font-size:12px;color:var(--color-text-3)">
          지도 없이도 마커 목록은 아래에 표시됩니다.
        </span>
      </p>
      <!-- 목업 마커 목록 -->
      <div style="display:flex;flex-direction:column;gap:8px;max-height:300px;overflow-y:auto;width:320px">
        <div
          v-for="m in mockMarkers" :key="m.id"
          class="card"
          style="cursor:pointer;display:flex;gap:10px;align-items:center"
          @click="router.push(`/diaries/${m.id}`)"
        >
          <MapPin :size="16" style="color:var(--color-primary);flex-shrink:0" />
          <div>
            <div style="font-size:13px;font-weight:600">{{ m.title }}</div>
            <div style="font-size:11px;color:var(--color-text-2)">{{ m.locationName }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 일기 쓰기 버튼 (지도가 있을 때) -->
    <button
      v-if="mapReady"
      class="btn btn-primary"
      style="position:absolute;bottom:28px;right:28px;border-radius:var(--radius-full);padding:14px 22px;box-shadow:var(--shadow-lg);font-size:15px;z-index:10"
      @click="showModal = true"
    >
      <Plus :size="18" /> 일기 쓰기
    </button>

    <!-- 마커 팝업 -->
    <div
      v-if="popup"
      style="position:absolute;bottom:80px;left:50%;transform:translateX(-50%);background:var(--color-bg-2);border:1px solid var(--color-border);border-radius:var(--radius-lg);padding:16px 20px;min-width:220px;box-shadow:var(--shadow-lg);z-index:10"
    >
      <button class="modal-close" style="position:absolute;top:8px;right:8px" @click="popup=null">✕</button>
      <div style="font-size:13px;font-weight:600;padding-right:24px">{{ popup.title }}</div>
      <div style="font-size:11px;color:var(--color-text-2);margin-top:4px;display:flex;align-items:center;gap:4px">
        <MapPin :size="11" />{{ popup.locationName }}
      </div>
      <button
        class="btn btn-primary btn-sm"
        style="margin-top:12px;width:100%"
        @click="router.push(`/diaries/${popup.id}`)"
      >
        일기 보기
      </button>
    </div>

    <!-- 일기 작성 모달 -->
    <Teleport to="body">
      <div v-if="showModal" class="modal-backdrop" @click.self="closeModal">
        <div class="modal" style="max-width:560px">
          <div class="modal-header">
            <span class="modal-title">📝 일기 작성</span>
            <button class="modal-close" @click="closeModal">✕</button>
          </div>
          <div class="modal-body">
            <!-- 위치 표시 -->
            <div v-if="form.locationName" style="display:flex;align-items:center;gap:6px;margin-bottom:14px;color:var(--color-primary);font-size:13px">
              <MapPin :size="14" />
              {{ form.locationName }}
            </div>

            <div class="form-group">
              <label class="form-label">제목 *</label>
              <input v-model="form.title" type="text" class="form-input" placeholder="일기 제목을 입력하세요" maxlength="200" />
            </div>

            <div class="form-group">
              <label class="form-label">내용 *</label>
              <textarea v-model="form.content" class="form-input" placeholder="오늘의 기억을 기록해보세요..." style="min-height:120px"></textarea>
            </div>

            <div class="form-group">
              <label class="form-label">
                <span style="display:flex;align-items:center;gap:6px"><Image :size="14" />사진 (최대 5장)</span>
              </label>
              <div class="img-upload-zone" @click="$refs.fileInput.click()">
                <span>클릭하여 사진 추가</span>
              </div>
              <input ref="fileInput" type="file" accept="image/*" multiple style="display:none" @change="onImageChange" />
              <div class="img-preview-list">
                <div v-for="(src, idx) in form.imagePreviews" :key="idx" class="img-preview-item">
                  <img :src="src" />
                  <button class="img-preview-remove" @click="removeImage(idx)">✕</button>
                </div>
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">주소 (선택)</label>
              <input v-model="form.address" type="text" class="form-input" placeholder="상세 주소를 입력하세요" />
            </div>

            <p v-if="error" class="text-danger text-sm">{{ error }}</p>
          </div>
          <div class="modal-footer">
            <button class="btn btn-ghost" @click="closeModal">취소</button>
            <button class="btn btn-primary" :disabled="loading" @click="saveDiary">
              <span v-if="loading" class="spinner" style="width:14px;height:14px;border-width:2px"></span>
              <span v-else>저장하기</span>
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>