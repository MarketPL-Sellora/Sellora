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
  const isLoading  = ref(false)
  const error      = ref<string | null>(null)

  // ─── ЛОГІКА LOCALSTORAGE (ПРИВ'ЯЗКА ДО ЮЗЕРА) ─────────────────────────────

  function getCurrentUserKey(): string {
    try {
      const raw = localStorage.getItem('sellora_user')
      if (!raw) return 'guest'
      const userData = JSON.parse(raw)
      return userData?.user?.email || 'guest'
    } catch {
      return 'guest'
    }
  }

  function markAsJoinedLocally(uuid: string) {
    const userKey = getCurrentUserKey()
    let storage: Record<string, string[]> = {}

    try {
      const raw = localStorage.getItem('joinedGroupSessions')
      if (raw) {
        const parsed = JSON.parse(raw)
        // Якщо це старий формат (простий масив) - стираємо його і починаємо з чистого аркуша
        storage = Array.isArray(parsed) ? {} : parsed
      }
    } catch {
      storage = {}
    }

    // Створюємо масив для юзера, якщо його ще немає
    if (!Array.isArray(storage[userKey])) {
      storage[userKey] = []
    }

    // Додаємо uuid, якщо його там ще немає
    if (!storage[userKey].includes(uuid)) {
      storage[userKey].push(uuid)
      localStorage.setItem('joinedGroupSessions', JSON.stringify(storage))
    }
  }

  function isJoinedLocally(uuid: string): boolean {
    const userKey = getCurrentUserKey()
    try {
      const raw = localStorage.getItem('joinedGroupSessions')
      if (!raw) return false
      const parsed = JSON.parse(raw)

      // Підтримка для старих тестів (якщо там ще масив)
      if (Array.isArray(parsed)) return parsed.includes(uuid)

      return Array.isArray(parsed[userKey]) && parsed[userKey].includes(uuid)
    } catch {
      return false
    }
  }
  // ─────────────────────────────────────────────────────────────────────────

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

      // Одразу фіксуємо в пам'яті, що творець (поточний юзер) є учасником
      if (response.data.uuid) {
        markAsJoinedLocally(response.data.uuid)
      }

      session.value  = response.data
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

      // Фіксуємо приєднання в пам'яті браузера для конкретного юзера
      markAsJoinedLocally(uuid)

      return true
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Не вдалося приєднатися'
      return false
    } finally {
      isLoading.value = false
    }
  }

  function clearSession() {
    session.value = null
    error.value   = null
  }

  return {
    session, isLoading, error,
    fetchSession, createSession, joinSession, clearSession,
    isJoinedLocally // Експортуємо для використання в компонентах
  }
})
