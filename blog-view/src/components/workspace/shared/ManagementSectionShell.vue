<template>
  <div class="management-shell" :class="{ plain: !bordered }">

  </div>
</template>

<script setup>
defineProps({
  showHeader: {
    type: Boolean,
    default: true,
  },
  bordered: {
    type: Boolean,
    default: true,
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

defineEmits(['refresh'])
</script>

<style scoped>
.management-shell {
  margin-top: 18px;
  border-top: 1px dashed rgba(15, 23, 42, 0.12);
}

.management-shell.plain {
  margin-top: 0;
  padding-top: 0;
  border-top: 0;
}

.management-head {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 10px;
}

@media (max-width: 720px) {
  .management-head {
    flex-direction: column;
  }
}
</style>
