import { defineStore } from 'pinia'
import { ref } from 'vue'
import { apiClient } from '../api/axios'
import { toast } from 'vue3-toastify'

interface UserData {
  id: number
  email: string
  role: string
  firstName?: string
  lastName?: string
  phone?: string
  avatarUrl: string
  createdAt: string
  updatedAt: string
}

export interface UserSettings {
  notifyEmailOnOrderStatus: boolean
}

export interface StorePayload {
  name: string
  address: string
  contactPhone: string
  description: string
  logoUrl: string
}

export interface StoreResponse {
  id?: number | string
  name: string
  address: string
  contactPhone: string
  description: string
  logoUrl: string
  [key: string]: unknown
}

export interface SellerStore {
  id: number
  ownerId: number
  name: string
  slug: string
  address: string
  contactPhone: string
  description: string
  logoUrl: string
  rating: number
  status: string
  createdAt: string
  updatedAt: string
}

export const useUserStore = defineStore('user', () => {
  const user = ref<UserData | null>(null)
  const isAuthenticated = ref<boolean>(false)

  const isCreatingStore = ref<boolean>(false)
  const sellerStore = ref<SellerStore | null>(null)
  const isLoadingStore = ref<boolean>(false)
  const isAuthModalOpen = ref<boolean>(false)
  const settings = ref<UserSettings | null>(null)

  // ─── ЛОГІКА LOCALSTORAGE (ІЗОЛЯЦІЯ ЮЗЕРІВ) ─────────────────────────────
  function getCurrentUserEmail(): string {
    return user.value?.email || 'guest'
  }

  function markAsJoinedLocally(uuid: string) {
    const email = getCurrentUserEmail()
    const raw = localStorage.getItem('joinedGroupSessions')
    let storage: Record<string, string[]> = {}
    try {
      if (raw) storage = JSON.parse(raw)
      if (Array.isArray(storage)) storage = {} // очистка старого формату
    } catch { storage = {} }

    if (!storage[email]) storage[email] = []
    if (!storage[email].includes(uuid)) {
      storage[email].push(uuid)
      localStorage.setItem('joinedGroupSessions', JSON.stringify(storage))
    }
  }

  function isJoinedLocally(uuid: string): boolean {
    const email = getCurrentUserEmail()
    try {
      const raw = localStorage.getItem('joinedGroupSessions')
      if (!raw) return false
      const storage = JSON.parse(raw)
      return Array.isArray(storage[email]) && storage[email].includes(uuid)
    } catch { return false }
  }
  // ─────────────────────────────────────────────────────────────────────────

  function login(userData: UserData) {
    user.value = userData
    isAuthenticated.value = true
  }

  async function logout() {
    // Запит на бекенд для видалення cookie (ігноруємо помилки)
    try {
      await apiClient.post('/auth/logout')
    } catch {
      // Ігноруємо — навіть якщо бекенд недоступний, скидаємо стан локально
    }

    user.value = null
    isAuthenticated.value = false
    sellerStore.value = null
  }

  async function fetchMe(): Promise<void> {
    try {
      const response = await apiClient.get<UserData>('/auth/me')
      user.value = response.data
      isAuthenticated.value = true
    } catch {
      user.value = null
      isAuthenticated.value = false
    }
  }

  async function fetchUserStore(userId: number): Promise<void> {
    isLoadingStore.value = true
    try {
      const response = await apiClient.get<SellerStore>(`/stores/user/${userId}`)
      sellerStore.value = response.data
    } catch {
      sellerStore.value = null
    } finally {
      isLoadingStore.value = false
    }
  }

  async function createStore(payload: StorePayload): Promise<StoreResponse> {
    isCreatingStore.value = true
    try {
      const response = await apiClient.post<StoreResponse>('/stores/create', payload)
      return response.data
    } catch (error) {
      throw error
    } finally {
      isCreatingStore.value = false
    }
  }

  async function updateStore(storeId: number, payload: StorePayload): Promise<StoreResponse> {
    isCreatingStore.value = true
    try {
      const response = await apiClient.put<StoreResponse>(`/stores/${storeId}`, payload)
      return response.data
    } catch (error) {
      throw error
    } finally {
      isCreatingStore.value = false
    }
  }

  async function deleteStore(storeId: number): Promise<boolean> {
    try {
      await apiClient.delete(`/stores/${storeId}`)
      sellerStore.value = null // Очищаємо локальний стейт
      await fetchMe() // Оновлюємо юзера, бо його роль могла змінитись на BUYER
      toast.success('Магазин успішно видалено')
      return true
    } catch (error: any) {
      if (error.response?.status === 409) {
        toast.error('Помилка: У магазину є існуючі товари. Спочатку видаліть їх.')
      } else {
        toast.error(error.response?.data?.message || 'Помилка при видаленні магазину')
      }
      return false
    }
  }

  async function changeStoreStatus(storeId: number, status: string): Promise<boolean> {
    try {
      await apiClient.patch(`/stores/${storeId}/status`, { status })
      return true
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Помилка при зміні статусу')
      return false
    }
  }

  async function fetchUserSettings(): Promise<void> {
    try {
      const response = await apiClient.get<UserSettings>('/user-settings/me')
      settings.value = response.data
    } catch {
      settings.value = null
    }
  }

  async function updateUserSettings(payload: Partial<UserSettings>): Promise<void> {
    await apiClient.put('/user-settings/me', payload)
    if (settings.value) {
      Object.assign(settings.value, payload)
    } else {
      settings.value = payload as UserSettings
    }
  }

  async function updateProfile(payload: { email: string; avatarUrl: string | null }): Promise<void> {
    const response = await apiClient.put<UserData>('/users/me', payload)
    user.value = response.data
  }

  async function changePassword(payload: { oldPassword: string; newPassword: string }): Promise<void> {
    await apiClient.patch('/users/me/password', payload)
  }

  return {
    user, isAuthenticated, isCreatingStore, sellerStore, isLoadingStore, isAuthModalOpen, settings,
    login, logout, fetchMe, fetchUserStore, createStore, updateStore, deleteStore, changeStoreStatus,
    isJoinedLocally, markAsJoinedLocally,
    fetchUserSettings, updateUserSettings, updateProfile, changePassword
  }
})
