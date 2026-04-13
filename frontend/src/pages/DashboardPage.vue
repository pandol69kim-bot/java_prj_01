<template>
  <div class="dashboard">
    <header class="header">
      <h1>대시보드</h1>
      <button @click="handleLogout">로그아웃</button>
    </header>
    <main class="content">
      <p>연동 현황 대시보드 (구현 예정)</p>
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

function handleLogout() {
  auth.clearTokens()
  router.push({ name: 'login' })
}
</script>

<style scoped>
.dashboard { display: flex; flex-direction: column; min-height: 100vh; }
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 2rem;
  background: #1e40af;
  color: white;
}
.content { padding: 2rem; }
button {
  padding: 0.5rem 1rem;
  background: white;
  color: #1e40af;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
</style>
