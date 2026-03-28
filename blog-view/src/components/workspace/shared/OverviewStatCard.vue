<template>
  <article class="stat-card">
    <div class="stat-top">
      <span class="stat-label">{{ label }}</span>
      <span class="stat-dot"></span>
    </div>
    <strong class="stat-value">{{ value }}</strong>
    <p class="stat-hint">{{ hint }}</p>
  </article>
</template>

<script setup>
defineProps({
  label: {
    type: String,
    required: true,
  },
  value: {
    type: [String, Number],
    required: true,
  },
  hint: {
    type: String,
    default: '',
  },
})
</script>

<style scoped>
.stat-card {
  position: relative;
  overflow: hidden;
  padding: 22px;
  border-radius: 24px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background:
    radial-gradient(circle at top right, var(--workspace-accent-soft, rgba(15, 118, 110, 0.12)), transparent 38%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.92));
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.06);
  transition:
    transform 0.24s ease,
    box-shadow 0.24s ease,
    border-color 0.24s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
  border-color: rgba(148, 163, 184, 0.26);
  box-shadow: 0 24px 52px rgba(15, 23, 42, 0.1);
}

.stat-card::after {
  content: '';
  position: absolute;
  right: 20px;
  bottom: 18px;
  width: 72px;
  height: 72px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.04), rgba(15, 23, 42, 0.06));
  transform: rotate(12deg);
}

.stat-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.stat-label {
  display: block;
  color: #64748b;
  font-size: 13px;
  letter-spacing: 0.04em;
}

.stat-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: var(--workspace-accent, #0f766e);
  box-shadow: 0 0 0 6px var(--workspace-accent-soft, rgba(15, 118, 110, 0.12));
}

.stat-value {
  position: relative;
  z-index: 1;
  display: block;
  margin-top: 18px;
  color: var(--workspace-title, #0f172a);
  font-size: clamp(30px, 4vw, 38px);
  line-height: 0.96;
  font-family: 'Noto Serif SC', 'Source Han Serif SC', Georgia, serif;
}

.stat-hint {
  position: relative;
  z-index: 1;
  margin: 12px 0 0;
  color: #64748b;
  line-height: 1.6;
}
</style>
