<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '../../state/userStore'
import { apiClient } from '../../api/axios'
import { toast } from 'vue3-toastify'

const userStore = useUserStore()

// ─── Стан профілю ──────────────────────────────────────────────────────────────
const email = ref('')
const avatarUrl = ref('')
const emailError = ref('')
const isProfileSaving = ref(false)

// ─── Стан пароля ───────────────────────────────────────────────────────────────
const isPasswordModalOpen = ref(false)
const oldPassword = ref('')
const newPassword = ref('')
const repeatPassword = ref('')
const passwordError = ref('')
const isPasswordSaving = ref(false)

// ─── Стан налаштувань ──────────────────────────────────────────────────────────
const notifyEmailOnOrderStatus = ref(false)

// ─── Toast ─────────────────────────────────────────────────────────────────────
const showToast = ref(false)
const toastMessage = ref('')

// ─── Завантаження даних ────────────────────────────────────────────────────────
onMounted(async () => {
  await Promise.all([userStore.fetchMe(), userStore.fetchUserSettings()])
  email.value = userStore.user?.email || ''
  avatarUrl.value = userStore.user?.avatarUrl || ''
  notifyEmailOnOrderStatus.value = userStore.settings?.notifyEmailOnOrderStatus || false
})

// ─── Завантаження аватара ──────────────────────────────────────────────────────
async function handleAvatarUpload(e: Event) {
  const input = e.target as HTMLInputElement
  if (!input.files || !input.files[0]) return
  const file = input.files[0]

  const formData = new FormData()
  formData.append('file', file)

  try {
    const res = await apiClient.post('/uploads/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    avatarUrl.value = res.data?.url || res.data || ''
  } catch {
    // silent
  } finally {
    input.value = ''
  }
}

// ─── Збереження профілю ────────────────────────────────────────────────────────
async function handleSaveProfile() {
  emailError.value = ''
  isProfileSaving.value = true
  try {
    await userStore.updateProfile({
      email: email.value,
      avatarUrl: avatarUrl.value || null,
    })
    showNotification('Профіль збережено')
  } catch (err: any) {
    if (err.response?.status === 409) {
      emailError.value = 'Цей Email вже зайнятий'
    }
  } finally {
    isProfileSaving.value = false
  }
}

// ─── Зміна пароля ──────────────────────────────────────────────────────────────
async function handleChangePassword() {
  passwordError.value = ''
  if (newPassword.value !== repeatPassword.value) {
    passwordError.value = 'Паролі не співпадають'
    return
  }
  isPasswordSaving.value = true
  try {
    await userStore.changePassword({
      oldPassword: oldPassword.value,
      newPassword: newPassword.value,
    })
    isPasswordModalOpen.value = false
    oldPassword.value = ''
    newPassword.value = ''
    repeatPassword.value = ''
    passwordError.value = ''
    toast.success('Пароль успішно змінено')
  } catch {
    passwordError.value = 'Невірний старий пароль або помилка сервера'
  } finally {
    isPasswordSaving.value = false
  }
}

// ─── Авто-збереження налаштувань ───────────────────────────────────────────────
async function handleSettingsChange() {
  try {
    await userStore.updateUserSettings({
      notifyEmailOnOrderStatus: notifyEmailOnOrderStatus.value,
    })
    showNotification('Налаштування збережено')
  } catch {
    // silent
  }
}

// ─── Toast-хелпер ─────────────────────────────────────────────────────────────
function showNotification(msg: string) {
  toastMessage.value = msg
  showToast.value = true
  setTimeout(() => {
    showToast.value = false
  }, 3000)
}
</script>

<template>
  <div class="settings-root">
    <!-- ═══════════════ ЗАГАЛЬНІ НАЛАШТУВАННЯ ═══════════════ -->
    <section class="settings-card">
      <div class="card-header">
        <div class="card-header-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
        </div>
        <h2 class="card-title">Загальні налаштування</h2>
      </div>

      <div class="card-body">
        <!-- Аватар -->
        <div class="avatar-section">
          <div class="avatar-wrapper">
            <img v-if="avatarUrl" :src="avatarUrl" alt="Аватар" class="avatar-img" />
            <div v-else class="avatar-placeholder">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            </div>
            <div class="avatar-badge">
              <svg width="10" height="10" viewBox="0 0 12 12" fill="none" stroke="white" stroke-width="2" stroke-linecap="round"><path d="M6 2v8M2 6h8"/></svg>
            </div>
          </div>

          <div class="avatar-actions">
            <label class="btn-upload" tabindex="0">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
              Завантажити нове фото
              <input type="file" hidden accept="image/*" @change="handleAvatarUpload" />
            </label>
            <span class="avatar-hint">PNG, JPG до 2 МБ</span>
          </div>
        </div>

        <div class="divider"></div>

        <!-- Email -->
        <div class="field-group">
          <label class="field-label">Email</label>
          <div class="field-input-wrap">
            <svg class="field-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
            <input
              v-model="email"
              type="email"
              placeholder="your@email.com"
              class="field-input"
              :class="{ 'field-input--error': emailError }"
            />
          </div>
          <p v-if="emailError" class="field-error">{{ emailError }}</p>
        </div>

        <!-- Кнопки -->
        <div class="profile-actions">
          <button class="btn-primary" :disabled="isProfileSaving" @click="handleSaveProfile">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
            Зберегти зміни
          </button>
          <button class="btn-secondary" @click="isPasswordModalOpen = true">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
            Змінити пароль
          </button>
        </div>
      </div>
    </section>

    <!-- ═══════════════ ДОДАТКОВІ НАЛАШТУВАННЯ ═══════════════ -->
    <section class="settings-card">
      <div class="card-header">
        <div class="card-header-icon card-header-icon--purple">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12.22 2h-.44a2 2 0 00-2 2v.18a2 2 0 01-1 1.73l-.43.25a2 2 0 01-2 0l-.15-.08a2 2 0 00-2.73.73l-.22.38a2 2 0 00.73 2.73l.15.1a2 2 0 011 1.72v.51a2 2 0 01-1 1.74l-.15.09a2 2 0 00-.73 2.73l.22.38a2 2 0 002.73.73l.15-.08a2 2 0 012 0l.43.25a2 2 0 011 1.73V20a2 2 0 002 2h.44a2 2 0 002-2v-.18a2 2 0 011-1.73l.43-.25a2 2 0 012 0l.15.08a2 2 0 002.73-.73l.22-.39a2 2 0 00-.73-2.73l-.15-.08a2 2 0 01-1-1.74v-.5a2 2 0 011-1.74l.15-.09a2 2 0 00.73-2.73l-.22-.38a2 2 0 00-2.73-.73l-.15.08a2 2 0 01-2 0l-.43-.25a2 2 0 01-1-1.73V4a2 2 0 00-2-2z"/><circle cx="12" cy="12" r="3"/></svg>
        </div>
        <h2 class="card-title">Додаткові налаштування</h2>
      </div>

      <div class="card-body">
        <label class="toggle-row">
          <div class="toggle-info">
            <span class="toggle-label">Отримувати сповіщення про статус замовлення на Email</span>
            <span class="toggle-desc">Ви отримаєте лист при зміні статусу вашого замовлення</span>
          </div>
          <div class="toggle-switch" :class="{ 'toggle-switch--active': notifyEmailOnOrderStatus }">
            <input
              v-model="notifyEmailOnOrderStatus"
              type="checkbox"
              class="toggle-checkbox"
              @change="handleSettingsChange"
            />
            <span class="toggle-slider"></span>
          </div>
        </label>
      </div>
    </section>

    <!-- ═══════════════ TOAST ═══════════════ -->
    <Transition name="toast">
      <div v-if="showToast" class="toast">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
        {{ toastMessage }}
      </div>
    </Transition>

    <!-- ═══════════════ МОДАЛКА ЗМІНИ ПАРОЛЯ ═══════════════ -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="isPasswordModalOpen" class="modal-overlay" @click.self="isPasswordModalOpen = false">
          <div class="modal-card">
            <div class="modal-header">
              <h3 class="modal-title">Змінити пароль</h3>
              <button class="modal-close" @click="isPasswordModalOpen = false">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>

            <div class="modal-body">
              <div class="field-group">
                <label class="field-label">Старий пароль</label>
                <input v-model="oldPassword" type="password" placeholder="••••••••" class="field-input" />
              </div>
              <div class="field-group">
                <label class="field-label">Новий пароль</label>
                <input v-model="newPassword" type="password" placeholder="••••••••" class="field-input" />
              </div>
              <div class="field-group">
                <label class="field-label">Повторіть новий пароль</label>
                <input v-model="repeatPassword" type="password" placeholder="••••••••" class="field-input" :class="{ 'field-input--error': passwordError }" />
              </div>
              <p v-if="passwordError" class="field-error">{{ passwordError }}</p>
            </div>

            <div class="modal-footer">
              <button class="btn-secondary" @click="isPasswordModalOpen = false; passwordError = ''">Скасувати</button>
              <button class="btn-primary" :disabled="isPasswordSaving" @click="handleChangePassword">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
                Зберегти пароль
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
/* ─── Root ────────────────────────────────────────────────────────────────── */
.settings-root {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* ─── Card ────────────────────────────────────────────────────────────────── */
.settings-card {
  background: linear-gradient(135deg, #131720 0%, #111827 100%);
  border: 1px solid #1a2235;
  border-radius: 1rem;
  overflow: hidden;
  transition: border-color 0.3s ease;
}
.settings-card:hover {
  border-color: #243049;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #141d2b;
  background: rgba(13, 17, 23, 0.4);
}
.card-header-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: rgba(249, 115, 22, 0.12);
  color: #f97316;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.card-header-icon--purple {
  background: rgba(139, 92, 246, 0.12);
  color: #8b5cf6;
}
.card-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: #e2e8f0;
  letter-spacing: 0.01em;
}
.card-body {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* ─── Avatar ──────────────────────────────────────────────────────────────── */
.avatar-section {
  display: flex;
  align-items: center;
  gap: 1.25rem;
}
.avatar-wrapper {
  position: relative;
  width: 72px;
  height: 72px;
  flex-shrink: 0;
}
.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #2a3a52;
}
.avatar-placeholder {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: #0d1117;
  border: 2px solid #2a3a52;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #3d5070;
}
.avatar-badge {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 24px;
  height: 24px;
  background: #f97316;
  border-radius: 50%;
  border: 2.5px solid #131720;
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-actions {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.btn-upload {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: rgba(249, 115, 22, 0.1);
  border: 1px solid rgba(249, 115, 22, 0.2);
  border-radius: 0.625rem;
  color: #fb923c;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: 'Onest', sans-serif;
}
.btn-upload:hover {
  background: rgba(249, 115, 22, 0.18);
  border-color: rgba(249, 115, 22, 0.35);
}
.avatar-hint {
  font-size: 0.7rem;
  color: #3d5070;
  padding-left: 0.25rem;
}

/* ─── Divider ─────────────────────────────────────────────────────────────── */
.divider {
  height: 1px;
  background: linear-gradient(90deg, #1a2235 0%, transparent 100%);
}

/* ─── Field ───────────────────────────────────────────────────────────────── */
.field-group {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}
.field-label {
  font-size: 0.7rem;
  font-weight: 600;
  color: #4b6080;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}
.field-input-wrap {
  position: relative;
}
.field-icon {
  position: absolute;
  left: 0.875rem;
  top: 50%;
  transform: translateY(-50%);
  color: #3d5070;
  pointer-events: none;
}
.field-input {
  width: 100%;
  padding: 0.75rem 1rem;
  background: #0d1117;
  border: 1px solid #1e2535;
  border-radius: 0.75rem;
  color: #e2e8f0;
  font-size: 0.875rem;
  font-family: 'Onest', sans-serif;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}
.field-input-wrap .field-input {
  padding-left: 2.5rem;
}
.field-input:focus {
  border-color: #f97316;
  box-shadow: 0 0 0 3px rgba(249, 115, 22, 0.1);
}
.field-input--error {
  border-color: #ef4444 !important;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.1) !important;
}
.field-input::placeholder {
  color: #2a3a52;
}
.field-error {
  font-size: 0.75rem;
  color: #ef4444;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

/* ─── Buttons ─────────────────────────────────────────────────────────────── */
.profile-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  padding-top: 0.25rem;
}
.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 1.25rem;
  background: linear-gradient(135deg, #f97316 0%, #ea580c 100%);
  border: none;
  border-radius: 0.75rem;
  color: #fff;
  font-size: 0.8rem;
  font-weight: 700;
  font-family: 'Onest', sans-serif;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 14px rgba(249, 115, 22, 0.25);
}
.btn-primary:hover {
  background: linear-gradient(135deg, #fb923c 0%, #f97316 100%);
  box-shadow: 0 6px 20px rgba(249, 115, 22, 0.35);
  transform: translateY(-1px);
}
.btn-primary:active {
  transform: scale(0.97);
}

.btn-secondary {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 1.25rem;
  background: transparent;
  border: 1px solid #1e2535;
  border-radius: 0.75rem;
  color: #94a3b8;
  font-size: 0.8rem;
  font-weight: 600;
  font-family: 'Onest', sans-serif;
  cursor: pointer;
  transition: all 0.2s ease;
}
.btn-secondary:hover {
  background: rgba(255, 255, 255, 0.04);
  border-color: #2a3a52;
  color: #e2e8f0;
}

/* ─── Toggle / Тумблер ────────────────────────────────────────────────────── */
.toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  cursor: pointer;
  padding: 0.25rem 0;
}
.toggle-info {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  flex: 1;
  min-width: 0;
}
.toggle-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #cbd5e1;
}
.toggle-desc {
  font-size: 0.72rem;
  color: #475569;
}
.toggle-switch {
  position: relative;
  width: 44px;
  height: 24px;
  flex-shrink: 0;
}
.toggle-checkbox {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
  z-index: 2;
}
.toggle-slider {
  position: absolute;
  inset: 0;
  background: #1e293b;
  border-radius: 12px;
  border: 1px solid #2a3a52;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.toggle-slider::after {
  content: '';
  position: absolute;
  top: 3px;
  left: 3px;
  width: 16px;
  height: 16px;
  background: #475569;
  border-radius: 50%;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.toggle-switch--active .toggle-slider {
  background: rgba(249, 115, 22, 0.2);
  border-color: rgba(249, 115, 22, 0.4);
}
.toggle-switch--active .toggle-slider::after {
  transform: translateX(20px);
  background: #f97316;
  box-shadow: 0 0 8px rgba(249, 115, 22, 0.5);
}

/* ─── Toast ───────────────────────────────────────────────────────────────── */
.toast {
  position: fixed;
  bottom: 2rem;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  background: linear-gradient(135deg, #065f46 0%, #064e3b 100%);
  border: 1px solid #10b981;
  border-radius: 0.75rem;
  color: #a7f3d0;
  font-size: 0.8rem;
  font-weight: 600;
  font-family: 'Onest', sans-serif;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.4);
  z-index: 9999;
}
.toast-enter-active {
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}
.toast-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 1, 1);
}
.toast-enter-from {
  opacity: 0;
  transform: translateX(-50%) translateY(20px) scale(0.95);
}
.toast-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(10px) scale(0.97);
}

/* ─── Modal ───────────────────────────────────────────────────────────────── */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 9000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.65);
  backdrop-filter: blur(6px);
  padding: 1rem;
}
.modal-card {
  width: 100%;
  max-width: 440px;
  background: linear-gradient(135deg, #141b27 0%, #111827 100%);
  border: 1px solid #1a2235;
  border-radius: 1rem;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.6);
}
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #141d2b;
  background: rgba(13, 17, 23, 0.5);
}
.modal-title {
  font-size: 1rem;
  font-weight: 700;
  color: #e2e8f0;
  margin: 0;
}
.modal-close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s ease;
}
.modal-close:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #94a3b8;
}
.modal-body {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.15rem;
}
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1rem 1.5rem;
  border-top: 1px solid #141d2b;
  background: rgba(13, 17, 23, 0.4);
}

/* ─── Modal transitions ──────────────────────────────────────────────────── */
.modal-enter-active {
  transition: opacity 0.3s ease;
}
.modal-enter-active .modal-card {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-leave-active .modal-card {
  transition: all 0.2s ease;
}
.modal-enter-from {
  opacity: 0;
}
.modal-enter-from .modal-card {
  transform: scale(0.95) translateY(10px);
  opacity: 0;
}
.modal-leave-to {
  opacity: 0;
}
.modal-leave-to .modal-card {
  transform: scale(0.97);
  opacity: 0;
}

/* ─── Responsive ──────────────────────────────────────────────────────────── */
@media (max-width: 480px) {
  .avatar-section {
    flex-direction: column;
    text-align: center;
  }
  .avatar-actions {
    align-items: center;
  }
  .profile-actions {
    flex-direction: column;
  }
  .profile-actions .btn-primary,
  .profile-actions .btn-secondary {
    width: 100%;
    justify-content: center;
  }
  .modal-footer {
    flex-direction: column-reverse;
  }
  .modal-footer .btn-primary,
  .modal-footer .btn-secondary {
    width: 100%;
    justify-content: center;
  }
}
</style>
