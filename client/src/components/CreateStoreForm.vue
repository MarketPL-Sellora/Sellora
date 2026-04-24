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
  // category REMOVED — not part of backend schema
  description: '',
  city: '',         // maps to `address` in payload
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

// —— Лічильник символів опису ——
const descriptionLength = computed(() => storeForm.description.length)

// ─── Стан сабміту ─────────────────────────────────────────────────────────────
const submitted   = ref(false)
const isLoading   = ref(false)
const apiError    = ref<string | null>(null)

// ─── Валідація ────────────────────────────────────────────────────────────────
// Phone regex: digits only, 7–15 characters (ITU-T E.164 range minus country code)
const PHONE_REGEX = /^\d{7,15}$/

const errors = reactive({
  name:        false,
  city:        false,
  phoneNumber: false,
})

function validate(): boolean {
  errors.name        = !storeForm.name.trim()
  errors.city        = !storeForm.city.trim()
  // Strip spaces/dashes before regex check so users can type "050 123 45 67"
  errors.phoneNumber = !PHONE_REGEX.test(phoneNumber.value.replace(/[\s\-()]/g, ''))
  return !errors.name && !errors.city && !errors.phoneNumber
}

// ─── Збереження форми ─────────────────────────────────────────────────────────
async function handleCreate() {
  submitted.value = true
  apiError.value  = null

  if (!validate()) return

  const contactPhone = phoneCode.value + phoneNumber.value.replace(/[\s\-()]/g, '')

  // logoUrl: empty string for now — file upload endpoint not yet specified
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

    // Emit enriched data so CabinetPage can populate myStore immediately
    emit('created', {
      ...payload,
      logoFile: logoFile.value,
      // Pass back whatever the server returned (may include id, etc.)
      ...responseData,
    })
  } catch (err: unknown) {
    // Surface a human-readable message without crashing
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
    <div class="self-stretch h-[3px] bg-gradient-to-r from-orange-500 via-orange-400 to-orange-400/0"></div>
    <div class="self-stretch px-8 pt-8 pb-6 border-b border-[#141d2b] flex flex-col justify-start items-center gap-[4.90px]">
      <div class="self-stretch pt-1.5 pb-[0.84px] flex flex-col justify-start items-center">
        <div class="text-center justify-center text-slate-100 text-2xl font-extrabold font-['Onest'] leading-7">Відкрити свій магазин</div>
      </div>
      <div class="self-stretch pb-[0.54px] flex flex-col justify-start items-center">
        <div class="text-center justify-center text-slate-500 text-sm font-normal font-['Onest'] leading-5">Заповніть дані, щоб почати продавати.<br/>Налаштування можна змінити пізніше.</div>
      </div>
    </div>
    <div class="self-stretch px-8 py-7 flex flex-col justify-start items-start gap-7">

      <!-- ── API-рівнева помилка (показується тільки після невдалого запиту) ── -->
      <!-- NOTE: only v-if/text binding added; zero style changes -->
      <div
        v-if="apiError"
        class="self-stretch px-4 py-3 bg-red-500/10 rounded-xl outline outline-1 outline-offset-[-1px] outline-red-500/30"
      >
        <p class="text-red-400 text-sm font-normal font-['Onest'] leading-5">{{ apiError }}</p>
      </div>

      <div class="self-stretch h-36 relative flex justify-center">
        <div
          class="w-24 h-24 bg-[#0d1117] rounded-[48px] outline outline-2 outline-offset-[-2px] outline-[#2a3a52] inline-flex flex-col justify-center items-center cursor-pointer overflow-hidden relative z-10"
          @click="($refs.logoInput as HTMLInputElement).click()"
        >
          <img
            v-if="logoPreview"
            :src="logoPreview"
            alt="Логотип магазину"
            class="w-full h-full object-cover"
          />
          <div v-else data-svg-wrapper data-variant="4" class="relative">
            <svg width="22" height="22" viewBox="0 0 22 22" fill="none" xmlns="http://www.w3.org/2000/svg">
              <g clip-path="url(#clip0_152_3229)">
                <path d="M1.5 7.50021C1.5 6.96977 1.71071 6.46106 2.08579 6.08599C2.46086 5.71092 2.96957 5.50021 3.5 5.50021H4.44C4.76851 5.4996 5.09181 5.41808 5.38133 5.26285C5.67085 5.10763 5.91768 4.88348 6.1 4.61021L6.7 3.71021C6.90165 3.46999 7.15696 3.28057 7.44532 3.15723C7.73369 3.0339 8.04701 2.98011 8.36 3.00021H13.64C13.9685 3.00081 14.2918 3.08233 14.5813 3.23756C14.8709 3.39278 15.1177 3.61694 15.3 3.89021L15.9 4.79021C16.0823 5.06348 16.3291 5.28763 16.6187 5.44285C16.9082 5.59808 17.2315 5.6796 17.56 5.68021H18.5C19.0304 5.68021 19.5391 5.89092 19.9142 6.26599C20.2893 6.64106 20.5 7.14977 20.5 7.68021V17.0002C20.5 17.5306 20.2893 18.0393 19.9142 18.4144C19.5391 18.7895 19.0304 19.0002 18.5 19.0002H1.5C0.969567 19.0002 0.460859 18.7895 0.0857865 18.4144C-0.289286 18.0393 -0.5 17.5306 -0.5 17.0002V7.50021H1.5Z" stroke="#3D5070" stroke-width="1.35" stroke-linejoin="round"/>
                <path d="M11 14.5C12.6569 14.5 14 13.1569 14 11.5C14 9.84315 12.6569 8.5 11 8.5C9.34315 8.5 8 9.84315 8 11.5C8 13.1569 9.34315 14.5 11 14.5Z" stroke="#3D5070" stroke-width="1.35"/>
              </g>
              <defs>
                <clipPath id="clip0_152_3229">
                  <rect width="22" height="22" fill="white"/>
                </clipPath>
              </defs>
            </svg>
          </div>
          <div class="w-6 h-6 absolute -bottom-1 -right-1 bg-orange-500 rounded-xl outline outline-2 outline-offset-[-2px] outline-[#0d1117] inline-flex justify-center items-center">
            <div data-svg-wrapper data-variant="5" class="relative">
              <svg width="9" height="9" viewBox="0 0 9 9" fill="none" xmlns="http://www.w3.org/2000/svg">
                <g clip-path="url(#clip0_152_3233)">
                  <path d="M4.5 1V8M1 4.5H8" stroke="white" stroke-width="1.5" stroke-linecap="round"/>
                </g>
                <defs>
                  <clipPath id="clip0_152_3233">
                    <rect width="9" height="9" fill="white"/>
                  </clipPath>
                </defs>
              </svg>
            </div>
          </div>
        </div>
        <div class="absolute top-[104px] flex flex-col justify-start items-center w-full">
          <div class="text-[#3d5070] text-xs font-medium font-['Onest'] leading-4 tracking-wide text-center">Завантажити логотип</div>
          <div class="text-[#2a3a52] text-xs font-normal font-['Onest'] leading-4 mt-2 text-center">PNG, JPG до 2 МБ · рекомендовано 200×200</div>
        </div>
      </div>

      <div class="self-stretch flex flex-col justify-start items-start gap-4">
        <div class="self-stretch inline-flex justify-start items-center gap-3">
          <div class="justify-center text-slate-700 text-xs font-semibold font-['Onest'] leading-5">Основна інформація</div>
          <div class="flex-1 h-px bg-[#1a2235]"></div>
        </div>
        <div class="self-stretch inline-flex flex-col justify-start items-start gap-5">

          <!-- Назва магазину -->
          <div class="self-stretch inline-flex flex-col justify-start items-start gap-2">
            <div class="self-stretch flex flex-col justify-start items-start">
              <div class="self-stretch justify-center"><span class="text-[#4b6080] text-xs font-semibold font-['Onest'] uppercase leading-4 tracking-wide">Назва магазину </span><span class="text-orange-500 text-xs font-semibold font-['Onest'] uppercase leading-4 tracking-wide">*</span></div>
            </div>
            <input
              v-model="storeForm.name"
              type="text"
              placeholder="Наприклад: TechZone або StyleHub"
              :class="[
                'self-stretch px-4 py-3.5 bg-[#0d1117] rounded-2xl outline outline-1 outline-offset-[-1px] inline-flex justify-center items-start overflow-hidden text-slate-200 text-sm font-normal font-[\'Onest\'] placeholder-[#3d4f6b] focus:outline-none transition-colors',
                submitted && errors.name ? 'outline-red-500' : 'outline-[#1e2535]'
              ]"
              @input="errors.name = false"
            />
            <div v-if="submitted && errors.name" class="text-red-500 text-xs font-normal font-['Onest'] leading-4">Це поле є обов'язковим</div>
          </div>

          <!-- Контактний телефон -->
          <div class="self-stretch inline-flex flex-col justify-start items-start gap-2">
            <div class="self-stretch flex flex-col justify-start items-start">
              <div class="self-stretch justify-center"><span class="text-[#4b6080] text-xs font-semibold font-['Onest'] uppercase leading-4 tracking-wide">Контактний телефон </span><span class="text-orange-500 text-xs font-semibold font-['Onest'] uppercase leading-4 tracking-wide">*</span></div>
            </div>
            <div class="self-stretch relative flex flex-col justify-start items-start">
              <input
                v-model="phoneNumber"
                type="tel"
                placeholder="(050) 000 00 00"
                :class="[
                  'self-stretch pl-[72px] pr-4 py-3.5 bg-[#0d1117] rounded-2xl outline outline-1 outline-offset-[-1px] inline-flex justify-center items-start overflow-hidden text-slate-200 text-sm font-normal font-[\'Onest\'] placeholder-[#3d4f6b] focus:outline-none transition-colors',
                  submitted && errors.phoneNumber ? 'outline-red-500' : 'outline-[#1e2535]'
                ]"
                @input="errors.phoneNumber = false"
              />
              <div class="w-16 h-full left-0 top-0 absolute bg-[#0d1117] rounded-tl-2xl rounded-bl-2xl border-r border-[#1e2535] inline-flex justify-center items-center">
                <select
                  v-model="phoneCode"
                  class="bg-transparent text-center text-slate-200 text-sm font-normal font-['Onest'] leading-5 appearance-none cursor-pointer focus:outline-none w-full px-2"
                >
                  <option v-for="code in phoneCodes" :key="code" :value="code" class="bg-[#0d1117]">{{ code }}</option>
                </select>
              </div>
            </div>
            <div v-if="submitted && errors.phoneNumber" class="text-red-500 text-xs font-normal font-['Onest'] leading-4">
              Введіть коректний номер телефону (лише цифри, 7–15 символів)
            </div>
          </div>

          <!-- Категорія магазину — REMOVED (not in backend schema) -->
          <!--
          <div class="self-stretch inline-flex flex-col justify-start items-start gap-2">
            ...category select...
          </div>
          -->

          <!-- Фізична адреса магазину -->
          <div class="self-stretch inline-flex flex-col justify-start items-start gap-2">
            <div class="self-stretch flex flex-col justify-start items-start">
              <div class="self-stretch justify-center"><span class="text-[#4b6080] text-xs font-semibold font-['Onest'] uppercase leading-4 tracking-wide">Фізична адреса магазину </span><span class="text-orange-500 text-xs font-semibold font-['Onest'] uppercase leading-4 tracking-wide">*</span></div>
            </div>
            <div class="self-stretch relative flex flex-col justify-start items-start">
              <input
                v-model="storeForm.city"
                type="text"
                placeholder="Місто, вулиця, будинок"
                :class="[
                  'self-stretch pl-10 pr-4 py-3.5 bg-[#0d1117] rounded-2xl outline outline-1 outline-offset-[-1px] inline-flex justify-center items-start overflow-hidden text-slate-200 text-sm font-normal font-[\'Onest\'] placeholder-[#3d4f6b] focus:outline-none transition-colors',
                  submitted && errors.city ? 'outline-red-500' : 'outline-[#1e2535]'
                ]"
                @input="errors.city = false"
              />
              <div class="h-3.5 left-[14.39px] top-[17.29px] absolute flex flex-col justify-start items-start pointer-events-none">
                <div data-svg-wrapper data-variant="7" class="relative">
                  <svg width="14" height="14" viewBox="0 0 14 14" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <g clip-path="url(#clip0_152_3161)">
                      <path d="M7 1C4.79 1 3 2.79 3 5C3 8.25 7 13 7 13C7 13 11 8.25 11 5C11 2.79 9.21 1 7 1Z" stroke="#3D5070" stroke-width="1.3" stroke-linejoin="round"/>
                      <path d="M7.0001 6.39961C7.7733 6.39961 8.4001 5.77281 8.4001 4.99961C8.4001 4.22641 7.7733 3.59961 7.0001 3.59961C6.2269 3.59961 5.6001 4.22641 5.6001 4.99961C5.6001 5.77281 6.2269 6.39961 7.0001 6.39961Z" stroke="#3D5070" stroke-width="1.2"/>
                    </g>
                    <defs>
                      <clipPath id="clip0_152_3161">
                        <rect width="14" height="14" fill="white"/>
                      </clipPath>
                    </defs>
                  </svg>
                </div>
              </div>
            </div>
            <div v-if="submitted && errors.city" class="text-red-500 text-xs font-normal font-['Onest'] leading-4">Це поле є обов'язковим</div>
          </div>

        </div>
      </div>

      <div class="self-stretch flex flex-col justify-start items-start gap-2">
        <div class="self-stretch inline-flex justify-start items-center gap-3">
          <div class="justify-center text-slate-700 text-xs font-semibold font-['Onest'] leading-5">Про магазин</div>
          <div class="flex-1 h-px bg-[#1a2235]"></div>
        </div>
        <div class="self-stretch pt-2.5 flex flex-col justify-start items-start">
          <div class="self-stretch justify-center text-[#4b6080] text-xs font-semibold font-['Onest'] uppercase leading-4 tracking-wide">Опис магазину</div>
        </div>
        <textarea
          v-model="storeForm.description"
          maxlength="500"
          rows="4"
          placeholder="Розкажіть покупцям, що ви продаєте, чим відрізняєтесь від інших та чому варто обрати саме ваш магазин…"
          class="self-stretch px-4 pt-3 pb-4 bg-[#0d1117] rounded-2xl outline outline-1 outline-offset-[-1px] outline-[#1e2535] text-slate-200 text-sm font-normal font-['Onest'] leading-6 placeholder-[#3d4f6b] resize-none focus:outline-none"
        ></textarea>
        <div class="self-stretch pt-1.5 inline-flex justify-between items-start">
          <div class="self-stretch inline-flex flex-col justify-start items-start">
            <div class="justify-center text-[#3d4f6b] text-xs font-normal font-['Onest'] leading-4">Рекомендовано 80–300 символів</div>
          </div>
          <div class="self-stretch inline-flex flex-col justify-start items-start">
            <div class="justify-center"><span class="text-orange-500 text-xs font-semibold font-['Onest'] leading-4">{{ descriptionLength }}</span><span class="text-[#3d4f6b] text-xs font-normal font-['Onest'] leading-4"> / 500</span></div>
          </div>
        </div>
      </div>

    </div>

    <div class="self-stretch px-8 py-5 bg-[#0d1117] border-t border-[#141d2b] inline-flex justify-between items-center w-full">
      <div class="flex items-center">
        <div data-svg-wrapper data-variant="10" class="mr-2">
          <svg width="11" height="12" viewBox="0 0 11 12" fill="none" xmlns="http://www.w3.org/2000/svg">
            <g clip-path="url(#clip0_152_3203)">
              <path d="M5.4453 10.084C7.70082 10.084 9.52928 8.25551 9.52928 5.99999C9.52928 3.74447 7.70082 1.91602 5.4453 1.91602C3.18979 1.91602 1.36133 3.74447 1.36133 5.99999C1.36133 8.25551 3.18979 10.084 5.4453 10.084Z" stroke="#2A3A52" stroke-width="0.99825"/>
              <path d="M5.44482 4.18555V6.9082" stroke="#2A3A52" stroke-width="0.99825" stroke-linecap="round"/>
              <path d="M5.44499 8.722C5.6956 8.722 5.89876 8.51884 5.89876 8.26823C5.89876 8.01762 5.6956 7.81445 5.44499 7.81445C5.19437 7.81445 4.99121 8.01762 4.99121 8.26823C4.99121 8.51884 5.19437 8.722 5.44499 8.722Z" fill="#2A3A52"/>
            </g>
            <defs>
              <clipPath id="clip0_152_3203">
                <rect width="10.8906" height="12" fill="white"/>
              </clipPath>
            </defs>
          </svg>
        </div>
        <div class="text-[#2a3a52] text-xs font-normal font-['Onest'] leading-4 flex gap-1">
          Поля із <span class="text-orange-500 font-bold">*</span> є обов'язковими
        </div>
      </div>
      <div class="flex justify-start items-center gap-2.5">
        <button
          type="button"
          :disabled="isLoading"
          class="px-6 py-3 rounded-2xl outline outline-1 outline-offset-[-1px] outline-[#1e2535] inline-flex justify-center items-center cursor-pointer hover:bg-white/5 transition-colors"
        >
          <div class="text-center text-slate-500 text-xs font-medium font-['Onest'] leading-5">Скасувати</div>
        </button>
        <button
          type="button"
          :disabled="isLoading"
          class="pl-6 pr-6 py-3 bg-orange-500 rounded-2xl shadow-[0px_4px_18px_0px_rgba(249,115,22,0.35)] flex justify-center items-center gap-3 cursor-pointer hover:bg-orange-400 transition-colors active:scale-[0.98] disabled:opacity-60 disabled:cursor-not-allowed disabled:active:scale-100"
          @click="handleCreate"
        >
          <div data-svg-wrapper data-variant="11" class="relative">
            <!-- Spinner while loading, checkmark otherwise -->
            <svg v-if="isLoading" class="animate-spin" width="12" height="12" viewBox="0 0 12 12" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="6" cy="6" r="5" stroke="white" stroke-width="1.5" stroke-dasharray="20 10" stroke-linecap="round"/>
            </svg>
            <svg v-else width="12" height="14" viewBox="0 0 12 14" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M1.22656 7.40619L4.49799 10.6776L10.223 4.13477" stroke="white" stroke-width="1.47214" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="text-center text-white text-xs font-bold font-['Onest'] leading-5">
            {{ isLoading ? 'Збереження…' : 'Створити магазин' }}
          </div>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
select { -webkit-appearance: none; }
</style>
