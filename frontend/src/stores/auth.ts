import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('access_token'))
  const refreshToken = ref<string | null>(localStorage.getItem('refresh_token'))

  const isLoggedIn = computed(() => token.value !== null)

  function setTokens(accessToken: string, refresh: string) {
    token.value = accessToken
    refreshToken.value = refresh
    localStorage.setItem('access_token', accessToken)
    localStorage.setItem('refresh_token', refresh)
  }

  function clearTokens() {
    token.value = null
    refreshToken.value = null
    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
  }

  return { token, refreshToken, isLoggedIn, setTokens, clearTokens }
})
