<template>
  <div class="login-wrapper">
    <form class="login-form" @submit.prevent="handleLogin">
      <h1>로그인</h1>
      <div class="field">
        <label for="username">아이디</label>
        <input id="username" v-model="form.username" type="text" required autocomplete="username" />
      </div>
      <div class="field">
        <label for="password">비밀번호</label>
        <input id="password" v-model="form.password" type="password" required autocomplete="current-password" />
      </div>
      <p v-if="error" class="error">{{ error }}</p>
      <button type="submit" :disabled="loading">
        {{ loading ? '로그인 중...' : '로그인' }}
      </button>
    </form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import apiClient from '@/api/client'

const router = useRouter()
const auth = useAuthStore()

const form = reactive({ username: '', password: '' })
const loading = ref(false)
const error = ref('')

async function handleLogin() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await apiClient.post('/auth/login', form)
    auth.setTokens(data.data.accessToken, data.data.refreshToken)
    await router.push({ name: 'dashboard' })
  } catch {
    error.value = '아이디 또는 비밀번호를 확인해주세요.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
}
.login-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  width: 320px;
  padding: 2rem;
  border: 1px solid #ddd;
  border-radius: 8px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
input {
  padding: 0.5rem;
  border: 1px solid #ccc;
  border-radius: 4px;
}
button {
  padding: 0.75rem;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.error {
  color: #dc2626;
  font-size: 0.875rem;
}
</style>
