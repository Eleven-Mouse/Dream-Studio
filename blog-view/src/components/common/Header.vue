<template>
  <div class="header-wrapper" :class="{ 'is-hidden': isHeaderHidden }">
    <div class="header-container">
      <div class="logo">
        <router-link to="/home">梦工厂</router-link>
      </div>
      <div class="search-bar" @click="searchDialogVisible = true">
        <el-icon class="search-icon-btn" :size="20"><Search /></el-icon>
      </div>

      <!-- 搜索弹窗 -->
      <teleport to="body">
        <transition name="search-overlay">
          <div v-if="searchDialogVisible" class="search-overlay" @click.self="searchDialogVisible = false">
            <div class="search-modal">
              <div class="search-modal-header">
                <el-input
                  ref="searchInputRef"
                  v-model="searchInput"
                  class="search-modal-input"
                  :prefix-icon="Search"
                  clearable
                  placeholder="搜索文章..."
                  size="large"
                />
              </div>
              <div class="search-modal-body">
                <div v-if="searchLoading" class="search-result-tip">搜索中...</div>
                <div v-else-if="searchInput && !searchResults.length" class="search-result-tip">
                  未找到相关文章
                </div>
                <div v-else-if="!searchInput" class="search-result-tip">输入关键词搜索文章</div>
                <div v-else class="search-result-list">
                  <div
                    v-for="item in searchResults"
                    :key="item.id"
                    class="search-result-item"
                    @click="handleSelectArticle(item)"
                  >
                    <span class="search-result-title">{{ item.title }}</span>
                    <span v-if="item.categoryName" class="search-result-category">{{ item.categoryName }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </transition>
      </teleport>
      <el-menu
        :default-active="activeIndex"
        class="nav-menu"
        mode="horizontal"
        background-color="transparent"
        :text-color="'var(--app-text-color)'"
        :active-text-color="'var(--app-text-color)'"
        :ellipsis="false"
        :router="true"
      >
        <el-menu-item index="/home">
          <el-icon><HomeFilled /></el-icon>Home
        </el-menu-item>
        <el-menu-item index="/headlines">
          <el-icon><Reading /></el-icon>Headlines
        </el-menu-item>
        <el-sub-menu index="categories-menu">
          <template #title
            ><el-icon><Grid /></el-icon>categories</template
          >
          <el-menu-item
            v-for="category in categories"
            :key="category.id"
            :index="`/category/${category.id}`"
          >
            {{ category.name }}
          </el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/moment"
          ><el-icon><Comment /></el-icon>Moment</el-menu-item
        >

        <el-menu-item index="/forum"
          ><el-icon><ChatDotRound /></el-icon>Forum</el-menu-item
        >
        <el-menu-item index="/archive"
          ><el-icon><WalletFilled /></el-icon>Archive</el-menu-item
        >
        <el-menu-item index="/resources"
          ><el-icon><FolderOpened /></el-icon>Resources</el-menu-item
        >
        <el-menu-item index="/about"
          ><el-icon><UserFilled /></el-icon>About</el-menu-item
        >
      </el-menu>

      <div class="user-entry" @click="goToProfile">
        <el-badge v-if="canViewAdminDashboard" value="Admin" class="user-badge">
          <el-avatar shape="square" :size="36" :src="userAvatar">{{ userInitial }}</el-avatar>
        </el-badge>
        <el-avatar v-else shape="square" :size="36" :src="userAvatar">{{ userInitial }}</el-avatar>
        <div class="user-meta">
          <span class="user-name">{{ displayName }}</span>
        </div>
      </div>

      <theme-switcher class="theme-switch" />
      <div class="github-link">
        <a href="https://github.com/Eleven-Mouse/Dream-Studio" target="_blank">
          <span><h6>GitHub</h6></span> <el-icon size="15"><Promotion /></el-icon>
        </a>
      </div>
    </div>
    <!-- 页面滚动进度条 -->
    <el-progress
      :percentage="scrollProgress"
      :stroke-width="2.5"
      :show-text="false"
      class="scroll-progress"
      status="success"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onUnmounted, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchCategories } from '@/api/categories'
import { fetchAiArticleSearch } from '@/api/article.js'
import { useAuthStore } from '@/store/auth'
import { useUserStore } from '@/store/user'
import { openAdminApp } from '@/utils/adminBridge'
import { WORKSPACE_CAPABILITIES } from '@/utils/workspaceCapabilities'
import {
  Search,
  HomeFilled,
  Grid,
  UserFilled,
  WalletFilled,
  FolderOpened,
  Promotion,
  Comment,
  ChatDotRound,
  Reading,
} from '@element-plus/icons-vue'
import ThemeSwitcher from './ThemeSwitcher.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const userStore = useUserStore()
const activeIndex = ref('/home')
const searchInput = ref('')
const searchDialogVisible = ref(false)
const searchInputRef = ref(null)
const categories = ref([])
const searchLoading = ref(false)
const searchResults = ref([])
const currentCategoryId = ref(null)
const scrollProgress = ref(0)
const isHeaderHidden = ref(false)

const HEADER_HIDE_THRESHOLD = 90
const HEADER_DIRECTION_THRESHOLD = 6

const currentProfile = computed(() => userStore.profile)
const displayName = computed(() => currentProfile.value.nickname)
const userAvatar = computed(() => currentProfile.value.avatar)
const userInitial = computed(() => displayName.value?.slice(0, 1)?.toUpperCase() || 'D')
const canViewAdminDashboard = computed(() =>
  userStore.hasCapability(WORKSPACE_CAPABILITIES.DASHBOARD_VIEW),
)

// 获取分类列表
const loadCategories = async () => {
  try {
    categories.value = await fetchCategories()
  } catch (error) {
    console.error('获取分类列表失败', error)
  }
}

// 监听路由变化，更新导航菜单激活状态
watch(
  () => route.path,
  (newPath) => {
    if (newPath === '/home' || newPath === '/') {
      activeIndex.value = '/home'
    } else if (newPath.startsWith('/headlines')) {
      activeIndex.value = '/headlines'
    } else if (newPath.startsWith('/forum')) {
      activeIndex.value = '/forum'
    } else if (newPath.startsWith('/category/')) {
      activeIndex.value = 'categories-menu'
      currentCategoryId.value = route.params.id
    } else {
      activeIndex.value = newPath
    }
  },
  { immediate: true },
)

// 监听路由变化，获取categoryId
watch(
  () => route.params.id,
  (newId) => {
    if (route.path.startsWith('/category/')) {
      currentCategoryId.value = newId
    }
  },
  { immediate: true },
)

let scrollContainer = null

const showHeader = () => {
  isHeaderHidden.value = false
}

const updateHeaderVisibility = (scrollTop) => {
  const previousScrollTop = scrollContainer?.dataset.lastScrollTop
    ? Number(scrollContainer.dataset.lastScrollTop)
    : 0
  const delta = scrollTop - previousScrollTop

  if (scrollTop <= HEADER_HIDE_THRESHOLD) {
    showHeader()
  } else if (delta > HEADER_DIRECTION_THRESHOLD) {
    isHeaderHidden.value = true
  } else if (delta < -HEADER_DIRECTION_THRESHOLD) {
    showHeader()
  }

  if (scrollContainer) {
    scrollContainer.dataset.lastScrollTop = String(scrollTop)
  }
}

const handleMainScroll = (e) => {
  // 注意：这里我们通过 e.target 获取滚动的元素
  const target = e.target
  if (!target) return

  const scrollTop = target.scrollTop
  const scrollHeight = target.scrollHeight
  const clientHeight = target.clientHeight

  const scrollableHeight = scrollHeight - clientHeight
  const progress = scrollableHeight > 0 ? (scrollTop / scrollableHeight) * 100 : 0

  scrollProgress.value = Math.min(Math.max(progress, 0), 100)
  updateHeaderVisibility(scrollTop)
}

onMounted(() => {
  loadCategories()
  // 监听页面滚动
  // 注意：querySelector 前面的点 . 代表 class
  scrollContainer = document.querySelector('.main-scroll-container')

  // 2. 如果找到了，就手动添加原生事件监听
  if (scrollContainer) {
    scrollContainer.dataset.lastScrollTop = String(scrollContainer.scrollTop)
    scrollContainer.addEventListener('scroll', handleMainScroll)
  } else {
    console.warn('未找到滚动容器 .main-scroll-container')
  }
})

onUnmounted(() => {
  // 3. 组件销毁时，记得移除监听，防止内存泄漏
  if (scrollContainer) {
    scrollContainer.removeEventListener('scroll', handleMainScroll)
  }
})

watch(
  () => route.fullPath,
  () => {
    showHeader()
    if (scrollContainer) {
      scrollContainer.dataset.lastScrollTop = String(scrollContainer.scrollTop)
    }
  },
)

// 监听搜索关键字变化，实时查询文章
let searchTimer = null
watch(searchInput, (val) => {
  clearTimeout(searchTimer)
  const keyword = val.trim()
  if (!keyword) {
    searchResults.value = []
    return
  }
  // Header 搜索单独切到 AI 搜索接口，不影响原有文章列表接口行为。
  searchTimer = setTimeout(async () => {
    searchLoading.value = true
    try {
      const res = await fetchAiArticleSearch({ q: keyword, page: 1, size: 8 })
      searchResults.value = res?.data || []
    } catch (e) {
      console.error('搜索文章失败', e)
      searchResults.value = []
    } finally {
      searchLoading.value = false
    }
  }, 300)
})

// 选择某个搜索建议后，跳转到文章详情，并清空搜索框
const handleSelectArticle = (item) => {
  if (!item || !item.id) return
  router.push(`/article/${item.id}`)
  // 跳转后清空输入内容
  searchInput.value = ''
  searchDialogVisible.value = false
}

// 弹窗打开时自动聚焦搜索输入框
watch(searchDialogVisible, (visible) => {
  if (visible) {
    nextTick(() => {
      searchInputRef.value?.focus()
    })
  } else {
    searchInput.value = ''
    searchResults.value = []
  }
})

// Escape 键关闭搜索弹窗
const handleSearchKeydown = (e) => {
  if (e.key === 'Escape' && searchDialogVisible.value) {
    searchDialogVisible.value = false
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleSearchKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleSearchKeydown)
})

const goToProfile = () => {
  if (canViewAdminDashboard.value) {
    openAdminApp({
      isAdmin: true,
      accessToken: authStore.accessToken,
      router,
      targetPath: '/admin/overview',
    })
    return
  }

  router.push('/profile/overview')
}

const handleLogout = async () => {
  authStore.logout()
}
</script>

<style scoped>
.header-container {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 58px;
  padding: 0 22px;
  border: 1px solid var(--header-glass-border, rgba(255, 255, 255, 0.66));
  border-radius: 22px;
  background: var(--header-glass-bg, rgba(255, 255, 255, 0.56));
  box-shadow: var(--header-glass-shadow);
  backdrop-filter: blur(18px) saturate(160%);
  -webkit-backdrop-filter: blur(18px) saturate(160%);
}
.logo a {
  font-size: 22px;
  font-weight: bold;
  color: var(--app-text-color);
  text-decoration: none;
}

.search-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  margin: 0 8px;
  border-radius: 50%;
  cursor: pointer;
  transition: background-color 0.25s ease;
}

.search-bar:hover {
  background: rgba(255, 255, 255, 0.34);
}

.search-icon-btn {
  color: var(--app-text-color);
}

/* 搜索弹窗 - 遮罩层 */
.search-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 18vh;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}

/* 搜索弹窗 - 弹出框 */
.search-modal {
  display: flex;
  flex-direction: column;
  width: min(560px, 90vw);
  max-height: 60vh;
  border-radius: 16px;
  background: var(--el-bg-color, #fff);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3), 0 0 0 1px rgba(255, 255, 255, 0.1);
  overflow: hidden;
}

.search-modal-header {
  padding: 20px 24px 12px;
}

.search-modal-input {
  width: 100%;
}

.search-modal-input ::v-deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--el-border-color, #dcdfe6) inset !important;
  border-radius: 10px;
}

.search-modal-input ::v-deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--app-text-color) inset !important;
}

.search-modal-body {
  flex: 1;
  min-height: 180px;
  padding: 0 24px 20px;
  overflow-y: auto;
}

.search-result-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 120px;
  color: var(--el-text-color-placeholder, #a8abb2);
  font-size: 0.92rem;
}

.search-result-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  margin-bottom: 6px;
  border-radius: 10px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.search-result-item:hover {
  background: var(--el-fill-color-light, #f5f7fa);
}

.search-result-title {
  flex: 1;
  font-size: 0.95rem;
  color: var(--el-text-color-primary, #303133);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.search-result-category {
  flex-shrink: 0;
  margin-left: 12px;
  padding: 2px 10px;
  border-radius: 999px;
  background: var(--el-fill-color, #f0f2f5);
  color: var(--el-text-color-secondary, #909399);
  font-size: 0.78rem;
}

/* 弹窗过渡动画 */
.search-overlay-enter-active {
  transition: opacity 0.25s ease;
}

.search-overlay-enter-active .search-modal {
  transition: transform 0.25s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.25s ease;
}

.search-overlay-leave-active {
  transition: opacity 0.2s ease;
}

.search-overlay-leave-active .search-modal {
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.search-overlay-enter-from {
  opacity: 0;
}

.search-overlay-enter-from .search-modal {
  transform: translateY(-20px) scale(0.97);
  opacity: 0;
}

.search-overlay-leave-to {
  opacity: 0;
}

.search-overlay-leave-to .search-modal {
  transform: translateY(-10px) scale(0.98);
  opacity: 0;
}

.nav-menu {
  border-bottom: none;
}

.nav-menu .el-menu-item:hover,
.nav-menu .el-sub-menu__title:hover {
  background-color: rgba(255, 255, 255, 0.34) !important;
}
/* 带有子菜单的标题悬停 */
:deep(.el-sub-menu__title:hover) {
  background-color: rgba(255, 255, 255, 0.34) !important;
}

:deep(.el-menu--horizontal > .el-menu-item.is-active),
:deep(.el-menu--horizontal > .el-sub-menu.is-active .el-sub-menu__title) {
  background: rgba(255, 255, 255, 0.3) !important;
  border-bottom-color: rgba(109, 160, 81, 0.45) !important;
}
.github-link {
  margin-left: auto;
}

.github-link a {
  color: var(--app-text-color);
  text-decoration: none;
  display: flex;
  align-items: center;
}

.github-link a:hover {
  color: var(--app-text-color);
}

.github-link span {
  margin-left: 8px;
}

.theme-switch {
  margin-left: 16px;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: 18px;
  padding: 6px 10px;
  border-radius: 999px;
  cursor: pointer;
  transition: background-color 0.25s ease;
}

.user-entry:hover {
  background: rgba(255, 255, 255, 0.34);
}

.user-meta {
  display: flex;
  min-width: 0;
  flex-direction: column;
  line-height: 0.8;
}

.user-name {
  display: block;
  color: var(--app-text-color);
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-meta small {
  color: #8d8d8d;
}

.user-badge :deep(.el-badge__content) {
  transform: translateY(-30%) translateX(30%);
}

@media screen and (max-width: 1200px) {
  .user-meta {
    display: none;
  }

  .logout-button {
    padding: 0 6px;
  }
}

@media screen and (max-width: 900px) {
  .header-container {
    flex-wrap: wrap;
    gap: 10px;
    padding: 12px 14px;
    border-radius: 18px;
  }

  .logo {
    order: 1;
  }

  .logo a {
    font-size: 18px;
  }

  .search-bar {
    order: 2;
    margin: 0;
  }

  .user-entry {
    order: 3;
    margin-left: auto;
    padding: 4px 8px;
  }

  .theme-switch {
    order: 4;
    margin-left: 0;
  }

  .github-link {
    order: 5;
    margin-left: 0;
  }

  .nav-menu {
    order: 6;
    width: 100%;
    overflow-x: auto;
    overflow-y: hidden;
    white-space: nowrap;
    scrollbar-width: none;
  }

  .nav-menu::-webkit-scrollbar {
    display: none;
  }

  :deep(.nav-menu .el-menu--horizontal) {
    display: inline-flex;
    min-width: max-content;
  }

  :deep(.nav-menu .el-menu-item),
  :deep(.nav-menu .el-sub-menu__title) {
    padding: 0 12px;
    height: 40px;
    line-height: 40px;
  }
}

@media screen and (max-width: 700px) {
  .header-wrapper {
    padding: 0 4px 0;
  }

  .header-container {
    padding: 10px 12px;
    gap: 8px;
  }

  .search-bar {
    width: 34px;
    height: 34px;
  }

  .user-entry {
    padding: 0;
    background: transparent;
  }

  .github-link {
    display: none;
  }

  .search-overlay {
    padding: 10vh 12px 0;
    align-items: flex-start;
  }

  .search-modal {
    width: 100%;
    max-height: 72vh;
    border-radius: 14px;
  }

  .search-modal-header {
    padding: 16px 16px 10px;
  }

  .search-modal-body {
    min-height: 160px;
    padding: 0 16px 16px;
  }

  .search-result-item {
    align-items: flex-start;
    gap: 8px;
    padding: 12px;
  }

  .search-result-category {
    margin-left: 0;
  }
}

.header-wrapper {
  position: relative;
  width: 100%;
  max-width: 100vw;
  margin: 0 auto;

  box-sizing: border-box;
  transform: translateY(0);
  opacity: 1;
  filter: blur(0);
  transition:
    transform 0.56s cubic-bezier(0.22, 1, 0.36, 1),
    opacity 0.4s ease,
    filter 0.42s ease;
  will-change: transform, opacity, filter;
}

.header-wrapper.is-hidden {
  transform: translateY(calc(-100% - 18px));
  opacity: 0;
  filter: blur(8px);
  pointer-events: none;
}

.scroll-progress {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  border-radius: 0;
}
</style>
