import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { apiClient } from '../api/axios' // adjust path if needed

// ─── Ключ для localStorage ────────────────────────────────────────────────────
const STORAGE_KEY = 'sellora_user'

// ─── Тип даних користувача ────────────────────────────────────────────────────
interface UserData {
  email: string
}

// ─── Тип payload для створення магазину ──────────────────────────────────────
export interface StorePayload {
  name: string
  address: string
  contactPhone: string
  description: string
  logoUrl: string
}

// ─── Тип відповіді від бекенду (розширте за потреби) ─────────────────────────
export interface StoreResponse {
  id?: number | string
  name: string
  address: string
  contactPhone: string
  description: string
  logoUrl: string
  [key: string]: unknown
}

// ─── Допоміжна функція: зчитуємо збережений стан при старті ──────────────────
function loadFromStorage(): { user: UserData | null; isAuthenticated: boolean } {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return { user: null, isAuthenticated: false }
    const parsed = JSON.parse(raw)
    return {
      user:            parsed.user            ?? null,
      isAuthenticated: parsed.isAuthenticated ?? false,
    }
  } catch {
    return { user: null, isAuthenticated: false }
  }
}

// ─── Pinia Store: userStore ───────────────────────────────────────────────────
export const useUserStore = defineStore('user', () => {

  const saved = loadFromStorage()

  const user            = ref<UserData | null>(saved.user)
  const isAuthenticated = ref<boolean>(saved.isAuthenticated)

  // ─── Стан завантаження для createStore ───────────────────────────────────
  const isCreatingStore = ref<boolean>(false)

  watch(
    [user, isAuthenticated],
    ([newUser, newAuth]) => {
      localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({ user: newUser, isAuthenticated: newAuth }),
      )
    },
    { deep: true },
  )

  function login(userData: UserData) {
    user.value            = userData
    isAuthenticated.value = true
  }

  function logout() {
    user.value            = null
    isAuthenticated.value = false
    localStorage.removeItem(STORAGE_KEY)
  }

  // ─── Дія: створити магазин ────────────────────────────────────────────────
  async function createStore(payload: StorePayload): Promise<StoreResponse> {
    isCreatingStore.value = true
    try {
      const response = await apiClient.post<StoreResponse>('/stores/create', payload)
      return response.data
    } catch (error) {
      // Прокидаємо помилку далі, щоб компонент міг її обробити
      throw error
    } finally {
      isCreatingStore.value = false
    }
  }

  return { user, isAuthenticated, isCreatingStore, login, logout, createStore }
})
