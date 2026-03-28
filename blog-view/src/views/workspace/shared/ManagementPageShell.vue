<template>
  <section class="content-shell">
    <slot />
  </section>
</template>

<style scoped>
.content-shell {
  position: relative;
  overflow: hidden;
  padding: 22px;
  border-radius: 28px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background:
    radial-gradient(circle at top right, var(--workspace-accent-soft, rgba(15, 118, 110, 0.08)), transparent 36%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(248, 250, 252, 0.88));
  box-shadow: 0 24px 54px rgba(15, 23, 42, 0.06);
}
</style>
