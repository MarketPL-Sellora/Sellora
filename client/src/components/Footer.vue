<script setup lang="ts">
// ─── Types ────────────────────────────────────────────────────────────────────

interface FooterLink   { label: string; href: string }
interface FooterColumn { heading: string; links: FooterLink[] }
interface LegalLink    { label: string; href: string }
interface ContactRow   { icon: 'phone' | 'email' | 'location'; primary: string; secondary: string }
interface SocialLink   { id: 'facebook' | 'instagram' | 'telegram'; href: string; label: string }

// ─── Columns ─────────────────────────────────────────────────────────────────

const columns: FooterColumn[] = [
  {
    heading: 'Покупцям',
    links: [
      { label: 'Як зробити замовлення', href: '#' },
      { label: 'Оплата та доставка',    href: '#' },
      { label: 'Повернення товарів',    href: '#' },
      { label: 'Гарантія та сервіс',    href: '#' },
      { label: 'Групові покупки',       href: '#' },
      { label: 'Програма лояльності',   href: '#' },
    ],
  },
  {
    heading: 'Продавцям',
    links: [
      { label: 'Стати продавцем',    href: '#' },
      { label: 'Кабінет продавця',   href: '#' },
      { label: 'Тарифи та комісії',  href: '#' },
      { label: 'Групові акції',      href: '#' },
      { label: 'Реклама на Sellora', href: '#' },
      { label: 'Технічна підтримка', href: '#' },
    ],
  },
]

// ─── Contacts ─────────────────────────────────────────────────────────────────

const contacts: ContactRow[] = [
  { icon: 'phone',    primary: '0 800 100 500',            secondary: 'Безкоштовно, цілодобово'  },
  { icon: 'email',    primary: 'support@sellora.ua',        secondary: 'Відповідаємо за 2 години' },
  { icon: 'location', primary: 'Київ, вул. Хрещатик, 22',  secondary: 'Головний офіс'             },
]

// ─── Socials ──────────────────────────────────────────────────────────────────

const socials: SocialLink[] = [
  { id: 'facebook',  href: '#', label: 'Facebook'  },
  { id: 'instagram', href: '#', label: 'Instagram' },
  { id: 'telegram',  href: '#', label: 'Telegram'  },
]

// ─── Legal ───────────────────────────────────────────────────────────────────

const legalLinks: LegalLink[] = [
  { label: 'Умови використання',        href: '#' },
  { label: 'Політика конфіденційності', href: '#' },
  { label: 'Cookie',                    href: '#' },
]

const currentYear = new Date().getFullYear()
</script>

<template>
  <footer class="w-full bg-[#0a0b0f] border-t border-[#1c1f2a]/50">
    <div class="w-full max-w-[1536px] mx-auto px-6 py-12 flex flex-col gap-10">

      <!-- ── Columns row ──────────────────────────────────────────────────── -->
      <div class="w-full flex flex-col md:flex-row justify-between items-start gap-8 md:gap-4 flex-wrap">

        <!-- Brand column -->
        <div class="flex-1 min-w-[200px] self-stretch inline-flex flex-col justify-start items-start gap-4">

          <!-- Logo + wordmark -->
          <div class="inline-flex items-center gap-2">
            <img
              src="../assets/logo.png"
              alt="Sellora"
              class="w-auto h-8 rounded-lg object-contain"
            />
            <span class="text-orange-500 text-lg font-bold font-['Unbounded'] leading-7">
              Sellora
            </span>
          </div>

          <!-- Tagline -->
          <p class="text-[#3d4158] text-sm font-normal font-['Onest'] leading-6">
            Маркетплейс нового покоління з груповими<br />знижками та гарантією якості.
          </p>

          <!-- Social icons -->
          <div class="inline-flex gap-3">
            <a
              v-for="soc in socials"
              :key="soc.id"
              :href="soc.href"
              :aria-label="soc.label"
              class="w-8 h-8 bg-[#1c1f2a] rounded-lg flex justify-center items-center transition-all duration-150 hover:bg-white/10 hover:scale-110 active:scale-95 focus:outline-none focus:ring-2 focus:ring-orange-500/40 group"
            >
              <!-- Facebook -->
              <svg
                v-if="soc.id === 'facebook'"
                class="w-4 h-4 text-[#787d99] group-hover:text-white transition-colors duration-150"
                viewBox="0 0 16 16" fill="currentColor"
              >
                <path d="M14 8a6 6 0 10-6.94 5.93V9.84H5.4V8h1.66V6.61c0-1.64.98-2.55 2.48-2.55.72 0 1.47.13 1.47.13v1.61h-.83c-.81 0-1.07.5-1.07 1.02V8h1.82l-.29 1.84H9.11v4.09A6 6 0 0014 8z"/>
              </svg>

              <!-- Instagram -->
              <svg
                v-else-if="soc.id === 'instagram'"
                class="w-4 h-4 text-[#787d99] group-hover:text-white transition-colors duration-150"
                viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.3"
              >
                <rect x="2" y="2" width="12" height="12" rx="3.5"/>
                <circle cx="8" cy="8" r="2.8"/>
                <circle cx="11.5" cy="4.5" r="0.8" fill="currentColor" stroke="none"/>
              </svg>

              <!-- Telegram -->
              <svg
                v-else-if="soc.id === 'telegram'"
                class="w-4 h-4 text-[#787d99] group-hover:text-white transition-colors duration-150"
                viewBox="0 0 16 16" fill="currentColor"
              >
                <path d="M2.27 7.7 13.2 3.4a.5.5 0 0 1 .64.65l-1.9 9a.5.5 0 0 1-.77.3L8.3 11.1l-1.7 1.6a.5.5 0 0 1-.84-.37V9.98L2.13 8.63a.5.5 0 0 1 .14-.93z"/>
                <path d="m5.76 9.98 5.53-5.3-6.17 4.27.64 1.03z" fill="none"/>
              </svg>
            </a>
          </div>
        </div>

        <!-- Link columns (v-for) -->
        <div
          v-for="col in columns"
          :key="col.heading"
          class="flex-1 min-w-[160px] self-stretch inline-flex flex-col justify-start items-start gap-4"
        >
          <span class="text-[#c8ccdf] text-sm font-normal font-['Onest'] uppercase leading-5 tracking-wide">
            {{ col.heading }}
          </span>
          <div class="flex flex-col gap-2.5">
            <a
              v-for="link in col.links"
              :key="link.label"
              :href="link.href"
              class="py-0.5 text-[#5a5f7a] text-sm font-normal font-['Onest'] leading-5 transition-colors duration-150 hover:text-orange-500"
            >
              {{ link.label }}
            </a>
          </div>
        </div>

        <!-- Contacts column -->
        <div class="flex-1 min-w-[200px] self-stretch inline-flex flex-col justify-start items-start gap-4">
          <span class="text-[#c8ccdf] text-sm font-normal font-['Onest'] uppercase leading-5 tracking-wide">
            Контакти
          </span>
          <div class="flex flex-col gap-3">
            <div
              v-for="row in contacts"
              :key="row.primary"
              class="inline-flex justify-start items-start gap-2.5"
            >
              <!-- Phone icon -->
              <svg
                v-if="row.icon === 'phone'"
                class="w-4 h-4 mt-0.5 shrink-0 text-orange-400"
                viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.2"
              >
                <path d="M3 2h2.5l1 2.5L5 6a7 7 0 0 0 5 5l1.5-1.5L14 10.5V13a2 2 0 0 1-2 2A11 11 0 0 1 1 4a2 2 0 0 1 2-2z" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>

              <!-- Email icon -->
              <svg
                v-else-if="row.icon === 'email'"
                class="w-4 h-4 mt-0.5 shrink-0 text-orange-400"
                viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.2"
              >
                <rect x="2" y="4" width="12" height="8" rx="1.5"/>
                <path d="M2 5.5l6 3.5 6-3.5" stroke-linecap="round"/>
              </svg>

              <!-- Location icon -->
              <svg
                v-else-if="row.icon === 'location'"
                class="w-4 h-4 mt-0.5 shrink-0 text-orange-400"
                viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.2"
              >
                <path d="M8 1.5A4.5 4.5 0 0 1 12.5 6c0 3.5-4.5 8.5-4.5 8.5S3.5 9.5 3.5 6A4.5 4.5 0 0 1 8 1.5z" stroke-linecap="round" stroke-linejoin="round"/>
                <circle cx="8" cy="6" r="1.5"/>
              </svg>

              <!-- Text -->
              <div class="flex flex-col gap-0.5 min-w-0">
                <span class="text-[#a0a5bf] text-sm font-normal font-['Onest'] leading-5">
                  {{ row.primary }}
                </span>
                <span class="text-[#3d4158] text-xs font-normal font-['Onest'] leading-4">
                  {{ row.secondary }}
                </span>
              </div>
            </div>
          </div>
        </div>

      </div>

      <!-- ── Bottom bar ───────────────────────────────────────────────────── -->
      <div class="w-full pt-6 border-t border-[#1c1f2a] flex flex-col sm:flex-row justify-center sm:justify-between items-center text-center sm:text-left gap-4">
        <span class="text-[#3d4158] text-xs font-normal font-['Onest'] leading-4">
          © {{ currentYear }} Sellora. Всі права захищені.
        </span>
        <div class="flex gap-5 flex-wrap">
          <a
            v-for="link in legalLinks"
            :key="link.label"
            :href="link.href"
            class="text-[#3d4158] text-xs font-normal font-['Onest'] leading-4 transition-colors duration-150 hover:text-orange-500"
          >
            {{ link.label }}
          </a>
        </div>
      </div>

    </div>
  </footer>
</template>
