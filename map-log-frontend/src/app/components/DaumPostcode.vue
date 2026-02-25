<script setup>
/**
 * DaumPostcode.vue - 다음 우편번호 서비스 iframe 레이어 컴포넌트
 *
 * 【역할】
 * 다음(Kakao) 우편번호 서비스를 iframe 레이어 방식으로 화면에 표시합니다.
 * 사용자가 주소를 검색하고 선택하면, 선택된 주소 데이터를 부모 컴포넌트에 전달합니다.
 *
 * 【사용법】
 * <DaumPostcode v-model="showPostcode" @complete="onAddressComplete" />
 *
 * 【iframe 레이어 방식을 선택한 이유】
 * - 팝업 방식(open())은 모바일 웹뷰에서 window.open이 차단될 수 있음
 * - embed() 방식은 특정 DOM 요소에 iframe을 삽입하여 안정적으로 동작
 * - 레이어 오버레이로 감싸면 모달처럼 자연스러운 UX 제공
 */
import { ref, watch, nextTick } from 'vue'

// ── Props & Emits 정의 ──
// modelValue: v-model로 레이어의 열림/닫힘 상태를 제어
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

// update:modelValue — v-model 양방향 바인딩용
// complete — 사용자가 주소를 선택했을 때 데이터 전달
const emit = defineEmits(['update:modelValue', 'complete'])

// embed()로 iframe이 삽입될 DOM 요소에 대한 ref
const postcodeContainer = ref(null)

/**
 * 【핵심 로직】modelValue(레이어 표시 여부)가 변경될 때마다 실행
 *
 * true로 변경 → nextTick 후 embed()로 iframe 삽입
 * - nextTick을 쓰는 이유: v-if로 DOM이 생성된 직후에는 ref가 아직 연결 안 됨
 * - DOM 업데이트가 완료된 후에 embed()를 호출해야 정상 동작
 */
watch(() => props.modelValue, async (isOpen) => {
  if (isOpen) {
    // DOM이 렌더링될 때까지 대기
    await nextTick()

    // daum.Postcode가 로드되었는지 확인 (index.html에서 스크립트 로드)
    if (!window.daum?.Postcode) {
      console.error('[DaumPostcode] daum.Postcode 스크립트가 로드되지 않았습니다.')
      return
    }

    // 새로운 Postcode 인스턴스를 생성하고 embed() 호출
    new window.daum.Postcode({
      /**
       * 【oncomplete 콜백】
       * 사용자가 검색 결과에서 주소 항목을 클릭했을 때 실행됩니다.
       *
       * data 객체에 포함된 주요 속성:
       * - zonecode: 우편번호 (예: "13494")
       * - address: 기본 주소 (도로명 또는 지번 중 우선 표시된 주소)
       * - roadAddress: 도로명 주소 (예: "경기 성남시 분당구 판교역로 166")
       * - jibunAddress: 지번 주소 (예: "경기 성남시 분당구 백현동 532")
       * - buildingName: 건물명 (예: "카카오 판교 아지트")
       * - addressType: 주소 타입 ("R": 도로명, "J": 지번)
       * - userSelectedType: 사용자가 선택한 주소 타입
       */
      oncomplete: (data) => {
        // 부모 컴포넌트에 선택된 주소 데이터 전달
        emit('complete', data)
        // 주소 선택 후 레이어 자동 닫기
        close()
      },
      // 【onresize 콜백】iframe 크기가 변경될 때 호출 (레이어 방식에서는 고정 크기 사용)
      // onclose 콜백은 iframe embed 방식에서는 지원하지 않음 (팝업 전용)
      width: '100%',   // 컨테이너 너비에 맞춤
      height: '100%'   // 컨테이너 높이에 맞춤
    }).embed(postcodeContainer.value) // 지정한 DOM 요소에 iframe 삽입
  }
})

/**
 * 레이어 닫기 함수
 * - v-model을 false로 변경하여 레이어를 숨김
 * - 닫기 버튼(✕) 클릭 또는 backdrop 클릭 시 호출
 */
function close() {
  emit('update:modelValue', false)
}
</script>

<template>
  <!--
    【전체 구조】
    Teleport → body에 직접 렌더링 (모달처럼 z-index 충돌 방지)
    backdrop → 반투명 배경. 클릭 시 레이어 닫기
    postcode-layer → 실제 주소 검색 iframe이 표시되는 영역
  -->
  <Teleport to="body">
    <div v-if="modelValue" class="postcode-backdrop" @click.self="close">
      <div class="postcode-layer">
        <!-- 레이어 헤더: 타이틀 + 닫기 버튼 -->
        <div class="postcode-header">
          <span class="postcode-title">📮 주소 검색</span>
          <button class="postcode-close" @click="close" aria-label="닫기">✕</button>
        </div>
        <!--
          【iframe 삽입 영역】
          ref="postcodeContainer"로 연결된 이 div에
          daum.Postcode.embed()가 iframe을 삽입합니다.
          height를 충분히 확보해야 검색 결과가 잘 보입니다.
        -->
        <div ref="postcodeContainer" class="postcode-body"></div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
/*
 * 【스타일 구조 설명】
 * 1. backdrop — 화면 전체를 덮는 반투명 배경 (z-index: 9999)
 * 2. layer — 중앙에 위치한 주소 검색 카드 (최대 500px 너비)
 * 3. header — 타이틀과 닫기 버튼
 * 4. body — iframe이 삽입되는 영역
 */

/* 배경 오버레이: 화면 전체를 반투명하게 덮음 */
.postcode-backdrop {
  position: fixed;             /* 뷰포트 기준 고정 위치 */
  inset: 0;                    /* top/right/bottom/left 모두 0 (화면 전체) */
  background: rgba(0, 0, 0, 0.5); /* 반투명 어두운 배경 */
  display: flex;
  align-items: center;         /* 세로 중앙 정렬 */
  justify-content: center;     /* 가로 중앙 정렬 */
  z-index: 9999;               /* 다른 모달/요소보다 위에 표시 */
  animation: fadeIn 0.2s ease;  /* 부드러운 등장 애니메이션 */
}

/* 주소 검색 레이어 카드 */
.postcode-layer {
  background: var(--color-bg-2, #fff); /* 프로젝트 테마 변수 활용, 폴백: 흰색 */
  border-radius: var(--radius-lg, 16px);
  box-shadow: var(--shadow-lg, 0 20px 60px rgba(0,0,0,0.3));
  width: 95%;                  /* 모바일 대응: 화면 너비의 95% */
  max-width: 500px;            /* 데스크탑에서 최대 너비 제한 */
  overflow: hidden;            /* 둥근 모서리 밖으로 넘치는 콘텐츠 숨김 */
  animation: slideUp 0.3s ease; /* 아래에서 위로 슬라이드 등장 */
}

/* 레이어 헤더 */
.postcode-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}

/* 타이틀 텍스트 */
.postcode-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-1, #1a1a2e);
}

/* 닫기 버튼 */
.postcode-close {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  color: var(--color-text-3, #9ca3af);
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md, 8px);
  transition: background 0.15s, color 0.15s;
}

.postcode-close:hover {
  background: var(--color-bg-3, #f3f4f6);
  color: var(--color-text-1, #1a1a2e);
}

/* iframe이 삽입되는 본문 영역 */
.postcode-body {
  height: 470px;               /* 다음 우편번호 서비스 권장 최소 높이 */
}

/* 등장 애니메이션: 배경 페이드인 */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* 등장 애니메이션: 카드 슬라이드업 */
@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);  /* 아래에서 30px 위치에서 시작 */
  }
  to {
    opacity: 1;
    transform: translateY(0);     /* 원래 위치로 이동 */
  }
}
</style>
