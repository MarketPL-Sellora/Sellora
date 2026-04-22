<script setup lang="ts">
import Header from './Header.vue'
import Footer from './Footer.vue'
import CheckoutForm from './CheckoutForm.vue'
import CheckoutSummary from './CheckoutSummary.vue'
</script>

<template>
  <div class="min-h-screen bg-[#0f1117] flex flex-col font-['Onest'] text-white">

    <Header :showCategories="false" />

    <main class="flex-1 w-full max-w-[1536px] mx-auto px-6 py-12 flex flex-col gap-8">

      <h1 class="text-3xl font-bold font-['Unbounded']">Оформлення замовлення</h1>

      <div class="flex gap-8 items-start">

        <div class="w-2/3">
          <CheckoutForm />
        </div>

        <div class="w-1/3 sticky top-6">
          <CheckoutSummary />
        </div>

      </div>
    </main>

    <Footer />
  </div>
</template>
