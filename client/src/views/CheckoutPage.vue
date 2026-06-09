<script setup lang="ts">
import { ref } from 'vue'
import Header from '../components/layout/Header.vue'
import Footer from '../components/layout/Footer.vue'
import CheckoutForm from '../components/forms/CheckoutForm.vue'
import CheckoutSummary from '../components/forms/CheckoutSummary.vue'

const checkoutFormRef = ref<InstanceType<typeof CheckoutForm> | null>(null)
</script>

<template>
  <div class="min-h-screen bg-[#0f1117] flex flex-col font-['Onest'] text-white">

    <Header :showCategories="false" />

    <main class="flex-1 w-full max-w-[1536px] mx-auto px-4 md:px-6 py-8 md:py-12 flex flex-col gap-6 md:gap-8">

      <h1 class="text-2xl md:text-3xl font-bold font-['Unbounded']">Оформлення замовлення</h1>

      <div class="flex flex-col lg:flex-row gap-8 items-start">

        <div class="w-full lg:w-2/3 order-2 lg:order-1">
          <CheckoutForm ref="checkoutFormRef" />
        </div>

        <div class="w-full lg:w-1/3 lg:sticky lg:top-6 order-1 lg:order-2">
          <CheckoutSummary
            :discountAmount="checkoutFormRef?.discountAmount ?? 0"
            :promoDiscount="checkoutFormRef?.promoDiscount ?? null"
          />
        </div>

      </div>
    </main>

    <Footer />
  </div>
</template>
