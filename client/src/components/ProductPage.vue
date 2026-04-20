<script setup lang="ts">
import Header from './Header.vue'
import Breadcrumbs from './Breadcrumbs.vue'
import Footer from './Footer.vue'
import ProductDetails from './ProductDetails.vue';
import ProductGallery from './ProductGallery.vue';
import ProductTabs from './ProductTabs.vue';
import RelatedProducts from './RelatedProducts.vue';
</script>

<template>
  <div class="min-h-screen bg-[#0f1117] flex flex-col font-['Onest'] text-white">

    <Header :showCategories="false" />

    <main class="flex-1 w-full max-w-[1536px] mx-auto px-6 py-8 flex flex-col gap-12">

      <Breadcrumbs />

      <div class="grid grid-cols-2 gap-10 items-start">
        <div class="w-full">
          <ProductGallery />
        </div>

        <div class="w-full">
          <ProductDetails />
        </div>
      </div>

      <div class="flex flex-col gap-16">
        <ProductTabs />

        <RelatedProducts />
      </div>

    </main>

    <Footer />

  </div>
</template>
