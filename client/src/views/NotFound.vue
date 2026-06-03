<script setup lang="ts">
import Header from '../components/layout/Header.vue'
import Footer from '../components/layout/Footer.vue'
</script>

<template>
  <div class="min-h-screen bg-[#0f1117] flex flex-col font-['Onest'] text-white">
    <Header :showCategories="false" />
    
    <main class="flex-1 flex flex-col items-center justify-center p-6 text-center">
      <div class="relative">
        <h1 class="text-[150px] md:text-[200px] font-black font-['Unbounded'] text-transparent bg-clip-text bg-gradient-to-br from-orange-500/20 to-orange-500/5 select-none leading-none">
          404
        </h1>
        <div class="absolute inset-0 flex items-center justify-center flex-col mt-8">
          <svg class="w-16 h-16 text-orange-500 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="1.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
        </div>
      </div>
      
      <h2 class="text-2xl md:text-3xl font-bold font-['Unbounded'] mt-2 text-white">Сторінку не знайдено</h2>
      <p class="text-gray-400 mt-3 max-w-md text-sm md:text-base leading-relaxed">
        Можливо, вона була видалена, перенесена, або ви просто ввели неправильну адресу. 
      </p>
      
      <router-link to="/" class="mt-8 px-8 py-3.5 bg-gradient-to-r from-orange-500 to-amber-500 rounded-xl text-white font-bold font-['Unbounded'] text-sm tracking-wide uppercase transition-all duration-200 hover:from-orange-400 hover:to-amber-400 hover:shadow-[0px_6px_20px_0px_rgba(249,115,22,0.4)] active:scale-95">
        Повернутися на головну
      </router-link>
    </main>

    <Footer />
  </div>
</template>
