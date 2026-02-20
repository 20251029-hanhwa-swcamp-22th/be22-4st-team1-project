<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/app/api/admin.js'
import { ShieldAlert, ShieldCheck, ChevronLeft, ChevronRight } from 'lucide-vue-next'

const users = ref([])
const loading = ref(true)
const page = ref(0)
const totalPages = ref(0)
const statusFilter = ref('')

// 상태 변경 모달
const showModal = ref(false)
const target = ref(null)
const form = ref({ status: 'SUSPENDED', suspensionReason: '', suspensionExpiresAt: '' })
const modalLoading = ref(false)

const STATUS_LABELS = {
  ACTIVE: { label: '정상', cls: 'badge-success' },
  SUSPENDED: { label: '정지', cls: 'badge-danger' },
  WITHDRAWN: { label: '탈퇴', cls: 'badge-warning' }
}

async function load(p = 0) {
  loading.value = true
  try {
    const res = await adminApi.getUsers({ page: p, size: 20, status: statusFilter.value || undefined })
    users.value = res?.data?.content || []
    totalPages.value = res?.data?.totalPages || 1
    page.value = p
  } catch {
    users.value = []
  } finally {
    loading.value = false
  }
}

function openModal(user) {
  target.value = user
  form.value = {
    status: user.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE',
    suspensionReason: '',
    suspensionExpiresAt: ''
  }
  showModal.value = true
}

async function changeStatus() {
  modalLoading.value = true
  try {
    const payload = { status: form.value.status }
    if (form.value.status === 'SUSPENDED') {
      if (!form.value.suspensionReason) { alert('정지 사유를 입력하세요.'); return }
      payload.suspensionReason = form.value.suspensionReason
      if (form.value.suspensionExpiresAt) payload.suspensionExpiresAt = form.value.suspensionExpiresAt
    }
    await adminApi.changeStatus(target.value.userId, payload)
    showModal.value = false
    await load(page.value)
  } catch (e) {
    alert(e?.message || '처리 실패')
  } finally {
    modalLoading.value = false
  }
}

function formatDate(dt) {
  if (!dt) return '-'
  return new Date(dt).toLocaleDateString('ko-KR')
}

onMounted(() => load(0))
</script>

<template>
  <div class="page" style="max-width:100%">
    <h1 class="page-title" style="display:flex;align-items:center;gap:8px">
      <ShieldAlert :size="22" style="color:var(--color-accent)" /> 관리자 패널
    </h1>
    <p class="page-subtitle">회원 목록 조회 및 상태 관리</p>

    <!-- 필터 -->
    <div style="display:flex;gap:10px;margin-bottom:16px">
      <select v-model="statusFilter" class="form-input" style="max-width:160px" @change="load(0)">
        <option value="">전체 상태</option>
        <option value="ACTIVE">정상</option>
        <option value="SUSPENDED">정지</option>
        <option value="WITHDRAWN">탈퇴</option>
      </select>
    </div>

    <div v-if="loading" class="loading-wrap"><div class="spinner"></div></div>

    <div v-else-if="!users.length" class="empty">
      <div class="empty-icon">👤</div>
      <p class="empty-text">회원이 없습니다</p>
    </div>

    <div v-else>
      <!-- 테이블 -->
      <div style="overflow-x:auto;border-radius:var(--radius-lg);border:1px solid var(--color-border)">
        <table style="width:100%;border-collapse:collapse">
          <thead>
            <tr style="background:var(--color-bg-2);border-bottom:1px solid var(--color-border)">
              <th style="padding:12px 16px;text-align:left;font-size:12px;color:var(--color-text-2);font-weight:600">ID</th>
              <th style="padding:12px 16px;text-align:left;font-size:12px;color:var(--color-text-2);font-weight:600">닉네임</th>
              <th style="padding:12px 16px;text-align:left;font-size:12px;color:var(--color-text-2);font-weight:600">이메일</th>
              <th style="padding:12px 16px;text-align:left;font-size:12px;color:var(--color-text-2);font-weight:600">가입일</th>
              <th style="padding:12px 16px;text-align:left;font-size:12px;color:var(--color-text-2);font-weight:600">상태</th>
              <th style="padding:12px 16px;text-align:left;font-size:12px;color:var(--color-text-2);font-weight:600">정지 사유</th>
              <th style="padding:12px 16px;text-align:center;font-size:12px;color:var(--color-text-2);font-weight:600">관리</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="u in users" :key="u.userId"
              style="border-bottom:1px solid var(--color-border);transition:background .15s"
              @mouseenter="$event.target.closest('tr').style.background='var(--color-bg-2)'"
              @mouseleave="$event.target.closest('tr').style.background=''"
            >
              <td style="padding:12px 16px;font-size:13px;color:var(--color-text-3)">{{ u.userId }}</td>
              <td style="padding:12px 16px;font-size:13px;font-weight:500">{{ u.nickname }}</td>
              <td style="padding:12px 16px;font-size:13px;color:var(--color-text-2)">{{ u.email }}</td>
              <td style="padding:12px 16px;font-size:12px;color:var(--color-text-3)">{{ formatDate(u.createdAt) }}</td>
              <td style="padding:12px 16px">
                <span class="badge" :class="STATUS_LABELS[u.status]?.cls || 'badge-primary'">
                  {{ STATUS_LABELS[u.status]?.label || u.status }}
                </span>
              </td>
              <td style="padding:12px 16px;font-size:12px;color:var(--color-text-2)">
                {{ u.suspensionReason || '-' }}
              </td>
              <td style="padding:12px 16px;text-align:center">
                <button
                  v-if="u.status !== 'WITHDRAWN'"
                  class="btn btn-sm"
                  :class="u.status === 'ACTIVE' ? 'btn-danger' : 'btn-success'"
                  @click="openModal(u)"
                >
                  <component :is="u.status === 'ACTIVE' ? ShieldAlert : ShieldCheck" :size="12" />
                  {{ u.status === 'ACTIVE' ? '정지' : '해제' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 페이징 -->
      <div class="pagination">
        <button class="page-btn" :disabled="page===0" @click="load(page-1)">
          <ChevronLeft :size="14" />
        </button>
        <span style="font-size:13px;color:var(--color-text-2)">{{ page+1 }} / {{ totalPages }}</span>
        <button class="page-btn" :disabled="page>=totalPages-1" @click="load(page+1)">
          <ChevronRight :size="14" />
        </button>
      </div>
    </div>

    <!-- 상태 변경 모달 -->
    <Teleport to="body">
      <div v-if="showModal" class="modal-backdrop" @click.self="showModal=false">
        <div class="modal" style="max-width:440px">
          <div class="modal-header">
            <span class="modal-title">회원 상태 변경</span>
            <button class="modal-close" @click="showModal=false">✕</button>
          </div>
          <div class="modal-body" v-if="target">
            <div class="card" style="margin-bottom:16px;background:var(--color-bg-3)">
              <div style="font-size:13px;font-weight:600">{{ target.nickname }}</div>
              <div style="font-size:12px;color:var(--color-text-2)">{{ target.email }}</div>
            </div>

            <div class="form-group">
              <label class="form-label">변경할 상태</label>
              <select v-model="form.status" class="form-input">
                <option value="ACTIVE">정상 (정지 해제)</option>
                <option value="SUSPENDED">정지</option>
              </select>
            </div>

            <template v-if="form.status === 'SUSPENDED'">
              <div class="form-group">
                <label class="form-label">정지 사유 *</label>
                <textarea v-model="form.suspensionReason" class="form-input" placeholder="정지 사유를 입력하세요" style="min-height:80px"></textarea>
              </div>
              <div class="form-group">
                <label class="form-label">정지 만료일 (선택)</label>
                <input v-model="form.suspensionExpiresAt" type="datetime-local" class="form-input" />
              </div>
            </template>
          </div>
          <div class="modal-footer">
            <button class="btn btn-ghost" @click="showModal=false">취소</button>
            <button class="btn btn-primary" :disabled="modalLoading" @click="changeStatus">
              <span v-if="modalLoading" class="spinner" style="width:14px;height:14px;border-width:2px"></span>
              <span v-else>변경</span>
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>