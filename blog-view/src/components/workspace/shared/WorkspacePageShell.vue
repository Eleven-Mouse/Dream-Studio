<template>
  <section class="workspace-shell">
    <div class="shell-glow shell-glow-primary"></div>
    <div class="shell-glow shell-glow-secondary"></div>

    <div class="workspace-head">
      <div class="workspace-head-copy">
        <span class="head-kicker">Curated Workspace</span>
        <h2>{{ title }}</h2>
        <p>{{ description }}</p>
      </div>

      <div class="workspace-head-actions">
        <span class="head-status">{{ loading ? '正在同步内容' : '内容已准备就绪' }}</span>
        <el-button round :loading="loading" @click="$emit('refresh')">刷新内容</el-button>
      </div>
    </div>

    <div class="workspace-body">
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
    required: true,
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

defineEmits(['refresh'])
</script>

<style scoped>
.workspace-shell {
  position: relative;
  overflow: hidden;
  padding: 24px;
  border-radius: 30px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.9)),
    rgba(255, 255, 255, 0.92);
  box-shadow:
    0 28px 60px rgba(15, 23, 42, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.86);
}

.shell-glow {
  position: absolute;
  border-radius: 999px;
  pointer-events: none;
  filter: blur(12px);
  opacity: 0.7;
}

.shell-glow-primary {
  top: -54px;
  right: -28px;
  width: 180px;
  height: 180px;
  background: var(--workspace-accent-soft, rgba(15, 118, 110, 0.12));
}

.shell-glow-secondary {
  bottom: -90px;
  left: -40px;
  width: 220px;
  height: 220px;
  background: rgba(148, 163, 184, 0.12);
}

.workspace-head {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.14);
}

.workspace-head-copy {
  max-width: 720px;
}

.head-kicker {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--workspace-title, #0f172a);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.workspace-head h2 {
  margin: 16px 0 10px;
  color: var(--workspace-title, #0f172a);
  font-size: clamp(28px, 4vw, 36px);
  line-height: 1.08;
  font-family: 'Noto Serif SC', 'Source Han Serif SC', Georgia, serif;
}

.workspace-head p {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}

.workspace-head-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.head-status {
  display: inline-flex;
  align-items: center;
  min-height: 38px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background: rgba(255, 255, 255, 0.75);
  color: #475569;
  font-size: 13px;
}

.workspace-body {
  position: relative;
  z-index: 1;
}

@media (max-width: 720px) {
  .workspace-head {
    flex-direction: column;
    margin-bottom: 20px;
    padding-bottom: 16px;
  }

  .workspace-shell {
    padding: 18px;
    border-radius: 24px;
  }

  .workspace-head-actions {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
