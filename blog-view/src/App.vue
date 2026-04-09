<template>
  <router-view v-slot="{ Component, route }">
    <transition :name="transitionName" mode="out-in">
      <component :is="Component" :key="topLevelRouteKey(route)" />
    </transition>
  </router-view>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const transitionName = ref('app-fade')

const topLevelRouteKey = (currentRoute) => currentRoute.matched?.[0]?.path || currentRoute.path
const isWorkspaceRoute = (path = '') => path.startsWith('/admin') || path.startsWith('/profile')

watch(
  () => route.fullPath,
  (to, from) => {
    const enteringWorkspace = isWorkspaceRoute(to)
    const leavingWorkspace = isWorkspaceRoute(from)

    if (to.startsWith('/admin') && from?.startsWith('/profile')) {
      transitionName.value = 'app-slide-left'
      return
    }

    if (to.startsWith('/profile') && from?.startsWith('/admin')) {
      transitionName.value = 'app-slide-right'
      return
    }

    if (enteringWorkspace && !leavingWorkspace) {
      transitionName.value = 'app-slide-left'
      return
    }

    if (!enteringWorkspace && leavingWorkspace) {
      transitionName.value = 'app-slide-right'
      return
    }

    transitionName.value = 'app-fade'
  },
  { immediate: true },
)
</script>

<style>
html,
body,
#app {
  min-height: 100%;
  background: var(--app-page-bg);
}

body {
  margin: 0;
  overflow-x: hidden;
}

* {
  box-sizing: border-box;
}

img,
video,
canvas,
iframe,
svg {
  max-width: 100%;
}

@media (max-width: 720px) {
  :root {
    --el-dialog-width: min(94vw, 560px);
  }

  body {
    overflow-wrap: anywhere;
    word-break: break-word;
    padding-bottom: var(--safe-area-bottom);
  }

  .el-button,
  .el-input__wrapper,
  .el-select__wrapper,
  .el-textarea__inner,
  .el-date-editor.el-input,
  .el-pagination button {
    min-height: var(--mobile-touch-min);
  }

  .el-dialog {
    width: min(94vw, 560px) !important;
    margin-top: max(5vh, 24px) !important;
    max-height: calc(100vh - max(5vh, 24px) - max(5vh, 24px));
  }

  .el-dialog__body {
    max-height: calc(100vh - 210px);
    overflow-y: auto;
  }
}

html[data-theme='dark']::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(16, 16, 27, 0.38);
  pointer-events: none;
  z-index: 9999;
  transition: background-color 0.3s;
}
.el-header {
  top: 0;
  z-index: 2000;
  background-color: transparent;
}

.app-fade-enter-active,
.app-fade-leave-active,
.app-slide-left-enter-active,
.app-slide-left-leave-active,
.app-slide-right-enter-active,
.app-slide-right-leave-active {
  transition:
    opacity 0.38s ease,
    transform 0.38s ease;
}

.app-fade-enter-from,
.app-fade-leave-to {
  opacity: 0;
}

.app-slide-left-enter-from {
  opacity: 0.88;
  transform: translateX(88px);
}

.app-slide-left-leave-to {
  opacity: 0.4;
  transform: translateX(-88px);
}

.app-slide-right-enter-from {
  opacity: 0.88;
  transform: translateX(-88px);
}

.app-slide-right-leave-to {
  opacity: 0.4;
  transform: translateX(88px);
}
</style>
