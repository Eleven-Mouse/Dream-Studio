<template>
  <div class="category-detail">
    <CategoriesDirectoryCard />
  </div>
</template>

<script setup>
import CategoriesDirectoryCard from '@/components/CategoriesDirectoryCard.vue'
</script>

<style scoped>
.category-detail {
  width: min(700px, 100%);
  margin: 40px auto;
  padding: 0 20px;
}

@media (max-width: 720px) {
  .category-detail {
    margin: 24px auto;
    padding: 0;
  }
}
</style>
