<script setup lang="ts">
import { ref, reactive, computed } from 'vue'

// —— Оголошення подій форми ——
// 'created' — сигналізує батьківському CabinetPage, що магазин успішно створено.
// Передає formData з усіма даними форми.
const emit = defineEmits<{
  (e: 'created', formData: Record<string, unknown>): void
}>()

// ─── Реактивний стан форми ────────────────────────────────────────────────────

const storeForm = reactive({
  name: '',         // Назва магазину (обов'язкове)
  category: '',     // Категорія магазину (обов'язкове)
  description: '',  // Опис магазину (до 500 символів)
  city: '',         // Фізична адреса (обов'язкове)
  website: '',      // Сайт (необов'язково)
  instagram: '',    // Instagram (необов'язково)
})

// —— Телефон: код країни + номер ——
const phoneCode = ref('+380')
const phoneNumber = ref('')

// Коди країн для <select>
const phoneCodes = ['+380', '+48', '+44', '+1']

// —— Логотип магазину ——
const logoFile = ref<File | null>(null)
const logoPreview = ref<string | null>(null)

// Обробник вибору файлу логотипу
function handleLogoInput(event: Event) {
  const input = event.target as HTMLInputElement
  if (!input.files || !input.files[0]) return
  const file = input.files[0]
  logoFile.value = file
  // Створюємо blob URL для попереднього перегляду
  if (logoPreview.value) URL.revokeObjectURL(logoPreview.value)
  logoPreview.value = URL.createObjectURL(file)
  input.value = '' // Скидаємо input для повторного вибору того ж файлу
}

// —— Лічильник символів опису ——
const descriptionLength = computed(() => storeForm.description.length)

// —— Категорії для <select> ——
const categoryOptions = [
  'Електроніка', 'Одяг та взуття', 'Дім і сад',
  "Краса та здоров'я", 'Спорт', 'Авто',
  'Книги та канцелярія', 'Зоотовари', 'Інше',
]

// ─── Валідація ─────────────────────────────────────────────────────────────────

// Стан помилок — true якщо поле невалідне
const errors = reactive({
  name: false,
  category: false,
  city: false,
  phoneNumber: false,
})

// Перевірка чи була спроба сабміту (для показу помилок)
const submitted = ref(false)

// Валідація форми — повертає true якщо все ОК
function validate(): boolean {
  errors.name = !storeForm.name.trim()
  errors.category = !storeForm.category
  errors.city = !storeForm.city.trim()
  errors.phoneNumber = !phoneNumber.value.trim()
  return !errors.name && !errors.category && !errors.city && !errors.phoneNumber
}

// ─── Збереження форми ─────────────────────────────────────────────────────────

function handleCreate() {
  submitted.value = true

  // Якщо валідація не пройдена — не відправляємо
  if (!validate()) return

  const formData = {
    ...storeForm,
    phone: phoneCode.value + phoneNumber.value,
    logoFile: logoFile.value,
  }
  console.log('Створення магазину:', formData)

  // Повідомляємо батьківський компонент про успішне створення
  emit('created', formData)
}
</script>

<template>
  <!-- Прихований input для завантаження логотипу -->
  <input
    ref="logoInput"
    type="file"
    accept="image/png,image/jpeg,image/webp"
    class="hidden"
    @change="handleLogoInput"
  />

  <div class="w-[520px] max-w-[520px] bg-gradient-to-br from-[#141b27] via-gray-900 to-[#13161f] rounded-2xl shadow-[0px_25px_50px_-12px_rgba(0,0,0,0.60)] outline outline-1 outline-offset-[-1px] outline-[#1a2235] inline-flex flex-col justify-start items-start overflow-hidden">
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

      <!-- ── Зона завантаження логотипу ─────────────────────────────────────── -->
      <div class="self-stretch h-36 relative">
        <div
          class="w-24 h-24 left-[179px] top-0 absolute bg-[#0d1117] rounded-[48px] outline outline-2 outline-offset-[-2px] outline-[#2a3a52] inline-flex flex-col justify-center items-center cursor-pointer overflow-hidden"
          @click="($refs.logoInput as HTMLInputElement).click()"
        >
          <!-- Прев'ю завантаженого логотипу -->
          <img
            v-if="logoPreview"
            :src="logoPreview"
            alt="Логотип магазину"
            class="w-full h-full object-cover"
          />
          <!-- Іконка камери (якщо логотип не завантажено) -->
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
          <!-- Кнопка «+» -->
          <div class="w-6 h-6 left-[71.61px] top-[71.61px] absolute bg-orange-500 rounded-xl outline outline-2 outline-offset-[-2px] outline-[#0d1117] inline-flex justify-center items-center">
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
        <div class="left-[162.11px] top-[104px] absolute inline-flex flex-col justify-start items-start">
          <div class="justify-center text-[#3d5070] text-xs font-medium font-['Onest'] leading-4 tracking-wide">Завантажити логотип</div>
        </div>
        <div class="left-[111.59px] top-[128.27px] absolute inline-flex flex-col justify-start items-start">
          <div class="justify-center text-[#2a3a52] text-xs font-normal font-['Onest'] leading-4">PNG, JPG до 2 МБ · рекомендовано 200×200</div>
        </div>
      </div>

      <!-- ── Секція: Основна інформація ─────────────────────────────────────── -->
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
                  'self-stretch pl-14 pr-4 py-3.5 bg-[#0d1117] rounded-2xl outline outline-1 outline-offset-[-1px] inline-flex justify-center items-start overflow-hidden text-slate-200 text-sm font-normal font-[\'Onest\'] placeholder-[#3d4f6b] focus:outline-none transition-colors',
                  submitted && errors.phoneNumber ? 'outline-red-500' : 'outline-[#1e2535]'
                ]"
                @input="errors.phoneNumber = false"
              />
              <div class="w-12 h-12 left-0 top-0 absolute bg-[#0d1117] rounded-tl-2xl rounded-bl-2xl border-r border-[#1e2535] inline-flex justify-center items-center">
                <select
                  v-model="phoneCode"
                  class="bg-transparent text-center text-slate-500 text-xs font-semibold font-['Onest'] leading-5 appearance-none cursor-pointer focus:outline-none"
                >
                  <option v-for="code in phoneCodes" :key="code" :value="code">{{ code }}</option>
                </select>
              </div>
            </div>
            <div v-if="submitted && errors.phoneNumber" class="text-red-500 text-xs font-normal font-['Onest'] leading-4">Це поле є обов'язковим</div>
          </div>

          <!-- Категорія магазину -->
          <div class="self-stretch inline-flex flex-col justify-start items-start gap-2">
            <div class="self-stretch flex flex-col justify-start items-start">
              <div class="self-stretch justify-center"><span class="text-[#4b6080] text-xs font-semibold font-['Onest'] uppercase leading-4 tracking-wide">Категорія магазину </span><span class="text-orange-500 text-xs font-semibold font-['Onest'] uppercase leading-4 tracking-wide">*</span></div>
            </div>
            <div class="self-stretch relative flex flex-col justify-start items-start">
              <select
                v-model="storeForm.category"
                :class="[
                  'self-stretch pl-4 pr-10 py-3 bg-[#0d1117] rounded-2xl outline outline-1 outline-offset-[-1px] inline-flex justify-center items-center appearance-none cursor-pointer text-sm font-normal font-[\'Onest\'] leading-5 focus:outline-none transition-colors',
                  storeForm.category ? 'text-slate-200' : 'text-[#3d4f6b]',
                  submitted && errors.category ? 'outline-red-500' : 'outline-[#1e2535]'
                ]"
                @change="errors.category = false"
              >
                <option value="" disabled>Оберіть категорію…</option>
                <option v-for="cat in categoryOptions" :key="cat" :value="cat" class="text-slate-200 bg-[#0d1117]">{{ cat }}</option>
              </select>
              <div class="h-3.5 right-4 top-1/2 -translate-y-1/2 absolute flex flex-col justify-start items-start pointer-events-none">
                <div data-svg-wrapper data-variant="6" class="relative">
                  <svg width="14" height="14" viewBox="0 0 14 14" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M3 5L7 9L11 5" stroke="#3D5070" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </div>
              </div>
            </div>
            <div v-if="submitted && errors.category" class="text-red-500 text-xs font-normal font-['Onest'] leading-4">Це поле є обов'язковим</div>
          </div>

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

      <!-- ── Секція: Про магазин ────────────────────────────────────────────── -->
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
          rows="3"
          placeholder="Розкажіть покупцям, що ви продаєте, чим відрізняєтесь від інших та чому варто обрати саме ваш магазин…"
          class="self-stretch px-4 pt-3 pb-14 bg-[#0d1117] rounded-2xl outline outline-1 outline-offset-[-1px] outline-[#1e2535] text-slate-200 text-sm font-normal font-['Onest'] leading-6 placeholder-[#3d4f6b] resize-none focus:outline-none"
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

      <!-- ── Секція: Посилання ──────────────────────────────────────────────── -->
      <div class="self-stretch flex flex-col justify-start items-start gap-4">
        <div class="self-stretch inline-flex justify-start items-center gap-3">
          <div class="justify-center text-slate-700 text-xs font-semibold font-['Onest'] leading-5">Посилання</div>
          <div class="inline-flex flex-col justify-start items-start">
            <div class="justify-center text-[#2a3a52] text-[10.40px] font-medium font-['Onest'] leading-4">(необов'язково)</div>
          </div>
          <div class="flex-1 h-px bg-[#1a2235]"></div>
        </div>
        <div class="self-stretch inline-flex flex-col justify-start items-start gap-5">

          <!-- Сайт -->
          <div class="self-stretch inline-flex flex-col justify-start items-start gap-2">
            <div class="self-stretch flex flex-col justify-start items-start">
              <div class="self-stretch justify-center text-[#4b6080] text-xs font-semibold font-['Onest'] uppercase leading-4 tracking-wide">Сайт</div>
            </div>
            <div class="self-stretch relative flex flex-col justify-start items-start">
              <input
                v-model="storeForm.website"
                type="url"
                placeholder="https://myshop.ua"
                class="self-stretch pl-10 pr-4 py-3.5 bg-[#0d1117] rounded-2xl outline outline-1 outline-offset-[-1px] outline-[#1e2535] text-slate-200 text-sm font-normal font-['Onest'] placeholder-[#3d4f6b] focus:outline-none"
              />
              <div class="h-3.5 left-[14.39px] top-[17.30px] absolute flex flex-col justify-start items-start pointer-events-none">
                <div data-svg-wrapper data-variant="8" class="relative">
                  <svg width="14" height="14" viewBox="0 0 14 14" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <g clip-path="url(#clip0_152_3191)">
                  <path d="M7 12.5C10.0376 12.5 12.5 10.0376 12.5 7C12.5 3.96243 10.0376 1.5 7 1.5C3.96243 1.5 1.5 3.96243 1.5 7C1.5 10.0376 3.96243 12.5 7 12.5Z" stroke="#3D5070" stroke-width="1.2"/>
                  <path d="M1.5 7H12.5M7 1.5C7 1.5 5 4 5 7C5 10 7 12.5 7 12.5C7 12.5 9 10 9 7C9 4 7 1.5 7 1.5Z" stroke="#3D5070" stroke-width="1.2" stroke-linecap="round"/>
                  </g>
                  <defs>
                  <clipPath id="clip0_152_3191">
                  <rect width="14" height="14" fill="white"/>
                  </clipPath>
                  </defs>
                  </svg>
                </div>
              </div>
            </div>
          </div>

          <!-- Instagram -->
          <div class="self-stretch inline-flex flex-col justify-start items-start gap-2">
            <div class="self-stretch flex flex-col justify-start items-start">
              <div class="self-stretch justify-center text-[#4b6080] text-xs font-semibold font-['Onest'] uppercase leading-4 tracking-wide">Instagram</div>
            </div>
            <div class="self-stretch relative flex flex-col justify-start items-start">
              <input
                v-model="storeForm.instagram"
                type="text"
                placeholder="@myshop"
                class="self-stretch pl-10 pr-4 py-3.5 bg-[#0d1117] rounded-2xl outline outline-1 outline-offset-[-1px] outline-[#1e2535] text-slate-200 text-sm font-normal font-['Onest'] placeholder-[#3d4f6b] focus:outline-none"
              />
              <div class="h-3.5 left-[14.39px] top-[17.30px] absolute flex flex-col justify-start items-start pointer-events-none">
                <div data-svg-wrapper data-variant="9" class="relative">
                  <svg width="14" height="14" viewBox="0 0 14 14" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M9.5 1.5H4.5C2.84315 1.5 1.5 2.84315 1.5 4.5V9.5C1.5 11.1569 2.84315 12.5 4.5 12.5H9.5C11.1569 12.5 12.5 11.1569 12.5 9.5V4.5C12.5 2.84315 11.1569 1.5 9.5 1.5Z" stroke="#3D5070" stroke-width="1.2"/>
                  <path d="M7 9.5C8.38071 9.5 9.5 8.38071 9.5 7C9.5 5.61929 8.38071 4.5 7 4.5C5.61929 4.5 4.5 5.61929 4.5 7C4.5 8.38071 5.61929 9.5 7 9.5Z" stroke="#3D5070" stroke-width="1.2"/>
                  <path d="M10.4999 4.10039C10.8313 4.10039 11.0999 3.83176 11.0999 3.50039C11.0999 3.16902 10.8313 2.90039 10.4999 2.90039C10.1685 2.90039 9.8999 3.16902 9.8999 3.50039C9.8999 3.83176 10.1685 4.10039 10.4999 4.10039Z" fill="#3D5070"/>
                  </svg>
                </div>
              </div>
            </div>
          </div>

        </div>
      </div>

    </div>

    <!-- ── Нижня панель ────────────────────────────────────────────────────── -->
    <div class="self-stretch px-8 py-5 bg-[#0d1117] border-t border-[#141d2b] inline-flex justify-between items-center">
      <div class="w-40 h-8 relative">
        <div data-svg-wrapper data-variant="10" class="left-0 top-[10.80px] absolute">
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
        <div class="w-8 h-8 left-[17px] top-[-0.92px] absolute justify-center text-[#2a3a52] text-xs font-normal font-['Onest'] leading-4">Поля<br/>із</div>
        <div class="px-0.5 left-[58.56px] top-[8.39px] absolute inline-flex flex-col justify-start items-start">
          <div class="justify-center text-orange-500 text-xs font-normal font-['Onest'] leading-4">*</div>
        </div>
        <div class="w-20 h-8 left-[73.59px] top-[-0.60px] absolute justify-center text-[#2a3a52] text-xs font-normal font-['Onest'] leading-4">є<br/>обов'язковими</div>
      </div>
      <div class="flex justify-start items-center gap-2.5">
        <button
          type="button"
          class="px-6 pt-2.5 pb-3 rounded-2xl outline outline-1 outline-offset-[-1px] outline-[#1e2535] inline-flex flex-col justify-center items-center cursor-pointer hover:bg-white/5 transition-colors"
        >
          <div class="text-center justify-center text-slate-500 text-xs font-medium font-['Onest'] leading-5">Скасувати</div>
        </button>
        <button
          type="button"
          class="pl-6 pr-11 py-3 bg-orange-500 rounded-2xl shadow-[0px_4px_18px_0px_rgba(249,115,22,0.35)] flex justify-start items-center gap-6 cursor-pointer hover:bg-orange-400 transition-colors active:scale-[0.98]"
          @click="handleCreate"
        >
          <div data-svg-wrapper data-variant="11" class="relative">
            <svg width="12" height="14" viewBox="0 0 12 14" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M1.22656 7.40619L4.49799 10.6776L10.223 4.13477" stroke="white" stroke-width="1.47214" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="text-center justify-center text-white text-xs font-bold font-['Onest'] leading-5">Створити<br/>магазин</div>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Прибираємо дефолтний вигляд select на webkit */
select { -webkit-appearance: none; }
</style>
