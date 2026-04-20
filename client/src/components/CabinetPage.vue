<script setup lang="ts">
import Header from './Header.vue'
import Footer from './Footer.vue'
import CabinetSidebar from './CabinetSidebar.vue'
import CabinetGroupBuys from './CabinetGroupBuys.vue'
</script>

<template>
  <div class="min-h-screen bg-[#0f1117] flex flex-col font-['Onest'] text-white">

    <Header :showCategories="false" />

    <main class="flex-1 w-full max-w-[1536px] mx-auto px-6 py-12 flex gap-8 items-start">

      <div class="w-1/4 sticky top-6">
        <CabinetSidebar />
      </div>

      <div class="w-3/4">
        <CabinetGroupBuys />
      </div>

    </main>

    <Footer />

  </div>
</template>
