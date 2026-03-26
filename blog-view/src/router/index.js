import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { useUserStore } from '@/store/user'
import { WORKSPACE_CAPABILITIES } from '@/utils/workspaceCapabilities'

const workspaceViewLoaders = {
  user: {
    layout: () => import('@/views/user/Layout.vue'),
    overview: () => import('@/views/user/ProfileOverview.vue'),
    notifications: () => import('@/views/user/NotificationCenter.vue'),
    account: () => import('@/views/user/AccountSettings.vue'),
    articleManagement: () => import('@/views/user/ArticleManagement.vue'),
    momentManagement: () => import('@/views/user/MomentManagement.vue'),
    articleEditor: () => import('@/views/user/PostArticle.vue'),
    momentEditor: () => import('@/views/user/PostMoment.vue'),
    forumPublish: () => import('@/views/user/PostForumThread.vue'),
    forumManagement: () => import('@/views/user/ForumThreadManagement.vue'),
  },
  admin: {
    layout: () => import('@/views/admin/Layout.vue'),
    overview: () => import('@/views/admin/AdminOverview.vue'),
    articleManagement: () => import('@/views/admin/ArticleManagement.vue'),
    commentManagement: () => import('@/views/admin/AdminCommentManagement.vue'),
    reportManagement: () => import('@/views/admin/AdminReportManagement.vue'),
    momentManagement: () => import('@/views/admin/MomentManagement.vue'),
    articleEditor: () => import('@/views/admin/PostArticle.vue'),
    momentEditor: () => import('@/views/admin/PostMoment.vue'),
    categoryManagement: () => import('@/views/admin/AdminCategoryManagement.vue'),
    tagManagement: () => import('@/views/admin/AdminTagManagement.vue'),
    forumPublish: () => import('@/views/admin/PostForumThread.vue'),
    forumManagement: () => import('@/views/admin/ForumThreadManagement.vue'),
    siteOverview: () => import('@/views/admin/AdminSiteOverview.vue'),
    userManagement: () => import('@/views/admin/AdminUserManagement.vue'),
  },
}

const createWorkspaceChildren = (workspaceMode) => {
  const isAdminWorkspace = workspaceMode === 'admin'
  const routeBase = isAdminWorkspace ? '/admin' : '/profile'
  const routeNamePrefix = isAdminWorkspace ? 'admin' : 'profile'
  const views = workspaceViewLoaders[workspaceMode]

  const routes = [
    {
      path: 'overview',
      name: `${routeNamePrefix}Overview`,
      component: views.overview,
      meta: {
        requiresAuth: true,
        requiredCapability: isAdminWorkspace
          ? WORKSPACE_CAPABILITIES.DASHBOARD_VIEW
          : WORKSPACE_CAPABILITIES.PROFILE_OVERVIEW_VIEW,
      },
    },
    {
      path: 'articlemgmt',
      name: `${routeNamePrefix}ArticleMgmt`,
      component: views.articleManagement,
      meta: {
        requiresAuth: true,
        requiredCapability: isAdminWorkspace
          ? WORKSPACE_CAPABILITIES.ARTICLE_MANAGE_ALL
          : WORKSPACE_CAPABILITIES.ARTICLE_MANAGE_OWN,
      },
    },
  ]

  if (!isAdminWorkspace) {
    routes.splice(1, 0,
      {
        path: 'notifications',
        name: `${routeNamePrefix}Notifications`,
        component: views.notifications,
        meta: {
          requiresAuth: true,
          requiredCapability: WORKSPACE_CAPABILITIES.PROFILE_NOTIFICATIONS_VIEW,
        },
      },
      {
        path: 'account',
        name: `${routeNamePrefix}Account`,
        component: views.account,
        meta: {
          requiresAuth: true,
          requiredCapability: WORKSPACE_CAPABILITIES.PROFILE_ACCOUNT_EDIT,
        },
      },
    )
  }

  if (isAdminWorkspace) {
    routes.push({
      path: 'commentmgmt',
      name: 'adminCommentMgmt',
      component: views.commentManagement,
      meta: {
        requiresAuth: true,
        requiredCapability: WORKSPACE_CAPABILITIES.COMMENT_MODERATE,
      },
    })

    routes.push({
      path: 'reportmgmt',
      name: 'adminReportMgmt',
      component: views.reportManagement,
      meta: {
        requiresAuth: true,
        requiredCapability: WORKSPACE_CAPABILITIES.REPORT_REVIEW,
      },
    })
  }

  routes.push(
    {
      path: 'momentsmgmt',
      name: `${routeNamePrefix}MomentsMgmt`,
      component: views.momentManagement,
      meta: {
        requiresAuth: true,
        requiredCapability: isAdminWorkspace
          ? WORKSPACE_CAPABILITIES.MOMENT_MANAGE_ALL
          : WORKSPACE_CAPABILITIES.MOMENT_MANAGE_OWN,
      },
    },
    {
      path: 'content',
      redirect: `${routeBase}/articlemgmt`,
    },
    {
      path: 'writearticle',
      name: `${routeNamePrefix}WriteArticle`,
      component: views.articleEditor,
      meta: {
        requiresAuth: true,
        requiredCapability: WORKSPACE_CAPABILITIES.ARTICLE_WRITE,
      },
    },
    {
      path: 'article/edit/:id',
      name: `${routeNamePrefix}EditArticle`,
      component: views.articleEditor,
      meta: {
        requiresAuth: true,
        requiredCapability: WORKSPACE_CAPABILITIES.ARTICLE_WRITE,
      },
    },
    {
      path: 'writemoment',
      name: `${routeNamePrefix}WriteMoment`,
      component: views.momentEditor,
      meta: {
        requiresAuth: true,
        requiredCapability: WORKSPACE_CAPABILITIES.MOMENT_WRITE,
      },
    },
  )

  if (isAdminWorkspace) {
    routes.push(
      {
        path: 'categorisemgmt',
        name: 'adminCategoriseMgmt',
        component: views.categoryManagement,
        meta: {
          requiresAuth: true,
          requiredCapability: WORKSPACE_CAPABILITIES.TAXONOMY_MANAGE,
        },
      },
      {
        path: 'tagsmgmt',
        name: 'adminTagsMgmt',
        component: views.tagManagement,
        meta: {
          requiresAuth: true,
          requiredCapability: WORKSPACE_CAPABILITIES.TAXONOMY_MANAGE,
        },
      },
      {
        path: 'usermgmt',
        name: 'adminUserMgmt',
        component: views.userManagement,
        meta: {
          requiresAuth: true,
          requiredCapability: WORKSPACE_CAPABILITIES.USER_MANAGE,
        },
      },
    )
  }

  routes.push(
    {
      path: 'forum-publish',
      name: `${routeNamePrefix}ForumPublish`,
      component: views.forumPublish,
      meta: {
        requiresAuth: true,
        requiredCapability: WORKSPACE_CAPABILITIES.FORUM_POST_WRITE,
      },
    },
    {
      path: 'forum-manage',
      name: `${routeNamePrefix}ForumManage`,
      component: views.forumManagement,
      meta: {
        requiresAuth: true,
        requiredCapability: isAdminWorkspace
          ? WORKSPACE_CAPABILITIES.FORUM_POST_MODERATE
          : WORKSPACE_CAPABILITIES.FORUM_POST_MANAGE_OWN,
      },
    },
    {
      path: 'forum-entry',
      redirect: `${routeBase}/forum-publish`,
    },
  )

  if (isAdminWorkspace) {
    routes.push({
      path: 'site',
      name: 'adminSite',
      component: views.siteOverview,
      meta: {
        requiresAuth: true,
        requiredCapability: WORKSPACE_CAPABILITIES.SITE_MANAGE,
      },
    })
  }

  return routes
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/Login.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      name: 'layout',
      component: () => import('@/views/Layout.vue'),
      redirect: '/home',
      meta: { requiresAuth: true },
      children: [
        {
          path: 'home',
          name: 'home',
          component: () => import('@/views/Home.vue'),
        },
        {
          path: 'tag/:id',
          name: 'tag',
          component: () => import('@/views/Tags.vue'),
        },
        {
          path: 'archive',
          name: 'archive',
          component: () => import('@/views/Archive.vue'),
        },
        {
          path: 'about',
          name: 'about',
          component: () => import('@/views/About.vue'),
        },
        {
          path: 'moment',
          name: 'moment',
          component: () => import('@/views/Moment.vue'),
        },
        {
          path: 'friendlinks',
          redirect: '/forum',
        },
        {
          path: 'forum',
          name: 'forum',
          component: () => import('@/views/Forum.vue'),
        },
        {
          path: 'forum/:id',
          name: 'forumPostDetail',
          component: () => import('@/views/ForumPostDetail.vue'),
        },

        {
          path: 'article/:id',
          name: 'articleDetail',
          component: () => import('@/views/ArticleDetail.vue'),
        },
        {
          path: 'category/:id',
          name: 'categoryDetail',
          component: () => import('@/views/CategoryDetail.vue'),
        },
      ],
    },
    {
      path: '/profile',
      component: workspaceViewLoaders.user.layout,
      redirect: '/profile/overview',
      meta: { requiresAuth: true },
      children: createWorkspaceChildren('user'),
    },
    {
      path: '/admin',
      component: workspaceViewLoaders.admin.layout,
      redirect: '/admin/overview',
      meta: { requiresAuth: true, requiresAdmin: true },
      children: createWorkspaceChildren('admin'),
    },
  ],
  // 路由切换时自动滚动到页面顶部
  scrollBehavior(to, from, savedPosition) {
    // 如果路径变了（从页面A跳到页面B），但没有锚点，才回到顶部
    if (to.path !== from.path && !to.hash) {
      const container = document.querySelector('.main-scroll-container')
      if (container) {
        container.scrollTop = 0
      }
    }

    // 如果有锚点，我们什么都不做，让 el-anchor 自己去处理跳转
    if (to.hash) {
      return false // 返回 false 表示不干预滚动
    }
  },
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  const userStore = useUserStore()
  const isLoggedIn = Boolean(authStore.accessToken)
  const requiresAuth = Boolean(to.meta?.requiresAuth)
  const requiresAdmin = Boolean(to.meta?.requiresAdmin)
  const requiredCapability = to.meta?.requiredCapability

  if (!isLoggedIn && requiresAuth) {
    return {
      path: '/login',
      query: { redirect: to.fullPath },
    }
  }

  if (isLoggedIn && to.path === '/login' && !to.query.code) {
    return '/home'
  }

  if (requiresAdmin && !userStore.isAdmin) {
    return '/profile/overview'
  }

  if (requiredCapability && !userStore.hasCapability(requiredCapability)) {
    return userStore.hasCapability(WORKSPACE_CAPABILITIES.DASHBOARD_VIEW)
      ? '/admin/overview'
      : '/profile/overview'
  }

  return true
})

export default router
