<template>
  <div class="workspace-shell" :class="`mode-${mode}`">
    <div class="workspace-glow workspace-glow-secondary"></div>

    <aside class="workspace-sidebar">
      <button type="button" class="workspace-brand" @click="router.push('/home')">
        <div class="workspace-brand-avatar">
          <el-avatar :size="28" :src="profile.avatar">{{ userInitial }}</el-avatar>
        </div>
        <div class="workspace-brand-meta">
          <h4>{{ profile.nickname || workspaceConfig.defaultTitle }}</h4>
        </div>
      </button>

      <el-scrollbar class="workspace-menu-scroll">
        <div class="workspace-menu-stack">
          <section
            v-for="section in groupedMenuSections"
            :key="section.key"
            class="workspace-menu-group"
          >
            <div class="workspace-menu-list">
              <button
                v-for="item in section.items"
                :key="item.index"
                type="button"
                class="workspace-nav-button"
                :class="{ 'is-active': isMenuItemActive(item) }"
                @click="router.push(item.index)"
              >
                <div class="menu-item-main">
                  <span class="menu-icon-wrap">
                    <el-icon><component :is="item.icon" /></el-icon>
                  </span>

                  <h4>{{ item.label }}</h4>
                </div>

                <span v-if="item.badge" class="menu-item-badge">{{ item.badge }}</span>
              </button>
            </div>
          </section>
        </div>
      </el-scrollbar>
    </aside>

    <div class="workspace-main">
      <header class="workspace-header">
        <div class="workspace-header-main">
          <el-breadcrumb separator=">">
            <el-breadcrumb-item @click="router.push(primaryEntryPath)">
              {{ workspaceConfig.breadcrumbLabel }}
            </el-breadcrumb-item>
          </el-breadcrumb>

          <div class="page-kicker-row">
            <div class="page-kicker">{{ workspaceConfig.pageKicker }}</div>
            <span class="page-index">Section {{ currentPageSequence }}</span>
          </div>

          <h1>{{ currentPageTitle }}</h1>

          <div class="page-tags">
            <span class="page-chip is-accent">{{ workspaceConfig.tagText }}</span>
            <span class="page-chip">{{ currentDateLabel }}</span>
            <span class="page-chip">{{ profileRoleLabel }}</span>
          </div>
        </div>
      </header>

      <main class="workspace-content">
        <router-view v-slot="{ Component }">
          <transition name="app-fade" mode="out-in">
            <component :is="Component" :key="route.fullPath" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchAdminDashboardStatistics } from '@/api/admin'
import { fetchAdminAnnouncements } from '@/api/admin/site'
import { fetchCurrentUserOverview } from '@/api/user'
import { useUserStore } from '@/store/user'
import { WORKSPACE_CAPABILITIES } from '@/utils/workspaceCapabilities'
import {
  Bell,
  ChatSquare,
  Comment,
  Document,
  Discount,
  EditPen,
  HomeFilled,
  House,
  Odometer,
  Setting,
  UserFilled,
  WalletFilled,
} from '@element-plus/icons-vue'

const props = defineProps({
  mode: {
    type: String,
    required: true,
    validator: (value) => ['user', 'admin'].includes(value),
  },
})

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const sidebarInsights = reactive({
  loading: false,
  unreadNotificationCount: 0,
  pendingReportCount: 0,
  postCount: 0,
  commentCount: 0,
  dashboardTotalContent: 0,
  announcementCount: 0,
})

const WORKSPACE_CONFIGS = {
  user: {
    brandKicker: 'Dream Studio',
    breadcrumbLabel: '个人中心',
    defaultTitle: '个人总览',
    brandDescription: '把创作进度、互动反馈与账号资料收束成一个更舒展的个人空间。',
    pageKicker: 'Personal Workspace',
    tagText: 'Personal Hub',
    panelKicker: 'Creative Flow',
    panelDescription: '优先处理通知、保持内容更新，并让你的个人资料呈现得更完整。',
    homeDescription: '把个人创作、通知和互动反馈集中到一个独立空间里维护。',
  },
  admin: {
    brandKicker: 'Dream Studio',
    breadcrumbLabel: '后台管理',
    defaultTitle: '管理概览',
    brandDescription: '用更稳定的节奏处理内容、社区与站点配置，让后台信息层次更明确。',
    pageKicker: 'Admin Console',
    tagText: 'Admin Console',
    panelKicker: 'Operation Focus',
    panelDescription: '优先查看待处理事项和高频入口，让审核、配置与内容维护保持顺畅。',
    homeDescription: '查看并维护整站内容、互动数据和站点配置。',
  },
}

const WORKSPACE_MENU_SECTIONS = {
  user: [
    {
      key: 'overview',
      label: '个人空间',
      description: '先看总览、通知和账号资料。',
    },
    {
      key: 'content',
      label: '创作管理',
      description: '维护文章、动态和新的发布动作。',
    },
    {
      key: 'community',
      label: '社区互动',
      description: '处理论坛发帖与个人讨论沉淀。',
    },
  ],
  admin: [
    {
      key: 'control',
      label: '后台总控',
      description: '概览审核队列、评论和站点设置。',
    },
    {
      key: 'content',
      label: '内容维护',
      description: '统一整理文章、动态和分类标签。',
    },
    {
      key: 'community',
      label: '社区运维',
      description: '处理帖子发布、社区秩序和用户账号。',
    },
  ],
}

const WORKSPACE_MENUS = {
  user: [
    {
      path: 'overview',
      label: '总览',
      title: '个人总览',
      description: '查看个人资料、通知提醒、创作数据和最近互动。',
      icon: Odometer,
      section: 'overview',
      requiredCapability: WORKSPACE_CAPABILITIES.PROFILE_OVERVIEW_VIEW,
    },
    {
      path: 'notifications',
      label: '通知中心',
      title: '通知中心',
      description: '查看站内通知，并按需标记为已读。',
      icon: Bell,
      section: 'overview',
      requiredCapability: WORKSPACE_CAPABILITIES.PROFILE_NOTIFICATIONS_VIEW,
    },
    {
      path: 'account',
      label: '账号设置',
      title: '账号设置',
      description: '更新头像、设置站内密码并确认当前账号资料。',
      icon: Setting,
      section: 'overview',
      requiredCapability: WORKSPACE_CAPABILITIES.PROFILE_ACCOUNT_EDIT,
    },
    {
      path: 'articlemgmt',
      label: '我的文章',
      title: '我的文章',
      description: '查看、筛选和维护你发布过的文章。',
      icon: HomeFilled,
      section: 'content',
      requiredCapability: WORKSPACE_CAPABILITIES.ARTICLE_MANAGE_OWN,
    },
    {
      path: 'momentsmgmt',
      label: '我的动态',
      title: '我的动态',
      description: '集中查看和删除自己发布的动态内容。',
      icon: House,
      section: 'content',
      requiredCapability: WORKSPACE_CAPABILITIES.MOMENT_MANAGE_OWN,
    },
    {
      path: 'writearticle',
      label: '写文章',
      title: '写文章',
      description: '继续写作、保存草稿或直接发布文章。',
      icon: WalletFilled,
      section: 'content',
      requiredCapability: WORKSPACE_CAPABILITIES.ARTICLE_WRITE,
    },
    {
      path: 'writemoment',
      label: '发动态',
      title: '发布动态',
      description: '记录近况并把图文动态发布到时间轴。',
      icon: ChatSquare,
      section: 'content',
      requiredCapability: WORKSPACE_CAPABILITIES.MOMENT_WRITE,
    },
    {
      path: 'forum-publish',
      label: '发布帖子',
      title: '发布帖子',
      description: '开始新的讨论，把内容同步到论坛页面。',
      icon: EditPen,
      section: 'community',
      requiredCapability: WORKSPACE_CAPABILITIES.FORUM_POST_WRITE,
    },
    {
      path: 'forum-manage',
      label: '我的帖子',
      title: '我的帖子',
      description: '查看、整理和删除自己发布的论坛帖子。',
      icon: Document,
      section: 'community',
      requiredCapability: WORKSPACE_CAPABILITIES.FORUM_POST_MANAGE_OWN,
    },
  ],
  admin: [
    {
      path: 'overview',
      label: '总览',
      title: '管理概览',
      description: '总览整站内容状态、待处理事项和运营趋势。',
      icon: Odometer,
      section: 'control',
      requiredCapability: WORKSPACE_CAPABILITIES.DASHBOARD_VIEW,
    },
    {
      path: 'articlemgmt',
      label: '文章管理',
      title: '文章管理',
      description: '统一维护站点文章内容、状态和发布节奏。',
      icon: HomeFilled,
      section: 'content',
      requiredCapability: WORKSPACE_CAPABILITIES.ARTICLE_MANAGE_ALL,
    },
    {
      path: 'commentmgmt',
      label: '评论管理',
      title: '评论管理',
      description: '审核评论内容并快速定位到对应页面。',
      icon: Comment,
      section: 'control',
      requiredCapability: WORKSPACE_CAPABILITIES.COMMENT_MODERATE,
    },
    {
      path: 'reportmgmt',
      label: '举报审核',
      title: '举报审核',
      description: '处理论坛举报，并记录审核结论与备注。',
      icon: Bell,
      section: 'control',
      requiredCapability: WORKSPACE_CAPABILITIES.REPORT_REVIEW,
    },
    {
      path: 'momentsmgmt',
      label: '动态管理',
      title: '动态管理',
      description: '查看、清理和维护站点动态内容。',
      icon: House,
      section: 'content',
      requiredCapability: WORKSPACE_CAPABILITIES.MOMENT_MANAGE_ALL,
    },
    {
      path: 'writearticle',
      label: '写文章',
      title: '写文章',
      description: '新建文章、保存草稿或继续编辑既有内容。',
      icon: WalletFilled,
      section: 'content',
      requiredCapability: WORKSPACE_CAPABILITIES.ARTICLE_WRITE,
    },
    {
      path: 'writemoment',
      label: '发布动态',
      title: '发布动态',
      description: '以管理员身份快速发布新的站点动态。',
      icon: ChatSquare,
      section: 'content',
      requiredCapability: WORKSPACE_CAPABILITIES.MOMENT_WRITE,
    },
    {
      path: 'categorisemgmt',
      label: '分类管理',
      title: '分类管理',
      description: '集中维护文章分类并保持内容结构清晰。',
      icon: UserFilled,
      section: 'content',
      requiredCapability: WORKSPACE_CAPABILITIES.TAXONOMY_MANAGE,
    },
    {
      path: 'tagsmgmt',
      label: '标签管理',
      title: '标签管理',
      description: '统一整理文章标签和站点内容索引。',
      icon: Discount,
      section: 'content',
      requiredCapability: WORKSPACE_CAPABILITIES.TAXONOMY_MANAGE,
    },
    {
      path: 'forum-publish',
      label: '发布帖子',
      title: '发布帖子',
      description: '从后台直接发起论坛讨论并发布内容。',
      icon: EditPen,
      section: 'community',
      requiredCapability: WORKSPACE_CAPABILITIES.FORUM_POST_WRITE,
    },
    {
      path: 'forum-manage',
      label: '帖子管理',
      title: '帖子管理',
      description: '管理论坛帖子状态、置顶和加精展示。',
      icon: Document,
      section: 'community',
      requiredCapability: WORKSPACE_CAPABILITIES.FORUM_POST_MODERATE,
    },
    {
      path: 'site',
      label: '站点设置',
      title: '站点设置',
      description: '维护站点信息、公告和论坛页展示内容。',
      icon: Setting,
      section: 'control',
      requiredCapability: WORKSPACE_CAPABILITIES.SITE_MANAGE,
    },
    {
      path: 'usermgmt',
      label: '用户管理',
      title: '用户管理',
      description: '查看账号状态，并设置管理员或删除用户。',
      icon: UserFilled,
      section: 'community',
      requiredCapability: WORKSPACE_CAPABILITIES.USER_MANAGE,
    },
  ],
}

const formatCompactCount = (value) => {
  const numericValue = Number(value || 0)

  if (numericValue > 99) return '99+'
  return `${numericValue}`
}

const summarizeMenuDescription = (description = '') => {
  if (description.length <= 18) return description
  return `${description.slice(0, 18)}...`
}

const profile = computed(() => userStore.profile || {})
const basePath = computed(() => (props.mode === 'admin' ? '/admin' : '/profile'))
const workspaceConfig = computed(() => WORKSPACE_CONFIGS[props.mode])
const profileCompletion = computed(() => {
  const checks = [
    Boolean(profile.value.avatar),
    Boolean(profile.value.bio),
    Boolean(profile.value.email),
    Boolean(profile.value.phone),
    Boolean(profile.value.passwordInitialized),
  ]
  const completed = checks.filter(Boolean).length
  const total = checks.length

  return {
    completed,
    total,
    rate: Math.round((completed / total) * 100),
  }
})

const resolveMenuBadge = (item) => {
  if (
    props.mode === 'user' &&
    item.path === 'notifications' &&
    sidebarInsights.unreadNotificationCount > 0
  ) {
    return formatCompactCount(sidebarInsights.unreadNotificationCount)
  }

  if (
    props.mode === 'admin' &&
    item.path === 'reportmgmt' &&
    sidebarInsights.pendingReportCount > 0
  ) {
    return formatCompactCount(sidebarInsights.pendingReportCount)
  }

  if (props.mode === 'admin' && item.path === 'site' && sidebarInsights.announcementCount > 0) {
    return formatCompactCount(sidebarInsights.announcementCount)
  }

  return ''
}

const menuItems = computed(() =>
  WORKSPACE_MENUS[props.mode]
    .map((item) => ({
      ...item,
      index: `${basePath.value}/${item.path}`,
      summary: summarizeMenuDescription(item.description),
      badge: resolveMenuBadge(item),
    }))
    .filter((item) => userStore.hasCapability(item.requiredCapability)),
)

const groupedMenuSections = computed(() =>
  WORKSPACE_MENU_SECTIONS[props.mode]
    .map((section) => ({
      ...section,
      items: menuItems.value.filter((item) => item.section === section.key),
    }))
    .filter((section) => section.items.length),
)

const userInitial = computed(() => profile.value.nickname?.slice(0, 1)?.toUpperCase() || 'D')
const profileRoleLabel = computed(() =>
  profile.value.role === 'ADMIN' ? '管理员身份' : '创作者空间',
)
const currentDateLabel = computed(() =>
  new Intl.DateTimeFormat('zh-CN', {
    month: 'long',
    day: 'numeric',
    weekday: 'long',
  }).format(new Date()),
)
const primaryEntryPath = computed(() => `${basePath.value}/overview`)
const workspaceShortcut = computed(() => {
  if (props.mode === 'admin') {
    return {
      label: '进入站点设置',
      path: '/admin/site',
    }
  }

  return {
    label: '查看通知中心',
    path: '/profile/notifications',
  }
})

const findCurrentMenuItem = () => {
  const currentPath = route.path
  return menuItems.value.find(
    (item) => currentPath === item.index || currentPath.startsWith(`${item.index}/`),
  )
}

const activeMenuItem = computed(() => findCurrentMenuItem())
const currentPageSequence = computed(() => {
  const total = Math.max(menuItems.value.length, 1)
  const currentIndex =
    Math.max(
      menuItems.value.findIndex((item) => item.index === activeMenuItem.value?.index),
      0,
    ) + 1

  return `${String(currentIndex).padStart(2, '0')} / ${String(total).padStart(2, '0')}`
})

const workspaceWelcomeTitle = computed(() => {
  const displayName =
    profile.value.nickname || profile.value.username || workspaceConfig.value.defaultTitle
  return props.mode === 'admin'
    ? `${displayName}，继续保持后台节奏`
    : `${displayName}，让创作空间更从容`
})

const workspaceStatusLabel = computed(() => {
  if (sidebarInsights.loading) {
    return props.mode === 'admin' ? '正在同步后台状态' : '正在同步个人状态'
  }

  if (props.mode === 'admin') {
    return sidebarInsights.pendingReportCount
      ? `待处理 ${sidebarInsights.pendingReportCount} 项审核`
      : '后台运行平稳'
  }

  if (sidebarInsights.unreadNotificationCount > 0) {
    return `待查看 ${sidebarInsights.unreadNotificationCount} 条通知`
  }

  if (profileCompletion.value.rate < 100) {
    return `资料完成 ${profileCompletion.value.rate}%`
  }

  return '个人空间已整理完成'
})

const workspaceSidebarStats = computed(() => [
  {
    label: '可用模块',
    value: `${menuItems.value.length} 项`,
  },
  {
    label: '当前章节',
    value: currentPageSequence.value,
  },
  {
    label: props.mode === 'admin' ? '待审举报' : '未读通知',
    value: `${props.mode === 'admin' ? sidebarInsights.pendingReportCount : sidebarInsights.unreadNotificationCount} 条`,
  },
])

const workspaceQuickLinks = computed(() => {
  if (props.mode === 'admin') {
    return [
      {
        label: '举报审核',
        summary: '优先处理社区反馈与待审事项。',
        meta: `${sidebarInsights.pendingReportCount} 条待处理`,
        path: '/admin/reportmgmt',
      },
      {
        label: '站点设置',
        summary: '维护公告与论坛展示信息。',
        meta: `${sidebarInsights.announcementCount} 条公告`,
        path: '/admin/site',
      },
      {
        label: '用户管理',
        summary: '查看账号状态与管理权限。',
        meta: profileRoleLabel.value,
        path: '/admin/usermgmt',
      },
    ]
  }

  return [
    {
      label: '通知中心',
      summary: '优先处理新的站内提醒与反馈。',
      meta: `${sidebarInsights.unreadNotificationCount} 条未读`,
      path: '/profile/notifications',
    },
    {
      label: '账号设置',
      summary: '继续补全头像、密码与资料信息。',
      meta: `${profileCompletion.value.rate}% 完成`,
      path: '/profile/account',
    },
    {
      label: '写文章',
      summary: '回到创作节奏，继续新的内容。',
      meta: `${sidebarInsights.postCount} 篇沉淀`,
      path: '/profile/writearticle',
    },
  ]
})

const workspaceFocus = computed(() => {
  if (props.mode === 'admin') {
    if (sidebarInsights.pendingReportCount > 0) {
      return {
        kicker: 'Operation Focus',
        title: '先处理举报审核队列',
        description: `当前还有 ${sidebarInsights.pendingReportCount} 条举报等待处理，优先清理可让后台节奏更稳定。`,
        actionLabel: '打开举报审核',
        path: '/admin/reportmgmt',
      }
    }

    if (sidebarInsights.announcementCount === 0) {
      return {
        kicker: 'Operation Focus',
        title: '补充一条最新站内公告',
        description: '论坛和站点的公告区暂时为空，可以补充新的活动提醒或规则说明。',
        actionLabel: '前往站点设置',
        path: '/admin/site',
      }
    }

    return {
      kicker: 'Operation Focus',
      title: '回到管理概览查看全站走势',
      description: '当前队列压力不大，可以继续关注内容分布、帖子活跃度和站点摘要。',
      actionLabel: '查看管理概览',
      path: '/admin/overview',
    }
  }

  if (sidebarInsights.unreadNotificationCount > 0) {
    return {
      kicker: 'Creative Flow',
      title: '先清空通知中心',
      description: `还有 ${sidebarInsights.unreadNotificationCount} 条站内提醒待查看，处理后能更顺畅回到创作状态。`,
      actionLabel: '查看通知中心',
      path: '/profile/notifications',
    }
  }

  if (profileCompletion.value.rate < 100) {
    return {
      kicker: 'Creative Flow',
      title: '继续补全个人资料',
      description: `当前资料完整度为 ${profileCompletion.value.rate}%，完善头像、简介和联系方式会让个人空间更完整。`,
      actionLabel: '前往账号设置',
      path: '/profile/account',
    }
  }

  return {
    kicker: 'Creative Flow',
    title: '继续发布新的内容',
    description: '个人空间已经整理得比较完整了，现在更适合继续写文章、发动态或发布帖子。',
    actionLabel: '开始写文章',
    path: '/profile/writearticle',
  }
})

const menuActivePath = computed(() => {
  if (route.path.startsWith(`${basePath.value}/article/edit`)) {
    return `${basePath.value}/writearticle`
  }

  if (route.path.startsWith(`${basePath.value}/content`)) {
    return `${basePath.value}/articlemgmt`
  }

  if (route.path.startsWith(`${basePath.value}/forum-entry`)) {
    return `${basePath.value}/forum-publish`
  }

  return route.path
})

const isMenuItemActive = (item) => menuActivePath.value === item.index

const currentPageTitle = computed(() => {
  if (route.path.startsWith(`${basePath.value}/article/edit`)) {
    return '编辑文章'
  }

  return activeMenuItem.value?.title || workspaceConfig.value.defaultTitle
})

const currentPageDescription = computed(() => {
  if (route.path.startsWith(`${basePath.value}/article/edit`)) {
    return props.mode === 'admin'
      ? '继续调整文章内容和发布设置，更新后会回到文章管理页。'
      : '继续打磨文章内容，更新后会返回个人文章列表。'
  }

  return activeMenuItem.value?.description || workspaceConfig.value.homeDescription
})

const syncSidebarInsights = async () => {
  sidebarInsights.loading = true

  try {
    if (props.mode === 'admin') {
      const [userOverviewResponse, dashboardResponse, announcementResponse] = await Promise.all([
        fetchCurrentUserOverview(),
        fetchAdminDashboardStatistics(),
        fetchAdminAnnouncements(),
      ])

      if (userOverviewResponse?.profile) {
        userStore.hydrateFromServerProfile(userOverviewResponse.profile)
      }

      sidebarInsights.unreadNotificationCount = Number(
        userOverviewResponse?.unreadNotificationCount || 0,
      )
      sidebarInsights.pendingReportCount = Number(userOverviewResponse?.pendingReportCount || 0)
      sidebarInsights.postCount = Number(userOverviewResponse?.postCount || 0)
      sidebarInsights.commentCount = Number(userOverviewResponse?.commentCount || 0)
      sidebarInsights.announcementCount = Array.isArray(announcementResponse)
        ? announcementResponse.length
        : 0
      sidebarInsights.dashboardTotalContent = [
        Number(dashboardResponse?.articleCount || 0),
        Number(dashboardResponse?.momentCount || 0),
        Number(dashboardResponse?.forumPostCount || 0),
      ].reduce((total, value) => total + value, 0)
      return
    }

    const userOverviewResponse = await fetchCurrentUserOverview()

    if (userOverviewResponse?.profile) {
      userStore.hydrateFromServerProfile(userOverviewResponse.profile)
    }

    sidebarInsights.unreadNotificationCount = Number(
      userOverviewResponse?.unreadNotificationCount || 0,
    )
    sidebarInsights.pendingReportCount = Number(userOverviewResponse?.pendingReportCount || 0)
    sidebarInsights.postCount = Number(userOverviewResponse?.postCount || 0)
    sidebarInsights.commentCount = Number(userOverviewResponse?.commentCount || 0)
    sidebarInsights.dashboardTotalContent = 0
    sidebarInsights.announcementCount = 0
  } catch (error) {
    console.error('同步工作区侧栏信息失败', error)
  } finally {
    sidebarInsights.loading = false
  }
}

watch(
  [() => props.mode, () => route.path],
  ([, currentPath]) => {
    if (currentPath.startsWith(basePath.value)) {
      syncSidebarInsights()
    }
  },
  { immediate: true },
)
</script>

<style scoped>
.workspace-shell {
  --workspace-font-sans: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  --workspace-font-serif: 'Noto Serif SC', 'Source Han Serif SC', Georgia, serif;
  position: relative;
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 24px;
  min-height: 100vh;
  overflow: hidden;
  font-family: var(--workspace-font-sans);
}

.workspace-shell.mode-user {
  --workspace-accent: #c96b2c;
  --workspace-accent-deep: #8f3e15;
  --workspace-accent-soft: rgba(201, 107, 44, 0.14);
  --workspace-title: #6b2d12;
  --workspace-surface: rgba(255, 250, 244, 0.9);
  --workspace-sidebar-bg: rgba(255, 247, 240, 0.82);
  background:
    radial-gradient(circle at top left, rgba(254, 215, 170, 0.82), transparent 34%),
    radial-gradient(circle at bottom right, rgba(251, 191, 36, 0.1), transparent 28%),
    linear-gradient(180deg, #fff7ef 0%, #fffdfb 42%, #ffffff 100%);
}

.workspace-shell.mode-admin {
  --workspace-accent: #0b766f;
  --workspace-accent-deep: #134e4a;
  --workspace-accent-soft: rgba(11, 118, 111, 0.14);
  --workspace-title: #0f3f3c;
  --workspace-surface: rgba(244, 252, 251, 0.9);
  --workspace-sidebar-bg: rgba(239, 250, 248, 0.82);
  background:
    radial-gradient(circle at top left, rgba(153, 246, 228, 0.68), transparent 32%),
    radial-gradient(circle at bottom right, rgba(45, 212, 191, 0.1), transparent 28%),
    linear-gradient(180deg, #f1fcfa 0%, #f8fbfc 42%, #ffffff 100%);
}

.workspace-glow {
  position: absolute;

  filter: blur(22px);
  pointer-events: none;
  opacity: 0.55;
}

.workspace-glow-primary {
  top: -120px;
  left: -40px;
  width: 280px;
  height: 280px;
  background: var(--workspace-accent-soft);
}

.workspace-glow-secondary {
  right: -120px;
  bottom: -100px;
  width: 340px;
  height: 340px;
  background: rgba(148, 163, 184, 0.14);
}

.workspace-sidebar,
.workspace-header,
.workspace-content {
  position: relative;
  z-index: 1;
}

.workspace-sidebar {
  position: sticky;
  top: 22px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  height: calc(100vh - 44px);
  padding: 18px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 34px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.8), rgba(255, 255, 255, 0.58)),
    var(--workspace-sidebar-bg);
  backdrop-filter: blur(18px);
  box-shadow:
    0 26px 54px rgba(15, 23, 42, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  box-sizing: border-box;
}

.workspace-brand {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  width: 100%;
  padding: 18px;
  border: 0;
  border-radius: 20px;
  background:
    radial-gradient(circle at top right, var(--workspace-accent-soft), transparent 38%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(255, 255, 255, 0.74));
  text-align: left;
  cursor: pointer;
  transition:
    transform 0.24s ease,
    box-shadow 0.24s ease;
}

.workspace-brand:hover {
  transform: translateY(-3px);
  box-shadow: 0 22px 44px rgba(15, 23, 42, 0.12);
}

.workspace-brand-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 52px;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92), var(--workspace-accent-soft));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.workspace-brand-meta {
  min-width: 0;
}

.brand-status {
  display: inline-flex;
  align-items: center;
  margin-top: 12px;
  padding: 5px 12px;
  border-radius: 999px;
  background: var(--workspace-accent-soft);
  color: var(--workspace-accent-deep);
  font-size: 12px;
  font-weight: 700;
}

.workspace-brand-meta h3,
.workspace-header h1,
.workspace-summary-item strong,
.workspace-menu-group-header strong,
.sidebar-action-button strong,
.workspace-focus-card strong {
  margin: 0;
  color: var(--workspace-title);
}

.workspace-brand-meta h3 {
  font-size: 22px;
  line-height: 1.15;
  font-family: var(--workspace-font-serif);
}

.workspace-brand-meta p,
.workspace-header p,
.workspace-sidebar-panel p,
.workspace-menu-group-header p,
.sidebar-action-button p,
.workspace-focus-card p {
  margin: 10px 0 0;
  color: #64748b;
  line-height: 1.75;
}

.brand-kicker,
.page-kicker,
.workspace-panel-kicker,
.focus-card-kicker {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: rgba(71, 85, 105, 0.92);
}

.workspace-sidebar-panel {
  padding: 18px;
  border-radius: 26px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.78), rgba(255, 255, 255, 0.58));
}

.workspace-sidebar-panel strong,
.workspace-focus-card strong {
  display: block;
  margin-top: 14px;
  color: var(--workspace-title);
  font-size: 22px;
  line-height: 1.3;
  font-family: var(--workspace-font-serif);
}

.workspace-summary-grid {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.workspace-summary-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.workspace-sidebar-actions {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.sidebar-action-button,
.workspace-nav-button,
.focus-card-action {
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease,
    background-color 0.2s ease;
}

.sidebar-action-button {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  padding: 14px 16px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  text-align: left;
  cursor: pointer;
}

.sidebar-action-button:hover,
.workspace-nav-button:hover,
.focus-card-action:hover {
  transform: translateY(-2px);
  border-color: rgba(148, 163, 184, 0.26);
  box-shadow: 0 18px 34px rgba(15, 23, 42, 0.08);
}

.sidebar-action-button span {
  flex-shrink: 0;
  color: var(--workspace-accent-deep);
  font-size: 12px;
  font-weight: 700;
}

.workspace-summary-item {
  padding: 14px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.7);
  border: 1px solid rgba(148, 163, 184, 0.12);
}

.workspace-summary-item span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.workspace-summary-item strong {
  display: block;
  margin-top: 10px;
  font-size: 15px;
  line-height: 1.45;
}

.workspace-menu-scroll {
  flex: 1;
  min-height: 0;
}

.workspace-menu-stack {
  display: grid;
  gap: 16px;
  padding-right: 4px;
}

.workspace-menu-group {
  display: grid;
  gap: 12px;
}

.workspace-menu-group-header {
  padding: 0 6px;
}

.workspace-menu-group-header strong {
  display: block;
  font-size: 13px;
  letter-spacing: 0.05em;
}

.workspace-menu-list {
  display: grid;
  gap: 8px;
}

.workspace-nav-button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  width: 100%;
  min-height: 72px;
  padding: 14px;
  border: 1px solid transparent;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.52);
  color: #334155;
  text-align: left;
  cursor: pointer;
}

.workspace-nav-button.is-active {
  border-color: rgba(255, 255, 255, 0.66);
  background: linear-gradient(135deg, var(--workspace-accent-soft), rgba(255, 255, 255, 0.92));
  color: var(--workspace-accent-deep);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.66);
}

.menu-item-main {
  display: flex;
  align-items: flex-start;
  flex: 1;
  gap: 12px;
  min-width: 0;
}

.menu-icon-wrap {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.82);
  color: var(--workspace-accent-deep);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.86);
}

.workspace-nav-button.is-active .menu-icon-wrap {
  background: rgba(255, 255, 255, 0.94);
}

.menu-item-badge {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  min-height: 28px;
  padding: 0 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--workspace-accent-deep);
  font-size: 12px;
  font-weight: 700;
}

.menu-item-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.menu-item-copy span {
  font-weight: 600;
  color: inherit;
}

.menu-item-copy small {
  color: #64748b;
  line-height: 1.4;
  white-space: normal;
}

.workspace-nav-button.is-active .menu-item-copy small {
  color: rgba(15, 23, 42, 0.68);
}

.workspace-focus-card {
  padding: 18px;
  border-radius: 24px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.82), rgba(255, 255, 255, 0.62));
}

.focus-card-action {
  width: 100%;
  min-height: 46px;
  margin-top: 16px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.86);
  color: var(--workspace-accent-deep);
  font-weight: 700;
  cursor: pointer;
}

.workspace-sidebar-footer {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.footer-link {
  min-height: 44px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.74);
  color: #475569;
  font-weight: 600;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.footer-link:hover {
  transform: translateY(-2px);
  border-color: rgba(148, 163, 184, 0.28);
  box-shadow: 0 16px 30px rgba(15, 23, 42, 0.08);
}

.footer-link.is-accent {
  border-color: transparent;
  background: linear-gradient(135deg, var(--workspace-accent), var(--workspace-accent-deep));
  color: #ffffff;
}

.workspace-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.workspace-header {
  display: block;
}

.workspace-header-main {
  padding: 26px;
  border-radius: 32px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.88), rgba(255, 255, 255, 0.7)),
    var(--workspace-surface);
  box-shadow:
    0 24px 50px rgba(15, 23, 42, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.page-kicker-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 14px;
}

.page-index {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: var(--workspace-accent-soft);
  color: var(--workspace-accent-deep);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.workspace-header h1 {
  margin-top: 18px;
  font-size: clamp(38px, 4.6vw, 56px);
  line-height: 1.02;
  font-family: var(--workspace-font-serif);
}

.page-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 20px;
}

.page-chip {
  display: inline-flex;
  align-items: center;
  min-height: 38px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(148, 163, 184, 0.14);
  color: #475569;
  font-size: 13px;
}

.page-chip.is-accent {
  background: var(--workspace-accent-soft);
  color: var(--workspace-accent-deep);
}

.workspace-content {
  min-width: 0;
  padding-bottom: 24px;
}

:deep(.el-breadcrumb__item) {
  cursor: pointer;
}

:deep(.el-breadcrumb__inner) {
  color: #64748b;
}

@media (max-width: 1280px) {
  .workspace-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1040px) {
  .workspace-shell {
    grid-template-columns: 1fr;
    padding: 18px;
  }

  .workspace-sidebar {
    position: static;
    height: auto;
  }
}

@media (max-width: 720px) {
  .workspace-shell {
    gap: 16px;
    padding: 14px;
  }

  .workspace-sidebar,
  .workspace-header-main {
    padding: 18px;
    border-radius: 24px;
  }

  .workspace-brand,
  .workspace-nav-button,
  .sidebar-action-button {
    padding: 16px;
  }

  .workspace-brand,
  .workspace-nav-button,
  .sidebar-action-button {
    flex-direction: column;
  }

  .workspace-summary-grid,
  .workspace-sidebar-footer {
    grid-template-columns: 1fr;
  }

  .workspace-header h1 {
    font-size: clamp(30px, 9vw, 40px);
  }
}
</style>
