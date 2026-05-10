import { defineStore } from 'pinia'
import { ref } from 'vue'
import { apiClient } from '../api/axios'

export interface GroupBuySession {
  uuid: string
  productId: number
  productTitle: string
  productImage: string
  price: number
  targetSize: number
  currentMembersCount: number
  status: string
  expiresAt: string
  isAvailable: boolean
}

export const useGroupBuyStore = defineStore('groupBuy', () => {
  const session    = ref<GroupBuySession | null>(null)
  const mySessions = ref<GroupBuySession[]>([])
  const isLoading  = ref(false)
  const error      = ref<string | null>(null)

  // ─── Перевірка участі через серверний масив mySessions ──────────────────────

  function isJoined(uuid: string): boolean {
    return mySessions.value.some(s => s.uuid === uuid)
  }

  async function fetchSession(uuid: string): Promise<GroupBuySession | null> {
    isLoading.value = true
    error.value     = null
    try {
      const response = await apiClient.get<GroupBuySession>(`/group-buy/sessions/${uuid}`)
      session.value  = response.data
      return response.data
    } catch (err: unknown) {
      error.value   = err instanceof Error ? err.message : 'Помилка завантаження сесії'
      session.value = null
      return null
    } finally {
      isLoading.value = false
    }
  }

  async function createSession(productId: number): Promise<GroupBuySession | null> {
    isLoading.value = true
    error.value     = null
    try {
      const response = await apiClient.post<GroupBuySession>('/group-buy/sessions', { productId })

      // ХАК ДЛЯ ФРОНТА: Якщо бекенд віддає 0 учасників при створенні, ми ставимо 1
      if (response.data.currentMembersCount === 0) {
        response.data.currentMembersCount = 1
      }

      session.value = response.data

      // Оновлюємо масив mySessions з бекенду
      await fetchMySessions()

      return response.data
    } catch (err: unknown) {
      error.value   = err instanceof Error ? err.message : 'Помилка створення сесії'
      session.value = null
      return null
    } finally {
      isLoading.value = false
    }
  }

  async function joinSession(uuid: string): Promise<boolean> {
    isLoading.value = true
    error.value     = null
    try {
      await apiClient.post(`/group-buy/sessions/${uuid}/join`)

      // Оновлюємо масив mySessions з бекенду
      await fetchMySessions()

      return true
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Не вдалося приєднатися'
      return false
    } finally {
      isLoading.value = false
    }
  }

  async function fetchMySessions(): Promise<void> {
    isLoading.value = true
    error.value     = null
    try {
      const response = await apiClient.get<GroupBuySession[]>('/group-buy/sessions/user')
      mySessions.value = response.data
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Помилка завантаження сесій'
      mySessions.value = []
    } finally {
      isLoading.value = false
    }
  }

  function clearSession() {
    session.value = null
    error.value   = null
  }

  return {
    session, mySessions, isLoading, error,
    fetchSession, createSession, joinSession, clearSession,
    fetchMySessions, isJoined,
  }
})
