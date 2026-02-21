/**
 * 지도(Map)를 메인으로 보여주는 화면입니다.
 * - 카카오맵 API 연동 (외부 SDK)
 * - 지도 범위 기반 마커(일기) 동적 렌더링
 * - 지도 클릭을 통한 신규 일기 작성 (친구 선택 포함)
 */
<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { diaryApi } from '@/app/api/diary.js'
import { friendApi } from '@/app/api/friend.js'
import { X, Image, MapPin, Plus, Users } from 'lucide-vue-next'
import { mockMarkers, mockFriends } from '@/app/data/MockData.js'

const router = useRouter()

// ── 지도 상태 관리 ──
const mapContainer = ref(null)
let map = null
let markers = []
const kakaoKey = import.meta.env.VITE_KAKAO_MAP_KEY // 환경변수에서 API 키 로드
const mapReady = ref(false)
const mapError = ref('')

// ── 일기 작성 모달 및 폼 상태 ──
const showModal = ref(false)
const selectedLocation = ref(null)
const friends = ref([]) // 친구 목록 (공유용)
const form = ref({
  title: '',
  content: '',
  images: [],
  imagePreviews: [],
  latitude: null,
  longitude: null,
  locationName: '',
  address: '',
  visibility: 'PRIVATE', // 기본값은 나만보기
  sharedUserIds: []      // 공유할 친구 ID 배열
})
const loading = ref(false)
const error = ref('')

// ── 마커 클릭 시 나타나는 팝업 데이터 ──
const popup = ref(null)

/**
 * 카카오맵 SDK를 동적으로 로드합니다.
 * @returns {Promise} SDK 로드 완료 후 resolve
 */
function loadKakaoMap() {
  if (!kakaoKey || kakaoKey === '발급받은_카카오맵_JavaScript_키_입력') {
    mapError.value = '.env.local에 VITE_KAKAO_MAP_KEY를 입력해주세요.'
    return
  }

  return new Promise((resolve, reject) => {
    if (window.kakao?.maps) { resolve(); return }

    const script = document.createElement('script')
    // autoload=false를 사용하여 수동으로 load() 호출
    script.src = `//dapi.kakao.com/v2/maps/sdk.js?appkey=${kakaoKey}&autoload=false`
    script.onload = () => {
      window.kakao.maps.load(() => resolve())
    }
    script.onerror = () => reject(new Error('카카오맵 SDK 로드 실패'))
    document.head.appendChild(script)
  })
}

/** 지도 객체 생성 및 이벤트 리스너 등록 */
function initMap() {
  const center = new window.kakao.maps.LatLng(37.5665, 126.9780)
  map = new window.kakao.maps.Map(mapContainer.value, {
    center,
    level: 5
  })

  // [이벤트] 지도 클릭 시: 해당 위치 정보를 저장하고 작성 모달 오픈
  window.kakao.maps.event.addListener(map, 'click', (e) => {
    const lat = e.latLng.getLat()
    const lng = e.latLng.getLng()
    selectedLocation.value = { lat, lng }
    form.value.latitude = lat
    form.value.longitude = lng
    form.value.locationName = `위도 ${lat.toFixed(4)}, 경도 ${lng.toFixed(4)}`
    showModal.value = true
  })

  // [이벤트] 지도 이동 완료 시: 화면 범위 내 마커들을 새로 불러옴
  window.kakao.maps.event.addListener(map, 'idle', loadMarkers)

  loadMarkers()
}

/** 현재 지도 영역(SouthWest, NorthEast) 정보를 백엔드에 전달하여 마커 목록 로드 */
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
    // API 실패 시 목업 데이터로 폴백
    renderMarkers(mockMarkers)
  }
}

/** 지도 위에 마커들을 실제로 렌더링 */
function renderMarkers(list) {
  markers.forEach(m => m.setMap(null)) // 기존 마커 모두 제거
  markers = []

  list.forEach(item => {
    const pos = new window.kakao.maps.LatLng(item.latitude, item.longitude)
    const marker = new window.kakao.maps.Marker({ position: pos, map })

    // 마커 클릭 시 상세 팝업 표시
    window.kakao.maps.event.addListener(marker, 'click', () => {
      popup.value = item
    })

    markers.push(marker)
  })
}

/** 이미지 파일 선택 시 미리보기 생성 (FileReader 활용) */
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

/** 서버에 일기 데이터 전송 (Multipart/FormData 방식) */
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
    
    // 비즈니스 룰: 친구를 선택했다면 가시성을 '친구공유'로 설정
    const visibility = form.value.sharedUserIds.length > 0 ? 'FRIENDS_ONLY' : 'PRIVATE'
    fd.append('visibility', visibility)
    
    form.value.sharedUserIds.forEach(id => fd.append('sharedUserIds', id))
    form.value.images.forEach(img => fd.append('images', img))

    await diaryApi.createDiary(fd)
    closeModal()
    loadMarkers() // 저장 후 마커 즉시 갱신
    alert(`일기가 작성되었습니다!`)
  } catch (e) {
    error.value = e?.message || '일기 작성에 실패했습니다.'
  } finally {
    loading.value = false
  }
}

function closeModal() {
  showModal.value = false
  // 폼 데이터 초기화
  form.value = { title:'',content:'',images:[],imagePreviews:[],latitude:null,longitude:null,locationName:'',address:'',visibility:'PRIVATE',sharedUserIds:[] }
  error.value = ''
}

onMounted(async () => {
  try {
    await loadKakaoMap()
    initMap()
    mapReady.value = true
    // 친구 목록 미리 로드 (공유용)
    const res = await friendApi.getFriends()
    friends.value = res?.data || []
  } catch (e) {
    mapError.value = e.message
  }
})

onUnmounted(() => {
  // 컴포넌트 파괴 시 마커 해제
  markers.forEach(m => m.setMap(null))
})
</script>

<template>
  <!-- 지도가 렌더링될 메인 컨테이너 -->
  <div style="position:relative;width:100%;height:100vh;overflow:hidden;">
    <div ref="mapContainer" style="width:100%;height:100%;background:var(--color-bg-3)"></div>
    
    <!-- 중략: 템플릿 및 모달 레이아웃 -->
  </div>
</template>
