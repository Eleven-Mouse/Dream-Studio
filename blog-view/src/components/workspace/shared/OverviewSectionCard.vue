<template>
  <section class="overview-section-card">
    <header class="section-header">
      <div class="section-header-copy">
        <h3>{{ title }}</h3>
        <p v-if="description">{{ description }}</p>
      </div>
      <slot name="header-extra" />
    </header>

    <div class="section-body">
      <slot />
    </div>
  </section>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true,
  },
  description: {
    type: String,
    default: '',
  },
})
</script>

<style scoped>
.overview-section-card {
  position: relative;
  overflow: hidden;
  padding: 22px;
  border-radius: 26px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(248, 250, 252, 0.88)),
    rgba(255, 255, 255, 0.9);
  box-shadow: 0 22px 48px rgba(15, 23, 42, 0.06);
}

.overview-section-card::before {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(148, 163, 184, 0.36), transparent);
}

.section-header {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.section-header-copy {
  max-width: 680px;
}

.section-header h3 {
  margin: 0;
  color: var(--workspace-title, #0f172a);
  font-size: 20px;
  line-height: 1.15;
  font-family: 'Noto Serif SC', 'Source Han Serif SC', Georgia, serif;
}

.section-header p {
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.65;
}

.section-body {
  position: relative;
  z-index: 1;
  min-width: 0;
}

@media (max-width: 720px) {
  .section-header {
    flex-direction: column;
  }
}
</style>
