<template>
  <div class="overview-panel">
    <section class="overview-hero">
      <div class="hero-main">
        <div class="hero-profile">
          <div class="hero-avatar-wrap">
            <el-avatar :size="36" :src="profileSummary.avatar">{{ userInitial }}</el-avatar>
          </div>

          <div class="hero-copy">
            <p class="hero-kicker">Personal Overview</p>
            <h3>{{ profileSummary.nickname || profileSummary.username || 'Dream 用户' }}</h3>

            <div class="hero-tags">
              <el-tag effect="plain">{{
                profileSummary.role === 'ADMIN' ? '管理员账号' : '普通用户'
              }}</el-tag>
              <el-tag v-if="profileSummary.githubLogin" effect="plain" type="success">
                GitHub · {{ profileSummary.githubLogin }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>

      <aside class="hero-side">
        <span class="hero-side-kicker">Profile Status</span>
        <strong>{{ profileCompletion.rate }}%</strong>
        <p>
          资料完整度 {{ profileCompletion.completed }} /
          {{ profileCompletion.total }}，继续补全信息会让个人主页展示更完整。
        </p>

        <div class="progress-track">
          <i :style="{ width: `${profileCompletion.rate}%` }"></i>
        </div>

        <div class="hero-side-list">
          <article v-for="item in personalSignals" :key="item.label" class="hero-side-item">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <p>{{ item.detail }}</p>
          </article>
        </div>
      </aside>
    </section>

    <div class="stats-grid">
      <overview-stat-card
        label="我的帖子"
        :value="overview.postCount || 0"
        hint="累计在论坛发布的帖子数量。"
      />
      <overview-stat-card
        label="我的评论"
        :value="overview.commentCount || 0"
        hint="包括文章和论坛中的互动评论。"
      />
      <overview-stat-card
        label="未读通知"
        :value="overview.unreadNotificationCount || 0"
        hint="新的处理结果和站内提醒会集中出现在这里。"
      />
      <overview-stat-card
        label="待处理举报"
        :value="overview.pendingReportCount || 0"
        hint="你提交的举报或待管理员处理的反馈数量。"
      />
    </div>

    <section class="overview-action-grid">
      <article v-for="item in overviewActionCards" :key="item.title" class="action-card">
        <span>{{ item.kicker }}</span>
        <strong>{{ item.title }}</strong>
        <p>{{ item.description }}</p>

        <div class="action-card-footer">
          <small>{{ item.meta }}</small>
          <el-button text type="primary" @click="goTo(item.path)">{{ item.actionLabel }}</el-button>
        </div>
      </article>
    </section>

    <section class="insight-grid">
      <article v-for="item in overviewHighlights" :key="item.kicker" class="insight-card">
        <span>{{ item.kicker }}</span>
        <strong>{{ item.title }}</strong>
        <p>{{ item.description }}</p>
      </article>
    </section>

    <div class="overview-grid overview-grid-two">
      <overview-section-card
        title="最近创作"
        description="优先展示你最近发布的帖子，可快速返回继续查看。"
      >
        <div v-if="overview.myPosts?.length" class="list-stack">
          <button
            v-for="post in overview.myPosts.slice(0, 5)"
            :key="post.id"
            type="button"
            class="list-item-button"
            @click="goToForumPost(post.id)"
          >
            <div class="list-item-copy">
              <strong>{{ post.title || '未命名帖子' }}</strong>
              <p>{{ post.summary || '进入帖子详情查看完整内容。' }}</p>
            </div>
            <span>{{ formatManagementTime(post.lastActivityTime || post.createTime) }}</span>
          </button>
        </div>
        <el-empty v-else :image-size="72" description="暂时还没有帖子，去发布第一篇讨论吧。" />
      </overview-section-card>

      <overview-section-card title="最近互动" description="查看你最近参与过的评论和互动位置。">
        <div v-if="overview.myComments?.length" class="list-stack">
          <button
            v-for="comment in overview.myComments.slice(0, 5)"
            :key="comment.id"
            type="button"
            class="list-item-button"
            @click="goToCommentTarget(comment)"
          >
            <div class="list-item-copy">
              <strong>{{ getCommentTargetLabel(comment) }}</strong>
              <p>{{ comment.content || '暂无评论内容' }}</p>
            </div>
            <span>{{ formatManagementTime(comment.createTime) }}</span>
          </button>
        </div>
        <el-empty v-else :image-size="72" description="最近还没有新的评论互动。" />
      </overview-section-card>
    </div>

    <div class="overview-grid overview-grid-two">
      <overview-section-card title="通知中心" description="新的处理结果和站内通知会显示在这里。">
        <template #header-extra>
          <el-button text type="primary" @click="goTo('/profile/notifications')"
            >查看全部</el-button
          >
        </template>
        <div v-if="overview.notifications?.length" class="list-stack">
          <article
            v-for="notification in overview.notifications.slice(0, 6)"
            :key="notification.id"
            class="notification-item"
          >
            <div class="list-item-copy">
              <strong>{{ notification.title }}</strong>
              <p>{{ notification.content }}</p>
              <small>{{ formatManagementTime(notification.createTime) }}</small>
            </div>

            <div class="notification-actions">
              <el-tag :type="notification.read ? 'info' : 'warning'" effect="plain">
                {{ notification.read ? '已读' : '未读' }}
              </el-tag>
              <el-button
                v-if="!notification.read"
                text
                type="primary"
                @click="markNotificationRead(notification)"
              >
                标记已读
              </el-button>
              <el-button text @click="openNotificationTarget(notification)">查看</el-button>
            </div>
          </article>
        </div>
        <el-empty v-else :image-size="72" description="暂无新的通知提醒。" />
      </overview-section-card>

      <overview-section-card
        title="举报进度"
        description="这里会同步你提交的举报记录和管理员处理状态。"
      >
        <div v-if="displayedReports.length" class="list-stack">
          <article
            v-for="report in displayedReports"
            :id="`profile-report-${report.id}`"
            :key="report.id"
            class="report-item"
            :class="{ 'is-highlighted': isHighlightedReport(report.id) }"
          >
            <div class="list-item-copy">
              <strong>{{ report.targetTitle || `内容 #${report.targetId}` }}</strong>
              <p>{{ report.detail || `举报原因：${report.reason || '未填写'}` }}</p>
              <small>{{ formatManagementTime(report.updateTime || report.createTime) }}</small>
            </div>
            <el-tag :type="resolveReportStatusType(report.status)" effect="plain">
              {{ resolveReportStatusLabel(report.status) }}
            </el-tag>
          </article>
        </div>
        <el-empty v-else :image-size="72" description="你还没有提交过举报记录。" />
      </overview-section-card>
    </div>

    <overview-section-card title="账号信息" description="帮助你快速确认当前资料与登录方式。">
      <template #header-extra>
        <el-button text type="primary" @click="goTo('/profile/account')">修改头像 / 密码</el-button>
      </template>
      <div class="account-grid">
        <article class="account-item">
          <span>用户名</span>
          <strong>@{{ profileSummary.username || '--' }}</strong>
        </article>
        <article class="account-item">
          <span>邮箱</span>
          <strong>{{ profileSummary.email || '未填写' }}</strong>
        </article>
        <article class="account-item">
          <span>手机号</span>
          <strong>{{ profileSummary.phone || '未填写' }}</strong>
        </article>
        <article class="account-item">
          <span>登录方式</span>
          <strong>{{ profileSummary.githubLogin ? 'GitHub + 站内资料' : '站内账号' }}</strong>
        </article>
      </div>
    </overview-section-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchCurrentUserOverview, markCurrentUserNotificationRead } from '@/api/user'
import OverviewSectionCard from '@/components/workspace/shared/OverviewSectionCard.vue'
import OverviewStatCard from '@/components/workspace/shared/OverviewStatCard.vue'
import { useUserStore } from '@/store/user'
import { formatManagementTime, getCommentTargetLabel } from '@/utils/profileManagement'
import { WORKSPACE_CAPABILITIES } from '@/utils/workspaceCapabilities'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const overview = ref({
  profile: null,
  postCount: 0,
  commentCount: 0,
  unreadNotificationCount: 0,
  pendingReportCount: 0,
  myPosts: [],
  myComments: [],
  reports: [],
  notifications: [],
})

const canViewAdminDashboard = computed(() =>
  userStore.hasCapability(WORKSPACE_CAPABILITIES.DASHBOARD_VIEW),
)
const highlightedReportId = computed(() => Number(route.query.reportId || 0) || null)
const displayedReports = computed(() => {
  const items = Array.isArray(overview.value.reports) ? [...overview.value.reports] : []

  if (highlightedReportId.value) {
    items.sort((left, right) => {
      if (left.id === highlightedReportId.value) return -1
      if (right.id === highlightedReportId.value) return 1
      return 0
    })
  }

  return items.slice(0, 6)
})
const profileSummary = computed(() => overview.value.profile || userStore.profile || {})
const userInitial = computed(() => profileSummary.value.nickname?.slice(0, 1)?.toUpperCase() || 'D')
const profileCompletion = computed(() => {
  const checks = [
    Boolean(profileSummary.value.avatar),
    Boolean(profileSummary.value.bio),
    Boolean(profileSummary.value.email),
    Boolean(profileSummary.value.phone),
    Boolean(profileSummary.value.passwordInitialized),
  ]
  const completed = checks.filter(Boolean).length
  const total = checks.length

  return {
    completed,
    total,
    rate: Math.round((completed / total) * 100),
  }
})
const personalSignals = computed(() => [
  {
    label: '最近创作',
    value: `${overview.value.postCount || 0} 篇累计内容`,
    detail: overview.value.myPosts?.[0]?.title || '继续发布新的文章、动态或帖子。',
  },
  {
    label: '互动提醒',
    value: `${overview.value.commentCount || 0} 条累计评论`,
    detail: overview.value.unreadNotificationCount
      ? `还有 ${overview.value.unreadNotificationCount} 条未读通知待查看。`
      : '最近的通知已经处理得差不多了。',
  },
  {
    label: '账号状态',
    value: profileSummary.value.passwordInitialized ? '安全信息已补全' : '建议补充密码',
    detail: profileSummary.value.githubLogin
      ? '当前支持 GitHub 登录与站内资料并行使用。'
      : '当前使用站内账号资料登录。',
  },
])
const overviewHighlights = computed(() => {
  const latestPost = overview.value.myPosts?.[0]
  const latestNotification = overview.value.notifications?.[0]
  const latestReport = displayedReports.value[0]

  return [
    {
      kicker: '创作动向',
      title: latestPost?.title || '准备你的下一篇内容',
      description:
        latestPost?.summary || '最近发布的帖子和文章会优先显示在这里，方便你快速接回创作节奏。',
    },
    {
      kicker: '互动提醒',
      title: overview.value.unreadNotificationCount
        ? `还有 ${overview.value.unreadNotificationCount} 条通知待查看`
        : '通知处理进度很清爽',
      description:
        latestNotification?.content || '站内提醒会帮助你回到关键内容位置，减少错过互动的情况。',
    },
    {
      kicker: '举报进度',
      title: overview.value.pendingReportCount
        ? `有 ${overview.value.pendingReportCount} 条反馈仍在处理中`
        : '当前没有需要继续跟进的举报',
      description: latestReport?.detail || '你提交过的举报会在这里保持同步，方便回看处理结果。',
    },
  ]
})
const overviewActionCards = computed(() => [
  {
    kicker: '消息整理',
    title: overview.value.unreadNotificationCount
      ? `还有 ${overview.value.unreadNotificationCount} 条通知待查看`
      : '通知中心已基本清空',
    description:
      overview.value.notifications?.[0]?.content ||
      '新的处理结果和站内提醒会在这里汇总，先处理消息能更顺畅回到创作。',
    meta: '通知中心',
    actionLabel: '查看通知',
    path: '/profile/notifications',
  },
  {
    kicker: '资料完善',
    title: `资料完整度 ${profileCompletion.value.rate}%`,
    description:
      profileCompletion.value.rate < 100
        ? '继续补充头像、简介、邮箱和手机号，会让个人空间呈现得更完整。'
        : '当前个人资料已经比较完整，可以保持现在的展示状态。',
    meta: `${profileCompletion.value.completed} / ${profileCompletion.value.total} 已完成`,
    actionLabel: '前往设置',
    path: '/profile/account',
  },
  {
    kicker: '创作节奏',
    title: overview.value.myPosts?.[0]?.title || '准备下一条内容发布',
    description:
      overview.value.myPosts?.[0]?.summary ||
      '可以继续写文章、发动态或发布帖子，把最近的灵感继续沉淀下来。',
    meta: `${overview.value.postCount || 0} 篇内容沉淀`,
    actionLabel: '开始写作',
    path: '/profile/writearticle',
  },
])

const scrollToHighlightedReport = async () => {
  if (!highlightedReportId.value) return

  await nextTick()
  const target = document.getElementById(`profile-report-${highlightedReportId.value}`)
  target?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

const loadOverview = async () => {
  loading.value = true

  try {
    const response = await fetchCurrentUserOverview()
    overview.value = {
      profile: response?.profile || null,
      postCount: Number(response?.postCount || 0),
      commentCount: Number(response?.commentCount || 0),
      unreadNotificationCount: Number(response?.unreadNotificationCount || 0),
      pendingReportCount: Number(response?.pendingReportCount || 0),
      myPosts: Array.isArray(response?.myPosts) ? response.myPosts : [],
      myComments: Array.isArray(response?.myComments) ? response.myComments : [],
      reports: Array.isArray(response?.reports) ? response.reports : [],
      notifications: Array.isArray(response?.notifications) ? response.notifications : [],
    }

    if (response?.profile) {
      userStore.hydrateFromServerProfile(response.profile)
    }

    await scrollToHighlightedReport()
  } catch (error) {
    console.error('加载个人总览失败', error)
    ElMessage.error(error.message || '加载个人总览失败，请稍后重试。')
  } finally {
    loading.value = false
  }
}

const goTo = (path) => router.push(path)

const goToForumPost = (id) => {
  if (!id) return
  router.push(`/forum/${id}`)
}

const goToCommentTarget = (comment) => {
  if (!comment) return

  if (comment.blogId) {
    router.push(`/article/${comment.blogId}`)
    return
  }

  if (comment.page?.startsWith('forum-post-')) {
    router.push(`/forum/${comment.page.replace('forum-post-', '')}`)
    return
  }

  if (comment.page === 'friends') {
    router.push('/forum')
  }
}

const openNotificationTarget = (notification) => {
  const notificationType = String(notification?.type || '').toUpperCase()

  if (notificationType === 'REPORT_CREATED' && notification?.relatedReportId) {
    if (userStore.hasCapability(WORKSPACE_CAPABILITIES.REPORT_REVIEW)) {
      router.push({
        path: '/admin/reportmgmt',
        query: { reportId: String(notification.relatedReportId) },
      })
    }
    return
  }

  if (notificationType === 'REPORT_REVIEWED' && notification?.relatedReportId) {
    router.push({
      path: '/profile/overview',
      query: { reportId: String(notification.relatedReportId) },
    })
    return
  }

  if (!notification?.targetId) return

  if (notification.targetType === 'forum-post') {
    router.push(`/forum/${notification.targetId}`)
    return
  }

  if (notification.targetType === 'article') {
    router.push(`/article/${notification.targetId}`)
  }
}

const markNotificationRead = async (notification) => {
  if (!notification?.id || notification.read) return

  try {
    await markCurrentUserNotificationRead(notification.id)
    notification.read = true
    overview.value.unreadNotificationCount = Math.max(
      0,
      Number(overview.value.unreadNotificationCount || 0) - 1,
    )
    ElMessage.success('通知已标记为已读')
  } catch (error) {
    console.error('标记通知失败', error)
    ElMessage.error(error.message || '标记通知失败，请稍后重试。')
  }
}

const resolveReportStatusLabel = (status = '') => {
  const normalized = String(status || '').toUpperCase()

  if (normalized === 'RESOLVED') return '已处理'
  if (normalized === 'REJECTED') return '已驳回'
  if (normalized === 'PENDING') return '待处理'

  return status || '未知状态'
}

const resolveReportStatusType = (status = '') => {
  const normalized = String(status || '').toUpperCase()

  if (normalized === 'RESOLVED') return 'success'
  if (normalized === 'REJECTED') return 'info'
  return 'warning'
}

const isHighlightedReport = (reportId) => highlightedReportId.value === reportId

onMounted(() => {
  loadOverview()
})

watch(
  () => route.query.reportId,
  async () => {
    await scrollToHighlightedReport()
  },
)
</script>

<style scoped>
.overview-panel {
  display: block;
}

.overview-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(280px, 360px);
  gap: 18px;
}

.hero-main,
.hero-side,
.insight-card,
.action-card {
  border-radius: 28px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 24px 52px rgba(15, 23, 42, 0.08);
}

.hero-main {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 22px;
  padding: 26px;
  background:
    radial-gradient(circle at top right, rgba(245, 158, 11, 0.18), transparent 34%),
    linear-gradient(135deg, rgba(255, 248, 239, 0.96), rgba(255, 255, 255, 0.92));
}

.hero-profile {
  display: flex;
  gap: 18px;
  align-items: flex-start;
}

.hero-avatar-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 96px;
  height: 96px;
  border-radius: 30px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.84), rgba(251, 191, 36, 0.18));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.88);
}

.hero-copy {
  min-width: 0;
}

.hero-kicker,
.hero-side-kicker,
.insight-card span,
.action-card span {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.68);
  color: #9a3412;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.overview-hero h3,
.hero-side strong,
.insight-card strong,
.action-card strong {
  margin: 0;
  color: #7c2d12;
  font-family: 'Noto Serif SC', 'Source Han Serif SC', Georgia, serif;
}

.overview-hero h3 {
  margin-top: 16px;
  font-size: clamp(32px, 4.2vw, 42px);
  line-height: 1.05;
}

.overview-hero p,
.hero-side p,
.insight-card p,
.action-card p {
  margin: 12px 0 0;
  color: #7c2d12;
  line-height: 1.72;
}

.hero-tags,
.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-tags {
  margin-top: 18px;
}

.hero-actions {
  justify-content: flex-start;
}

.hero-side {
  padding: 22px;
  background:
    radial-gradient(circle at top right, rgba(251, 191, 36, 0.12), transparent 40%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(255, 250, 245, 0.88));
}

.hero-side strong {
  display: block;
  margin-top: 18px;
  font-size: clamp(34px, 5vw, 48px);
  line-height: 0.96;
}

.progress-track {
  width: 100%;
  height: 10px;
  margin-top: 18px;
  border-radius: 999px;
  background: rgba(194, 65, 12, 0.1);
  overflow: hidden;
}

.progress-track i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #f59e0b, #c2410c);
}

.hero-side-list,
.list-stack {
  display: grid;
  gap: 12px;
}

.hero-side-list {
  margin-top: 18px;
}

.hero-side-item,
.list-item-button,
.notification-item,
.report-item,
.account-item {
  border-radius: 20px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  background: rgba(255, 255, 255, 0.76);
}

.hero-side-item {
  padding: 14px;
}

.hero-side-item span,
.account-item span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.hero-side-item strong,
.account-item strong {
  display: block;
  margin-top: 10px;
  color: #0f172a;
  font-size: 16px;
  line-height: 1.45;
  font-family: 'Noto Serif SC', 'Source Han Serif SC', Georgia, serif;
}

.hero-side-item p {
  margin-top: 8px;
  color: #64748b;
}

.stats-grid,
.overview-grid,
.account-grid,
.insight-grid,
.overview-action-grid {
  display: grid;
  gap: 16px;
  margin-top: 18px;
}

.stats-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.insight-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.overview-action-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.action-card {
  padding: 20px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(255, 249, 245, 0.88)),
    rgba(255, 255, 255, 0.94);
}

.action-card strong {
  display: block;
  margin-top: 18px;
  font-size: 24px;
  line-height: 1.24;
}

.action-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 20px;
}

.action-card-footer small {
  color: #64748b;
}

.insight-card {
  padding: 20px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(255, 248, 243, 0.88)),
    rgba(255, 255, 255, 0.94);
}

.insight-card strong {
  display: block;
  margin-top: 18px;
  font-size: 24px;
  line-height: 1.24;
}

.overview-grid-two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.list-item-button,
.notification-item,
.report-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
  padding: 16px;
  text-align: left;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.04);
}

.list-item-button {
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.list-item-button:hover {
  transform: translateY(-2px);
  border-color: rgba(245, 158, 11, 0.28);
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.08);
}

.list-item-copy {
  min-width: 0;
}

.list-item-copy strong {
  display: block;
  color: #0f172a;
}

.list-item-copy p {
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.68;
}

.list-item-button span,
.notification-item small,
.report-item small {
  flex-shrink: 0;
  color: #64748b;
}

.notification-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.report-item.is-highlighted {
  border-color: rgba(245, 158, 11, 0.55);
  box-shadow: 0 0 0 4px rgba(251, 191, 36, 0.14);
}

.account-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.account-item {
  padding: 18px;
}

@media (max-width: 1180px) {
  .overview-hero,
  .insight-grid,
  .stats-grid,
  .account-grid,
  .overview-action-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .overview-hero,
  .overview-grid-two {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .hero-main,
  .hero-side,
  .insight-card,
  .action-card,
  .list-item-button,
  .notification-item,
  .report-item {
    padding: 18px;
  }

  .hero-profile,
  .list-item-button,
  .notification-item,
  .report-item {
    flex-direction: column;
  }

  .stats-grid,
  .account-grid,
  .insight-grid,
  .overview-action-grid {
    grid-template-columns: 1fr;
  }

  .hero-actions,
  .notification-actions {
    width: 100%;
    align-items: stretch;
  }
}
</style>
