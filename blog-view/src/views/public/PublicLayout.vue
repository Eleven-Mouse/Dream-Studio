<template>
  <el-container class="common-layout">
    <el-header class="layout-header">
      <Header />
    </el-header>

    <el-main class="main-scroll-container layout-main">
      <div class="layout-view">
        <site-announcement-strip v-if="showAnnouncementStrip" />
        <router-view v-slot="{ Component }">
          <transition name="app-fade" mode="out-in">
            <component :is="Component" :key="route.fullPath" />
          </transition>
        </router-view>
      </div>
      <Footer class="layout-footer" />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Header from '@/components/common/Header.vue'
import Footer from '@/components/common/Footer.vue'
import SiteAnnouncementStrip from '@/components/common/SiteAnnouncementStrip.vue'

const route = useRoute()

const ANNOUNCEMENT_ROUTE_SET = new Set(['/home', '/headlines', '/forum', '/resources'])

const showAnnouncementStrip = computed(() => ANNOUNCEMENT_ROUTE_SET.has(route.path))
</script>

<style scoped>
.common-layout {
  min-height: 100vh;
  height: 100vh;
}

.layout-header {
  height: auto;
  padding: max(8px, var(--safe-area-top)) max(var(--mobile-gutter), var(--safe-area-right)) 8px
    max(var(--mobile-gutter), var(--safe-area-left));
  background: transparent;
}

.layout-main {
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  min-height: 0;
  padding: 0;
  background: var(--app-page-bg);
  font-family: Georgia, 'Times New Roman', Times, serif;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.layout-view {
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  padding: 12px max(var(--mobile-gutter), var(--safe-area-right)) 0
    max(var(--mobile-gutter), var(--safe-area-left));
  box-sizing: border-box;
}

.layout-footer {
  margin-top: 24px;
}

@media (max-width: 720px) {
  .layout-view {
    padding-top: max(8px, var(--safe-area-top));
  }

  .layout-footer {
    margin-bottom: max(12px, var(--safe-area-bottom));
  }
}

.layout-main::-webkit-scrollbar {
  width: 0;
  height: 0;
}
</style>
