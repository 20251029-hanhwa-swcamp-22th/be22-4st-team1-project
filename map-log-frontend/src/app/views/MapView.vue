<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { diaryApi } from '@/app/api/diary.js'
import { friendApi } from '@/app/api/friend.js'
import { X, Image, MapPin, Plus, Users, Search } from 'lucide-vue-next'
import { mockMarkers, mockFriends } from '@/app/data/MockData.js'
import DaumPostcode from '@/app/components/DaumPostcode.vue'

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
const friends = ref([])
const form = ref({
  title: '',
  content: '',
  images: [],
  imagePreviews: [],
  latitude: null,
  longitude: null,
  locationName: '',
  address: '',
  addressDetail: '',    // 상세 주소 (선택)
  visibility: 'PRIVATE',
  sharedUserIds: []
})
const loading = ref(false)
const error = ref('')

// ── 다음 주소 검색 레이어 상태 ──
const showPostcode = ref(false)

// ── 지도 상단 장소 검색 ──
const searchQuery = ref('')       // 검색어 입력값
const searchLoading = ref(false)  // 검색 중 로딩 상태

// ── 마커 팝업 (카카오맵 CustomOverlay 인스턴스) ──
let currentOverlay = null  // 현재 표시 중인 CustomOverlay 참조

// ── 카카오맵 로드 ──
function loadKakaoMap() {
  if (!kakaoKey || kakaoKey === '발급받은_카카오맵_JavaScript_키_입력') {
    mapError.value = '.env.local에 VITE_KAKAO_MAP_KEY를 입력해주세요.'
    return
  }

  return new Promise((resolve, reject) => {
    if (window.kakao?.maps) { resolve(); return }

    const script = document.createElement('script')
    // libraries=services 추가: Geocoder(좌표→주소 변환) 기능을 사용하기 위해 필요
    script.src = `//dapi.kakao.com/v2/maps/sdk.js?appkey=${kakaoKey}&autoload=false&libraries=services`
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

  // 【역지오코딩용 Geocoder 인스턴스】좌표 → 행정구역(시/구) 변환에 사용
  const geocoder = new window.kakao.maps.services.Geocoder()

  // 지도 클릭 시 위치 선택
  window.kakao.maps.event.addListener(map, 'click', (e) => {
    const lat = e.latLng.getLat()
    const lng = e.latLng.getLng()
    selectedLocation.value = { lat, lng }
    form.value.latitude = lat
    form.value.longitude = lng

    // 【역지오코딩 ①】클릭한 좌표를 시/구 단위 주소로 변환 → locationName
    geocoder.coord2RegionCode(lng, lat, (result, status) => {
      if (status === window.kakao.maps.services.Status.OK && result.length > 0) {
        const region = result[0]
        form.value.locationName = `${region.region_1depth_name} ${region.region_2depth_name}`
      } else {
        form.value.locationName = '📍 지도에서 선택한 위치'
      }
    })

    // 【역지오코딩 ②】클릭한 좌표를 도로명/지번 주소로 변환 → address
    // coord2Address: 좌표 → { road_address, address } 형태 반환
    // road_address.address_name = 도로명 주소 (예: '서울 종로구 사직로 161')
    // address.address_name     = 지번 주소 (예: '서울 종로구 세종로 1-68')
    geocoder.coord2Address(lng, lat, (result, status) => {
      if (status === window.kakao.maps.services.Status.OK && result.length > 0) {
        const addr = result[0]
        // 도로명 주소 우선, 없으면 지번 주소 사용
        form.value.address = addr.road_address
          ? addr.road_address.address_name
          : addr.address.address_name
      } else {
        form.value.address = ''
      }
    })

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

async function loadFriends() {
  try {
    const res = await friendApi.getFriends()
    friends.value = Array.isArray(res?.data) ? res.data : mockFriends
  } catch {
    friends.value = mockFriends
  }
}

function renderMarkers(list) {
  // 기존 마커 및 오버레이 제거
  markers.forEach(m => m.setMap(null))
  markers = []
  if (currentOverlay) {
    currentOverlay.setMap(null)
    currentOverlay = null
  }

  list.forEach(item => {
    const pos = new window.kakao.maps.LatLng(item.latitude, item.longitude)
    const marker = new window.kakao.maps.Marker({ position: pos, map })

    // 【호버】마커 위에 일기 미리보기 CustomOverlay 표시
    window.kakao.maps.event.addListener(marker, 'mouseover', () => {
      // 기존 오버레이 제거
      if (currentOverlay) currentOverlay.setMap(null)

      // 【CustomOverlay HTML】마커 바로 위에 뜨는 미리보기 카드
      // ⚠️ CustomOverlay는 카카오맵 iframe 위에 렌더링되므로 CSS 변수가 상속되지 않음
      //    → 색상을 직접 지정하여 다크/라이트 모드 모두 잘 보이게 처리
      const content = `
        <div style="
          background: #1e1e2e;
          border: 1px solid #3a3a4a;
          border-radius: 12px;
          padding: 12px 16px;
          min-width: 180px;
          max-width: 240px;
          box-shadow: 0 8px 24px rgba(0,0,0,0.3);
          transform: translateY(-12px);
          pointer-events: auto;
          position: relative;
        ">
          <div style="font-size:13px;font-weight:700;color:#f0f0f0;margin-bottom:4px;">
            ${item.title || '제목 없음'}
          </div>
          <div style="font-size:11px;color:#9ca3af;display:flex;align-items:center;gap:3px;">
            📍 ${item.locationName || '위치 정보 없음'}
          </div>
          <div style="
            width:0;height:0;
            border-left:8px solid transparent;
            border-right:8px solid transparent;
            border-top:8px solid #1e1e2e;
            position:absolute;
            bottom:-8px;
            left:50%;
            transform:translateX(-50%);
          "></div>
        </div>
      `

      // CustomOverlay: 마커 좌표 위치에 매핑, yAnchor=1.3으로 마커 위로 띄움
      currentOverlay = new window.kakao.maps.CustomOverlay({
        position: pos,
        content: content,
        yAnchor: 1.3,   // 1보다 크면 마커보다 위로 올라감
        xAnchor: 0.5     // 가로 중앙 정렬
      })
      currentOverlay.setMap(map)
    })

    // 【호버 해제】마커에서 마우스가 벗어나면 오버레이 숨김
    window.kakao.maps.event.addListener(marker, 'mouseout', () => {
      if (currentOverlay) {
        currentOverlay.setMap(null)
        currentOverlay = null
      }
    })

    // 【클릭】마커 클릭 시 상세 페이지로 이동
    window.kakao.maps.event.addListener(marker, 'click', () => {
      router.push(`/diaries/${item.id}`)
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
    
    // 친구 선택 시 FRIENDS_ONLY로 변경
    const visibility = form.value.sharedUserIds.length > 0 ? 'FRIENDS_ONLY' : 'PRIVATE'
    fd.append('visibility', visibility)
    
    form.value.sharedUserIds.forEach(id => fd.append('sharedUserIds', id))
    form.value.images.forEach(img => fd.append('images', img))

    const res = await diaryApi.createDiary(fd)
    closeModal()
    loadMarkers()
    alert(`일기가 작성되었습니다!`)
  } catch (e) {
    error.value = e?.message || '일기 작성에 실패했습니다.'
  } finally {
    loading.value = false
  }
}

function closeModal() {
  showModal.value = false
  form.value = { title:'',content:'',images:[],imagePreviews:[],latitude:null,longitude:null,locationName:'',address:'',addressDetail:'',visibility:'PRIVATE',sharedUserIds:[] }
  error.value = ''
}

/**
 * 【다음 주소 검색 완료 핸들러】
 * DaumPostcode 컴포넌트에서 @complete 이벤트로 전달받은 주소 데이터를 처리합니다.
 *
 * @param {Object} data - 다음 우편번호 서비스가 반환하는 주소 데이터
 *   - data.roadAddress: 도로명 주소 (예: "서울 강남구 테헤란로 152")
 *   - data.jibunAddress: 지번 주소 (예: "서울 강남구 역삼동 737")
 *   - data.address: 기본 주소 (도로명 우선, 없으면 지번)
 *   - data.buildingName: 건물명 (예: "강남파이낸스센터")
 *   - data.zonecode: 우편번호 (예: "06236")
 */
function onAddressComplete(data) {
  // 도로명 주소 우선, 없으면 지번 주소 사용
  const fullAddress = data.roadAddress || data.jibunAddress || data.address

  // 건물명이 있으면 주소 뒤에 괄호로 추가 (예: "서울 강남구 테헤란로 152 (강남파이낸스센터)")
  const displayAddress = data.buildingName
    ? `${fullAddress} (${data.buildingName})`
    : fullAddress

  // form 데이터에 주소 정보 반영
  form.value.address = displayAddress          // 전체 주소 (도로명 + 건물명)
  form.value.locationName = data.sido + ' ' + data.sigungu  // 시/도 + 시/군/구 (간략 위치명)
}

onMounted(async () => {
  try {
    await loadKakaoMap()
    initMap()
    mapReady.value = true
    loadFriends()
  } catch (e) {
    mapError.value = e.message
  }
})

onUnmounted(() => {
  markers.forEach(m => m.setMap(null))
  if (currentOverlay) currentOverlay.setMap(null)
})

/**
 * 【지도 상단 주소/장소 검색】
 * 1차: Geocoder.addressSearch() → 도로명/지번 주소 검색 (예: "한천로 97-10")
 * 2차: Places.keywordSearch()  → 장소명/키워드 검색 (예: "강남역", "스타벅스")
 *
 * 주소 검색이 실패하면 자동으로 장소 검색으로 폴백(fallback)합니다.
 */
function searchPlace() {
  const keyword = searchQuery.value.trim()
  if (!keyword || !map) return

  searchLoading.value = true

  const geocoder = new window.kakao.maps.services.Geocoder()
  const ps = new window.kakao.maps.services.Places()

  // 【1차】주소 검색 시도 (도로명/지번 주소에 적합)
  geocoder.addressSearch(keyword, (result, status) => {
    if (status === window.kakao.maps.services.Status.OK && result.length > 0) {
      // 주소 검색 성공 → 해당 좌표로 이동
      searchLoading.value = false
      const coord = result[0]
      const moveLatLng = new window.kakao.maps.LatLng(coord.y, coord.x)
      map.panTo(moveLatLng)  // 부드러운 이동
      map.setLevel(3)        // 확대하여 상세 보기
    } else {
      // 【2차 폴백】주소 검색 실패 → 장소명/키워드로 재검색
      ps.keywordSearch(keyword, (placeResult, placeStatus) => {
        searchLoading.value = false

        if (placeStatus === window.kakao.maps.services.Status.OK && placeResult.length > 0) {
          const place = placeResult[0]
          const moveLatLng = new window.kakao.maps.LatLng(place.y, place.x)
          map.panTo(moveLatLng)
          map.setLevel(3)
        } else {
          alert('검색 결과가 없습니다. 다른 키워드로 검색해주세요.')
        }
      })
    }
  })
}
</script>

<template>
  <div style="position:relative;width:100%;height:100vh;overflow:hidden;">

    <!-- 【지도 상단 검색바】주소/장소 검색 → 해당 위치로 지도 이동 -->
    <div
      v-if="mapReady"
      style="
        position: absolute;
        top: 16px;
        left: 50%;
        transform: translateX(-50%);
        z-index: 10;
        display: flex;
        gap: 8px;
        width: 90%;
        max-width: 480px;
      "
    >
      <input
        v-model="searchQuery"
        type="text"
        class="form-input"
        placeholder="장소나 주소를 검색하세요"
        style="
          flex: 1;
          background: var(--color-bg-2, #fff);
          box-shadow: 0 4px 16px rgba(0,0,0,0.2);
          border: none;
          padding: 12px 16px;
          font-size: 14px;
          border-radius: 12px;
        "
        @keyup.enter="searchPlace"
      />
      <button
        class="btn btn-primary"
        style="
          padding: 12px 20px;
          border-radius: 12px;
          box-shadow: 0 4px 16px rgba(0,0,0,0.2);
          display: flex;
          align-items: center;
          gap: 4px;
          white-space: nowrap;
        "
        :disabled="searchLoading"
        @click="searchPlace"
      >
        <Search :size="16" />
        <span v-if="!searchLoading">검색</span>
        <span v-else class="spinner" style="width:14px;height:14px;border-width:2px"></span>
      </button>
    </div>

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



    <!-- 일기 작성 모달 -->
    <Teleport to="body">
      <div v-if="showModal" class="modal-backdrop" @click.self="closeModal">
        <div class="modal" style="max-width:560px">
          <div class="modal-header">
            <span class="modal-title">📝 일기 작성</span>
            <button class="modal-close" @click="closeModal">✕</button>
          </div>
          <div class="modal-body" style="max-height:70vh;overflow-y:auto">
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

            <!-- 친구 공유 선택 -->
            <div class="form-group">
              <label class="form-label" style="display:flex;align-items:center;gap:6px">
                <Users :size="14" /> 친구와 공유하기
              </label>
              <p class="text-sm text-muted" style="margin-bottom:10px">공유할 친구를 선택하면 '친구공유'로 저장됩니다.</p>
              <div style="display:flex;flex-direction:column;gap:6px;max-height:160px;overflow-y:auto;padding:8px;border:1px solid var(--color-border);border-radius:var(--radius-md)">
                <label v-for="f in friends" :key="f.userId" style="display:flex;align-items:center;gap:10px;padding:6px;cursor:pointer">
                  <input type="checkbox" :value="f.userId" v-model="form.sharedUserIds" />
                  <div class="ml-avatar" style="width:24px;height:24px;font-size:10px">{{ f.nickname.charAt(0) }}</div>
                  <span style="font-size:13px">{{ f.nickname }}</span>
                </label>
                <div v-if="!friends.length" class="text-center text-sm text-muted" style="padding:10px">친구가 없습니다.</div>
              </div>
            </div>

            <!-- 【주소 검색 영역】다음 우편번호 서비스 연동 -->
            <div class="form-group">
              <label class="form-label">
                <span style="display:flex;align-items:center;gap:6px"><MapPin :size="14" />주소</span>
              </label>
              <!-- 주소 검색 버튼: 클릭 시 iframe 레이어 열기 -->
              <div style="display:flex;gap:8px">
                <input
                  v-model="form.address"
                  type="text"
                  class="form-input"
                  placeholder="주소 검색 버튼을 눌러주세요"
                  readonly
                  style="flex:1;cursor:pointer;background:var(--color-bg-3, #f9fafb)"
                  @click="showPostcode = true"
                />
                <button
                  type="button"
                  class="btn btn-primary"
                  style="white-space:nowrap;padding:8px 16px;display:flex;align-items:center;gap:4px"
                  @click="showPostcode = true"
                >
                  <Search :size="14" />검색
                </button>
              </div>
              <!-- 상세 주소 입력: 동/호수 등 세부 정보 (선택사항) -->
              <input
                v-model="form.addressDetail"
                type="text"
                class="form-input"
                placeholder="상세 주소를 입력하세요 (예: 3층 301호)"
                style="margin-top:8px"
              />
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

    <!-- 【다음 주소 검색 레이어】v-model로 열림/닫힘 제어, @complete로 선택 결과 수신 -->
    <DaumPostcode v-model="showPostcode" @complete="onAddressComplete" />
  </div>
</template>