<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useUserStore } from '../state/userStore'

// ─── Типи ────────────────────────────────────────────────────────────────────
const emit = defineEmits<{
  (e: 'created', formData: Record<string, unknown>): void
}>()

const userStore = useUserStore()

// ─── Реактивний стан форми ────────────────────────────────────────────────────
const storeForm = reactive({
  name: '',
  description: '',
  city: '',
})

// —— Телефон ——
const phoneCode   = ref('+380')
const phoneNumber = ref('')
const phoneCodes = ['+380', '+48', '+44', '+1', '+49', '+420', '+33', '+39', '+34']

// —— Логотип ——
const logoFile    = ref<File | null>(null)
const logoPreview = ref<string | null>(null)

function handleLogoInput(event: Event) {
  const input = event.target as HTMLInputElement
  if (!input.files || !input.files[0]) return
  const file = input.files[0]
  logoFile.value = file
  if (logoPreview.value) URL.revokeObjectURL(logoPreview.value)
  logoPreview.value = URL.createObjectURL(file)
  input.value = ''
}

const descriptionLength = computed(() => storeForm.description.length)

// ─── Стан сабміту ─────────────────────────────────────────────────────────────
const submitted   = ref(false)
const isLoading   = ref(false)
const apiError    = ref<string | null>(null)

// ─── Валідація ────────────────────────────────────────────────────────────────
const PHONE_REGEX = /^\d{7,15}$/

const errors = reactive({
  name:        false,
  city:        false,
  phoneNumber: false,
})

function validate(): boolean {
  errors.name        = !storeForm.name.trim()
  errors.city        = !storeForm.city.trim()
  errors.phoneNumber = !PHONE_REGEX.test(phoneNumber.value.replace(/[\s\-()]/g, ''))
  return !errors.name && !errors.city && !errors.phoneNumber
}

async function handleCreate() {
  submitted.value = true
  apiError.value  = null

  if (!validate()) return

  const contactPhone = phoneCode.value + phoneNumber.value.replace(/[\s\-()]/g, '')

  const payload = {
    name:         storeForm.name.trim(),
    address:      storeForm.city.trim(),
    contactPhone,
    description:  storeForm.description.trim(),
    logoUrl:      '',
  }

  isLoading.value = true
  try {
    const responseData = await userStore.createStore(payload)
    emit('created', {
      ...payload,
      logoFile: logoFile.value,
      ...responseData,
    })
  } catch (err: unknown) {
    if (
      err &&
      typeof err === 'object' &&
      'response' in err &&
      (err as { response?: { data?: { message?: string } } }).response?.data?.message
    ) {
      apiError.value = (err as { response: { data: { message: string } } }).response.data.message
    } else {
      apiError.value = 'Виникла помилка при створенні магазину. Спробуйте ще раз.'
    }
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <input
    ref="logoInput"
    type="file"
    accept="image/png,image/jpeg,image/webp"
    class="hidden"
    @change="handleLogoInput"
  />

  <div class="w-full max-w-[600px] mx-auto bg-gradient-to-br from-[#141b27] via-gray-900 to-[#13161f] rounded-2xl shadow-[0px_25px_50px_-12px_rgba(0,0,0,0.60)] outline outline-1 outline-offset-[-1px] outline-[#1a2235] flex flex-col justify-start items-start overflow-hidden">
    <div class="self-stretch h-[3px] bg-gradient-to-r from-orange-500 via-orange-400 to-orange-400/0 shrink-0"></div>

    <div class="self-stretch px-5 md:px-8 pt-8 pb-6 border-b border-[#141d2b] flex flex-col justify-start items-center gap-2">
      <div class="text-center text-slate-100 text-xl md:text-2xl font-extrabold font-['Onest'] leading-7">Відкрити свій магазин</div>
      <div class="text-center text-slate-500 text-xs md:text-sm font-normal font-['Onest'] leading-5 px-2">
        Заповніть дані, щоб почати продавати.<br class="hidden sm:block"/> Налаштування можна змінити пізніше.
      </div>
    </div>

    <div class="self-stretch px-5 md:px-8 py-7 flex flex-col justify-start items-start gap-7">

      <div v-if="apiError" class="self-stretch px-4 py-3 bg-red-500/10 rounded-xl outline outline-1 outline-red-500/30">
        <p class="text-red-400 text-sm font-normal font-['Onest'] leading-5">{{ apiError }}</p>
      </div>

      <div class="self-stretch h-36 relative flex justify-center">
        <div
          class="w-24 h-24 bg-[#0d1117] rounded-full outline outline-2 outline-offset-[-2px] outline-[#2a3a52] inline-flex flex-col justify-center items-center cursor-pointer overflow-hidden relative z-10"
          @click="($refs.logoInput as HTMLInputElement).click()"
        >
          <img v-if="logoPreview" :src="logoPreview" alt="Логотип" class="w-full h-full object-cover" />
          <svg v-else width="22" height="22" viewBox="0 0 22 22" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M1.5 7.50021C1.5 6.96977 1.71071 6.46106 2.08579 6.08599C2.46086 5.71092 2.96957 5.50021 3.5 5.50021H4.44C4.76851 5.4996 5.09181 5.41808 5.38133 5.26285C5.67085 5.10763 5.91768 4.88348 6.1 4.61021L6.7 3.71021C6.90165 3.46999 7.15696 3.28057 7.44532 3.15723C7.73369 3.0339 8.04701 2.98011 8.36 3.00021H13.64C13.9685 3.00081 14.2918 3.08233 14.5813 3.23756C14.8709 3.39278 15.1177 3.61694 15.3 3.89021L15.9 4.79021V17C15.9 18.5 1.5 18.5 1.5 17V7.5Z" stroke="#3D5070" stroke-width="1.35"/>
            <circle cx="11" cy="11.5" r="3" stroke="#3D5070" stroke-width="1.35"/>
          </svg>
          <div class="w-6 h-6 absolute -bottom-1 -right-1 bg-orange-500 rounded-full border-2 border-[#0d1117] flex justify-center items-center">
            <svg width="10" height="10" viewBox="0 0 10 10" fill="none" stroke="white" stroke-width="1.5"><path d="M5 1v8M1 5h8" stroke-linecap="round"/></svg>
          </div>
        </div>
        <div class="absolute top-[104px] flex flex-col justify-start items-center w-full">
          <div class="text-[#3d5070] text-xs font-semibold uppercase tracking-wide">Завантажити логотип</div>
          <div class="text-[#2a3a52] text-[10px] md:text-xs font-normal mt-1">PNG, JPG до 2 МБ</div>
        </div>
      </div>

      <div class="self-stretch flex flex-col gap-6">
        <div class="self-stretch inline-flex items-center gap-3">
          <div class="text-slate-700 text-xs font-semibold uppercase tracking-widest">Основна інформація</div>
          <div class="flex-1 h-px bg-[#1a2235]"></div>
        </div>

        <div class="flex flex-col gap-2">
          <label class="text-[#4b6080] text-xs font-semibold uppercase">Назва магазину <span class="text-orange-500">*</span></label>
          <input
            v-model="storeForm.name"
            type="text"
            placeholder="Наприклад: TechZone"
            :class="[
              'w-full px-4 py-3.5 bg-[#0d1117] rounded-xl outline outline-1 text-slate-200 text-sm focus:outline-none transition-all',
              submitted && errors.name ? 'outline-red-500' : 'outline-[#1e2535]'
            ]"
            @input="errors.name = false"
          />
        </div>

        <div class="flex flex-col gap-2">
          <label class="text-[#4b6080] text-xs font-semibold uppercase">Контактний телефон <span class="text-orange-500">*</span></label>
          <div class="relative w-full">
            <input
              v-model="phoneNumber"
              type="tel"
              placeholder="(050) 000 00 00"
              :class="[
                'w-full pl-24 pr-4 py-3.5 bg-[#0d1117] rounded-xl outline outline-1 text-slate-200 text-sm focus:outline-none transition-all',
                submitted && errors.phoneNumber ? 'outline-red-500' : 'outline-[#1e2535]'
              ]"
              @input="errors.phoneNumber = false"
            />
            <div class="w-20 h-full left-0 top-0 absolute bg-[#0d1117] rounded-l-xl border-r border-[#1e2535] flex items-center justify-center">
              <select v-model="phoneCode" class="bg-transparent text-slate-200 text-sm font-normal outline-none cursor-pointer w-full text-center px-1">
                <option v-for="code in phoneCodes" :key="code" :value="code" class="bg-[#0d1117]">{{ code }}</option>
              </select>
            </div>
          </div>
          <div v-if="submitted && errors.phoneNumber" class="text-red-500 text-[10px] mt-1 leading-3">Введіть коректний номер (7–15 цифр)</div>
        </div>

        <div class="flex flex-col gap-2">
          <label class="text-[#4b6080] text-xs font-semibold uppercase">Фізична адреса <span class="text-orange-500">*</span></label>
          <div class="relative w-full">
            <input
              v-model="storeForm.city"
              type="text"
              placeholder="Місто, вулиця, будинок"
              :class="[
                'w-full pl-10 pr-4 py-3.5 bg-[#0d1117] rounded-xl outline outline-1 text-slate-200 text-sm focus:outline-none transition-all',
                submitted && errors.city ? 'outline-red-500' : 'outline-[#1e2535]'
              ]"
              @input="errors.city = false"
            />
            <svg class="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-[#3D5070]" viewBox="0 0 14 14" fill="none" stroke="currentColor"><path d="M7 1C4.79 1 3 2.79 3 5C3 8.25 7 13 7 13C7 13 11 8.25 11 5C11 2.79 9.21 1 7 1Z" stroke-width="1.3" stroke-linejoin="round"/></svg>
          </div>
        </div>
      </div>

      <div class="self-stretch flex flex-col gap-2">
        <div class="text-slate-700 text-xs font-semibold uppercase tracking-widest">Про магазин</div>
        <textarea
          v-model="storeForm.description"
          maxlength="500"
          rows="4"
          placeholder="Розкажіть про ваш магазин…"
          class="w-full px-4 py-3 bg-[#0d1117] rounded-xl outline outline-1 outline-[#1e2535] text-slate-200 text-sm leading-6 resize-none focus:outline-none"
        ></textarea>
        <div class="flex justify-between text-[10px] font-normal text-[#3d4f6b]">
          <span>Рекомендовано 80–300 символів</span>
          <span><span class="text-orange-500 font-bold">{{ descriptionLength }}</span> / 500</span>
        </div>
      </div>

    </div>

    <div class="self-stretch px-5 md:px-8 py-5 bg-[#0d1117] border-t border-[#141d2b] flex flex-col sm:flex-row justify-between items-center gap-4 w-full">
      <div class="flex items-center gap-2">
        <svg class="w-4 h-4 text-[#2A3A52]" viewBox="0 0 12 12" fill="none" stroke="currentColor"><circle cx="6" cy="6" r="5"/><path d="M6 4v3M6 8.5v.5" stroke-linecap="round"/></svg>
        <span class="text-[#2a3a52] text-[10px] md:text-xs">Поля із <span class="text-orange-500 font-bold">*</span> є обов'язковими</span>
      </div>

      <div class="flex items-center gap-3 w-full sm:w-auto">
        <button
          type="button"
          :disabled="isLoading"
          class="flex-1 sm:flex-none px-6 py-2.5 rounded-xl border border-[#1e2535] text-slate-500 text-xs font-medium hover:bg-white/5 transition-all"
        >
          Скасувати
        </button>
        <button
          type="button"
          :disabled="isLoading"
          class="flex-1 sm:flex-none px-6 py-2.5 bg-orange-500 rounded-xl shadow-lg flex justify-center items-center gap-2 hover:bg-orange-400 active:scale-95 transition-all disabled:opacity-50"
          @click="handleCreate"
        >
          <svg v-if="isLoading" class="animate-spin w-3 h-3" viewBox="0 0 12 12"><circle cx="6" cy="6" r="5" stroke="currentColor" stroke-width="2" stroke-dasharray="15 15"/></svg>
          <span class="text-white text-xs font-bold">{{ isLoading ? 'Збереження…' : 'Створити' }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
select { -webkit-appearance: none; }
textarea::placeholder { line-height: 1.5; }
</style>
