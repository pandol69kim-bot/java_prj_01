<template>
  <AppLayout>
    <div class="systems-page">
      <div class="systems-page__header">
        <h1 class="systems-page__title">외부 시스템</h1>
        <BaseButton variant="primary" size="sm" @click="showCreateModal = true">
          + 시스템 등록
        </BaseButton>
      </div>

      <BaseAlert v-if="store.systemsError" :message="store.systemsError" type="error" />
      <BaseAlert v-if="successMsg" :message="successMsg" type="success" />

      <BaseTable
        :columns="columns"
        :rows="rows"
        :loading="store.systemsLoading"
      >
        <template #active="{ value }">
          <StatusBadge :status="value ? 'ACTIVE' : 'INACTIVE'" />
        </template>
        <template #authType="{ value }">
          <span class="auth-tag">{{ value }}</span>
        </template>
        <template #actions="{ row }">
          <BaseButton
            variant="danger"
            size="sm"
            :loading="deactivatingId === row.id"
            @click="handleDeactivate(Number(row.id))"
          >비활성화</BaseButton>
        </template>
      </BaseTable>

      <BasePagination
        :current-page="currentPage"
        :total-pages="totalPages"
        :total-elements="totalElements"
        @change="loadPage"
      />
    </div>

    <!-- 시스템 등록 모달 -->
    <BaseModal v-model="showCreateModal" title="외부 시스템 등록" max-width="40rem">
      <form @submit.prevent="handleCreate">
        <BaseInput
          v-model="form.systemCode"
          label="시스템 코드"
          placeholder="예: ERP_SYSTEM"
          :error="errors.systemCode"
          required
          hint="영문 대문자, 숫자, 언더스코어 2~50자"
        />
        <BaseInput
          v-model="form.name"
          label="시스템 이름"
          placeholder="예: ERP 시스템"
          :error="errors.name"
          required
        />
        <BaseInput
          v-model="form.baseUrl"
          label="Base URL"
          placeholder="https://api.example.com"
          :error="errors.baseUrl"
          required
        />
        <BaseSelect
          v-model="form.authType"
          label="인증 방식"
          :options="authTypeOptions"
          :error="errors.authType"
          required
        />
        <BaseInput
          v-if="form.authType !== 'NONE'"
          v-model="form.apiKey"
          label="API 키 / 토큰"
          placeholder="인증 키 또는 토큰 입력"
          type="password"
        />
        <BaseInput
          v-model="form.description"
          label="설명 (선택)"
          placeholder="시스템 설명을 입력하세요"
        />
      </form>

      <template #footer>
        <BaseButton variant="secondary" @click="showCreateModal = false">취소</BaseButton>
        <BaseButton variant="primary" :loading="creating" @click="handleCreate">등록</BaseButton>
      </template>
    </BaseModal>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseTable from '@/components/ui/BaseTable.vue'
import BaseAlert from '@/components/ui/BaseAlert.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import BaseSelect from '@/components/ui/BaseSelect.vue'
import BaseModal from '@/components/ui/BaseModal.vue'
import BasePagination from '@/components/ui/BasePagination.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import { useSyncDataStore } from '@/stores/syncData'
import { externalSystemApi, type CreateExternalSystemRequest } from '@/api/externalSystem'

const store = useSyncDataStore()
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const deactivatingId = ref<number | null>(null)
const showCreateModal = ref(false)
const creating = ref(false)
const successMsg = ref('')

const authTypeOptions = [
  { value: 'NONE', label: '없음 (NONE)' },
  { value: 'API_KEY', label: 'API Key' },
  { value: 'BEARER_TOKEN', label: 'Bearer Token' },
  { value: 'BASIC', label: 'Basic Auth' },
]

const initialForm = (): CreateExternalSystemRequest => ({
  systemCode: '',
  name: '',
  baseUrl: '',
  authType: 'NONE',
  apiKey: '',
  description: '',
})

const form = reactive<CreateExternalSystemRequest>(initialForm())
const errors = reactive<Partial<Record<keyof CreateExternalSystemRequest, string>>>({})

const columns = [
  { key: 'systemCode', label: '시스템 코드', width: '140px' },
  { key: 'name',       label: '시스템명' },
  { key: 'baseUrl',    label: 'Base URL' },
  { key: 'authType',   label: '인증 방식', width: '110px' },
  { key: 'active',     label: '상태',      width: '80px' },
  { key: 'actions',    label: '',          width: '90px' },
]

const rows = computed(() =>
  store.systems.map((s) => ({
    id:         s.id,
    systemCode: s.systemCode,
    name:       s.name,
    baseUrl:    s.baseUrl,
    authType:   s.authType,
    active:     s.active,
  })),
)

function validateForm(): boolean {
  errors.systemCode = ''
  errors.name = ''
  errors.baseUrl = ''
  errors.authType = ''

  let valid = true
  if (!form.systemCode) {
    errors.systemCode = '시스템 코드를 입력해주세요.'
    valid = false
  } else if (!/^[A-Z0-9_]{2,50}$/.test(form.systemCode)) {
    errors.systemCode = '영문 대문자, 숫자, 언더스코어 2~50자여야 합니다.'
    valid = false
  }
  if (!form.name) {
    errors.name = '시스템 이름을 입력해주세요.'
    valid = false
  }
  if (!form.baseUrl) {
    errors.baseUrl = 'Base URL을 입력해주세요.'
    valid = false
  } else if (!/^https?:\/\//.test(form.baseUrl)) {
    errors.baseUrl = 'http:// 또는 https://로 시작해야 합니다.'
    valid = false
  }
  return valid
}

async function handleCreate() {
  if (!validateForm()) return
  creating.value = true
  try {
    await externalSystemApi.create({
      ...form,
      apiKey: form.apiKey || undefined,
      description: form.description || undefined,
    })
    showCreateModal.value = false
    Object.assign(form, initialForm())
    successMsg.value = '시스템이 등록되었습니다.'
    setTimeout(() => { successMsg.value = '' }, 3000)
    await loadPage(0)
  } catch (err: unknown) {
    errors.systemCode = err instanceof Error ? err.message : '등록에 실패했습니다.'
  } finally {
    creating.value = false
  }
}

async function loadPage(page: number) {
  currentPage.value = page
  await store.fetchSystems(page)
  try {
    const res = await externalSystemApi.getList(page)
    totalPages.value = res.data.data.totalPages
    totalElements.value = res.data.data.totalElements
  } catch { /* store.systemsError에 표시됨 */ }
}

async function handleDeactivate(id: number) {
  deactivatingId.value = id
  try {
    await externalSystemApi.deactivate(id)
    await loadPage(currentPage.value)
  } finally {
    deactivatingId.value = null
  }
}

onMounted(() => loadPage(0))
</script>

<style scoped>
.systems-page { display: flex; flex-direction: column; gap: var(--space-4); }

.systems-page__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.systems-page__title { font-size: var(--text-2xl); font-weight: 700; color: var(--color-text); }

.auth-tag {
  padding: 1px var(--space-2);
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  font-family: monospace;
}
</style>
