<template>
  <workspace-page-shell
    title="管理概览"
    :loading="loading"
    @refresh="loadOverview"
  >
    <template v-if="!canViewDashboard">
      <workspace-permission-notice message="当前账号没有查看管理概览的权限。" />
    </template>
    <template v-else>
      <section class="overview-hero">
        <div class="hero-main">
          <p class="hero-kicker">Admin Overview</p>
          <h3>{{ siteInfo.name || 'Dream Studio' }}</h3>
          <p>{{ siteInfo.description || '从这里总览站点内容状态、处理待办并快速进入常用管理页。' }}</p>

          <div class="hero-tags">
            <el-tag effect="plain">{{ formatAnnouncementCount(announcements.length) }}</el-tag>
            <el-tag effect="plain" type="warning">待处理举报 {{ adminSummary.pendingReportCount || 0 }}</el-tag>
            <el-tag effect="plain" type="success">未读通知 {{ adminSummary.unreadNotificationCount || 0 }}</el-tag>
          </div>

          <div class="hero-actions">
            <el-button type="primary" @click="goTo('/admin/writearticle')">写文章</el-button>
            <el-button plain @click="goTo('/admin/commentmgmt')">评论管理</el-button>
            <el-button plain @click="goTo('/admin/usermgmt')">用户管理</el-button>
            <el-button text @click="goTo('/admin/site')">站点设置</el-button>
          </div>
        </div>

        <aside class="hero-side">
          <span class="hero-side-kicker">Console Health</span>
          <strong>{{ siteHealthScore }}</strong>
          <p>{{ siteHealthLabel }}</p>

          <div class="progress-track">
            <i :style="{ width: `${siteHealthScore}%` }"></i>
          </div>

          <div class="hero-side-list">
            <article v-for="item in adminPulseList" :key="item.label" class="hero-side-item">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <p>{{ item.detail }}</p>
            </article>
          </div>
        </aside>
      </section>

      <div class="stats-grid">
        <overview-stat-card label="文章总数" :value="dashboard.articleCount" hint="当前全站累计文章数量。" />
        <overview-stat-card label="评论总数" :value="dashboard.commentCount" hint="包括文章和论坛中的评论互动。" />
        <overview-stat-card label="动态总数" :value="dashboard.momentCount" hint="用于观察时间轴内容更新频率。" />
        <overview-stat-card label="论坛帖子" :value="dashboard.forumPostCount" hint="集中反映论坛活跃度与讨论沉淀。" />
      </div>

      <section class="control-grid">
        <article v-for="item in controlDeck" :key="item.title" class="control-card">
          <span>{{ item.kicker }}</span>
          <strong>{{ item.title }}</strong>
          <p>{{ item.description }}</p>

          <div class="control-card-footer">
            <small>{{ item.meta }}</small>
            <el-button text type="primary" @click="goTo(item.path)">{{ item.actionLabel }}</el-button>
          </div>
        </article>
      </section>

      <section class="insight-grid">
        <article v-for="item in managementHighlights" :key="item.kicker" class="insight-card">
          <span>{{ item.kicker }}</span>
          <strong>{{ item.title }}</strong>
          <p>{{ item.description }}</p>
        </article>
      </section>

      <div class="overview-grid overview-grid-two">
        <overview-section-card title="近期贡献趋势" description="后端统计接口已经返回发文趋势，这里用简洁柱状条展示。">
          <div v-if="contributionSeries.length" class="bar-grid">
            <div v-for="item in contributionSeries" :key="item.label" class="bar-item">
              <span>{{ item.value }}</span>
              <div class="bar-track">
                <i :style="{ height: `${item.height}%` }"></i>
              </div>
              <small>{{ item.label }}</small>
            </div>
          </div>
          <el-empty v-else :image-size="72" description="暂无趋势数据。" />
        </overview-section-card>

        <overview-section-card title="待处理事项" description="优先关注需要立即处理的管理动作。">
          <div class="task-list">
            <article class="task-item">
              <div>
                <strong>待处理举报</strong>
                <p>当前还有 {{ adminSummary.pendingReportCount || 0 }} 条举报等待处理。</p>
              </div>
              <el-button text type="primary" @click="goTo('/admin/reportmgmt')">查看详情</el-button>
            </article>
            <article class="task-item">
              <div>
                <strong>未读通知</strong>
                <p>你有 {{ adminSummary.unreadNotificationCount || 0 }} 条站内提醒尚未查看。</p>
              </div>
              <el-button text type="primary" @click="goTo('/profile/notifications')">前往通知中心</el-button>
            </article>
            <article class="task-item">
              <div>
                <strong>站点公告</strong>
                <p>当前展示 {{ announcements.length }} 条公告，可在站点设置页继续维护。</p>
              </div>
              <el-button text type="primary" @click="goTo('/admin/site')">管理公告</el-button>
            </article>
          </div>
        </overview-section-card>
      </div>

      <div class="overview-grid overview-grid-two">
        <overview-section-card title="分类分布" description="帮助快速识别内容集中在哪些分类下。">
          <div v-if="categoryDistribution.length" class="distribution-list">
            <div v-for="item in categoryDistribution" :key="item.label" class="distribution-item">
              <div class="distribution-meta">
                <strong>{{ item.label }}</strong>
                <span>{{ item.value }}</span>
              </div>
              <div class="distribution-track">
                <i :style="{ width: `${item.width}%` }"></i>
              </div>
            </div>
          </div>
          <el-empty v-else :image-size="72" description="暂无分类统计数据。" />
        </overview-section-card>

        <overview-section-card title="标签热度" description="帮助判断当前内容热点和标签使用情况。">
          <div v-if="tagDistribution.length" class="distribution-list">
            <div v-for="item in tagDistribution" :key="item.label" class="distribution-item">
              <div class="distribution-meta">
                <strong>#{{ item.label }}</strong>
                <span>{{ item.value }}</span>
              </div>
              <div class="distribution-track">
                <i :style="{ width: `${item.width}%` }"></i>
              </div>
            </div>
          </div>
          <el-empty v-else :image-size="72" description="暂无标签统计数据。" />
        </overview-section-card>
      </div>

      <div class="overview-grid overview-grid-two">
        <overview-section-card title="最近通知" description="管理员账号最近收到的站内提醒。">
          <div v-if="recentNotifications.length" class="list-stack">
            <article v-for="notification in recentNotifications" :key="notification.id" class="list-item">
              <div class="list-item-copy">
                <strong>{{ notification.title }}</strong>
                <p>{{ notification.content }}</p>
              </div>
              <span>{{ formatManagementTime(notification.createTime) }}</span>
            </article>
          </div>
          <el-empty v-else :image-size="72" description="暂无通知。" />
        </overview-section-card>

        <overview-section-card title="活跃帖子" description="优先关注最近仍有互动的帖子。">
          <div v-if="moderationPosts.length" class="list-stack">
            <button v-for="post in moderationPosts" :key="post.id" type="button" class="list-item-button" @click="goToForumPost(post.id)">
              <div class="list-item-copy">
                <strong>{{ post.title || '未命名帖子' }}</strong>
                <p>{{ post.summary || '进入详情查看当前讨论内容。' }}</p>
              </div>
              <span>{{ formatManagementTime(post.lastActivityTime || post.createTime) }}</span>
            </button>
          </div>
          <el-empty v-else :image-size="72" description="暂无活跃帖子数据。" />
        </overview-section-card>
      </div>

      <div class="overview-grid overview-grid-two">
        <overview-section-card title="站点摘要" description="概览当前站点基础信息。">
          <div class="site-info-grid">
            <article class="site-info-item">
              <span>网站名称</span>
              <strong>{{ siteInfo.name || 'Dream Studio' }}</strong>
            </article>
            <article class="site-info-item">
              <span>网站作者</span>
              <strong>{{ siteInfo.author || '未填写' }}</strong>
            </article>
            <article class="site-info-item">
              <span>网站关键词</span>
              <strong>{{ siteInfo.keywords || '未填写' }}</strong>
            </article>
            <article class="site-info-item">
              <span>ICP备案号</span>
              <strong>{{ siteInfo.icp || '未填写' }}</strong>
            </article>
          </div>
        </overview-section-card>

        <overview-section-card title="最新公告" description="论坛页当前展示的公告内容。">
          <div v-if="announcements.length" class="list-stack">
            <article v-for="item in announcements.slice(0, 4)" :key="item.id" class="list-item">
              <div class="list-item-copy">
                <strong>{{ item.title }}</strong>
                <p>{{ item.content }}</p>
              </div>
              <span>{{ formatManagementTime(item.publishTime) }}</span>
            </article>
          </div>
          <el-empty v-else :image-size="72" description="暂无公告。" />
        </overview-section-card>
      </div>
    </template>
  </workspace-page-shell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  fetchAdminCategoryStatistics,
  fetchAdminContributionStatistics,
  fetchAdminDashboardStatistics,
  fetchAdminTagStatistics,
} from '@/api/admin'
import { fetchAdminAnnouncements, fetchAdminSiteInfo } from '@/api/admin/site'
import { fetchCurrentUserOverview } from '@/api/user'
import OverviewSectionCard from '@/components/workspace/shared/OverviewSectionCard.vue'
import OverviewStatCard from '@/components/workspace/shared/OverviewStatCard.vue'
import WorkspacePageShell from '@/components/workspace/shared/WorkspacePageShell.vue'
import WorkspacePermissionNotice from '@/components/workspace/shared/WorkspacePermissionNotice.vue'
import { useUserStore } from '@/store/user'
import { formatManagementTime } from '@/utils/profileManagement'
import { WORKSPACE_CAPABILITIES } from '@/utils/workspaceCapabilities'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const dashboard = reactive({
  articleCount: 0,
  commentCount: 0,
  momentCount: 0,
  forumPostCount: 0,
})
const siteInfo = reactive({
  name: '',
  author: '',
  keywords: '',
  description: '',
  icp: '',
})
const adminSummary = reactive({
  unreadNotificationCount: 0,
  pendingReportCount: 0,
})
const announcements = ref([])
const contributionSeries = ref([])
const categoryDistribution = ref([])
const tagDistribution = ref([])
const recentNotifications = ref([])
const moderationPosts = ref([])

const canViewDashboard = computed(() =>
  userStore.hasCapability(WORKSPACE_CAPABILITIES.DASHBOARD_VIEW),
)
const siteHealthScore = computed(() => {
  const pendingPenalty = Math.min(adminSummary.pendingReportCount * 9, 28)
  const unreadPenalty = Math.min(adminSummary.unreadNotificationCount * 4, 18)
  const announcementBonus = announcements.value.length ? 6 : 0

  return Math.max(62, Math.min(98, 100 - pendingPenalty - unreadPenalty + announcementBonus))
})
const siteHealthLabel = computed(() => {
  if (siteHealthScore.value >= 88) return '站点状态稳定，当前后台节奏良好。'
  if (siteHealthScore.value >= 76) return '整体运行平稳，建议继续关注待处理事项。'
  return '待处理事项偏多，建议优先清理举报和通知队列。'
})
const adminPulseList = computed(() => [
  {
    label: '审核队列',
    value: `${adminSummary.pendingReportCount || 0} 条`,
    detail: adminSummary.pendingReportCount ? '优先处理举报可显著降低后台压力。' : '当前没有积压举报。',
  },
  {
    label: '提醒同步',
    value: `${adminSummary.unreadNotificationCount || 0} 条`,
    detail: adminSummary.unreadNotificationCount ? '建议及时查看站内提醒，避免遗漏关键更新。' : '通知提醒已基本清空。',
  },
  {
    label: '站点公告',
    value: `${announcements.value.length} 条`,
    detail: announcements.value.length ? '论坛展示区已有公告内容。' : '可以补充最新公告提升信息透明度。',
  },
])
const managementHighlights = computed(() => [
  {
    kicker: '审核优先级',
    title: adminSummary.pendingReportCount
      ? `${adminSummary.pendingReportCount} 条举报待处理`
      : '当前审核队列比较清爽',
    description: '把最需要回应的社区问题前置处理，能让后台管理节奏更平稳。',
  },
  {
    kicker: '内容覆盖',
    title: `${dashboard.articleCount + dashboard.momentCount} 条内容沉淀`,
    description: `文章 ${dashboard.articleCount} 篇，动态 ${dashboard.momentCount} 条，当前内容库已经形成稳定规模。`,
  },
  {
    kicker: '站点公告',
    title: announcements.value.length ? `当前展示 ${announcements.value.length} 条公告` : '建议补充站内公告',
    description: '公告区是连接社区和站点运营信息的重要入口，适合放置新活动或规则提示。',
  },
])
const controlDeck = computed(() => [
  {
    kicker: '审核中心',
    title: adminSummary.pendingReportCount
      ? `${adminSummary.pendingReportCount} 条举报等待处理`
      : '举报审核队列已基本清空',
    description: adminSummary.pendingReportCount
      ? '优先处理举报可以快速降低后台压力，也能更快给社区反馈结果。'
      : '当前没有新的举报堆积，可以继续巡检评论和帖子内容。',
    meta: `未读通知 ${adminSummary.unreadNotificationCount || 0} 条`,
    actionLabel: '打开审核页',
    path: '/admin/reportmgmt',
  },
  {
    kicker: '内容库',
    title: `${dashboard.articleCount + dashboard.momentCount + dashboard.forumPostCount} 条内容沉淀`,
    description: '文章、动态和帖子已经形成统一的内容库，可以从这里继续进入维护与发布流程。',
    meta: `文章 ${dashboard.articleCount} / 动态 ${dashboard.momentCount}`,
    actionLabel: '管理文章',
    path: '/admin/articlemgmt',
  },
  {
    kicker: '站点公告',
    title: announcements.value.length ? `当前展示 ${announcements.value.length} 条公告` : '公告区等待更新',
    description: announcements.value.length
      ? '当前论坛页已有公告内容，适合继续更新最新活动、规则与站点说明。'
      : '现在可以补充一条新的公告，用来同步最新活动或规则变化。',
    meta: siteInfo.name || 'Dream Studio',
    actionLabel: '前往站点设置',
    path: '/admin/site',
  },
])

const normalizeMetricValue = (value) => Number(value || 0)

const resolveDistributionLabel = (item, fallback) => {
  return item?.name || item?.categoryName || item?.tagName || item?.label || item?.title || fallback
}

const resolveDistributionValue = (item) => {
  return Number(item?.count ?? item?.articleCount ?? item?.value ?? item?.total ?? 0)
}

const normalizeDistribution = (list = [], fallbackPrefix = '项目') => {
  const items = Array.isArray(list) ? list : []
  const maxValue = Math.max(...items.map((item) => resolveDistributionValue(item)), 0)

  return items.slice(0, 6).map((item, index) => {
    const value = resolveDistributionValue(item)
    return {
      label: resolveDistributionLabel(item, `${fallbackPrefix} ${index + 1}`),
      value,
      width: maxValue > 0 ? Math.max((value / maxValue) * 100, 8) : 0,
    }
  })
}

const normalizeContributionSeries = (list = []) => {
  const items = Array.isArray(list) ? list.slice(-10) : []
  const maxValue = Math.max(...items.map((item) => resolveDistributionValue(item)), 0)

  return items.map((item, index) => {
    const value = resolveDistributionValue(item)
    const rawLabel = String(item?.date || item?.label || item?.name || `#${index + 1}`)

    return {
      label: rawLabel.length > 5 ? rawLabel.slice(-5) : rawLabel,
      value,
      height: maxValue > 0 ? Math.max((value / maxValue) * 100, 12) : 0,
    }
  })
}

const loadOverview = async () => {
  if (!canViewDashboard.value) return

  loading.value = true

  try {
    const [
      dashboardResponse,
      contributionResponse,
      categoryResponse,
      tagResponse,
      siteResponse,
      announcementResponse,
      userOverviewResponse,
    ] = await Promise.all([
      fetchAdminDashboardStatistics(),
      fetchAdminContributionStatistics(),
      fetchAdminCategoryStatistics(),
      fetchAdminTagStatistics(),
      fetchAdminSiteInfo(),
      fetchAdminAnnouncements(),
      fetchCurrentUserOverview(),
    ])

    dashboard.articleCount = normalizeMetricValue(dashboardResponse?.articleCount)
    dashboard.commentCount = normalizeMetricValue(dashboardResponse?.commentCount)
    dashboard.momentCount = normalizeMetricValue(dashboardResponse?.momentCount)
    dashboard.forumPostCount = normalizeMetricValue(dashboardResponse?.forumPostCount)

    siteInfo.name = siteResponse?.name || ''
    siteInfo.author = siteResponse?.author || ''
    siteInfo.keywords = siteResponse?.keywords || ''
    siteInfo.description = siteResponse?.description || ''
    siteInfo.icp = siteResponse?.icp || ''

    adminSummary.unreadNotificationCount = normalizeMetricValue(userOverviewResponse?.unreadNotificationCount)
    adminSummary.pendingReportCount = normalizeMetricValue(userOverviewResponse?.pendingReportCount)
    announcements.value = Array.isArray(announcementResponse) ? announcementResponse : []
    contributionSeries.value = normalizeContributionSeries(contributionResponse)
    categoryDistribution.value = normalizeDistribution(categoryResponse, '分类')
    tagDistribution.value = normalizeDistribution(tagResponse, '标签')
    recentNotifications.value = Array.isArray(userOverviewResponse?.notifications)
      ? userOverviewResponse.notifications.slice(0, 5)
      : []
    moderationPosts.value = Array.isArray(userOverviewResponse?.moderationPosts)
      ? userOverviewResponse.moderationPosts.slice(0, 5)
      : []
  } catch (error) {
    console.error('加载管理概览失败', error)
    ElMessage.error(error.message || '加载管理概览失败，请稍后重试。')
  } finally {
    loading.value = false
  }
}

const formatAnnouncementCount = (count) => `站内公告 ${count} 条`
const goTo = (path) => router.push(path)

const goToForumPost = (id) => {
  if (!id) return
  router.push(`/forum/${id}`)
}

onMounted(() => {
  loadOverview()
})
</script>

<style scoped>
.overview-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(280px, 360px);
  gap: 18px;
}

.hero-main,
.hero-side,
.insight-card,
.control-card {
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
    radial-gradient(circle at top right, rgba(45, 212, 191, 0.18), transparent 34%),
    linear-gradient(135deg, rgba(240, 253, 250, 0.96), rgba(255, 255, 255, 0.92));
}

.hero-kicker,
.hero-side-kicker,
.insight-card span,
.control-card span {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.68);
  color: #0f766e;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.overview-hero h3,
.hero-side strong,
.insight-card strong,
.control-card strong,
.distribution-meta strong,
.site-info-item strong {
  margin: 0;
  color: #134e4a;
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
.control-card p,
.task-item p,
.list-item-copy p {
  margin: 12px 0 0;
  color: #4b5563;
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

.hero-side {
  padding: 22px;
  background:
    radial-gradient(circle at top right, rgba(20, 184, 166, 0.12), transparent 40%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(243, 252, 251, 0.88));
}

.hero-side strong {
  display: block;
  margin-top: 18px;
  font-size: clamp(34px, 5vw, 48px);
  line-height: 0.96;
}

.progress-track,
.distribution-track {
  width: 100%;
  border-radius: 999px;
  overflow: hidden;
}

.progress-track {
  height: 10px;
  margin-top: 18px;
  background: rgba(15, 118, 110, 0.1);
}

.progress-track i,
.distribution-track i {
  display: block;
  border-radius: inherit;
}

.progress-track i {
  height: 100%;
  background: linear-gradient(90deg, #14b8a6, #0f766e);
}

.hero-side-list,
.task-list,
.distribution-list,
.list-stack,
.site-info-grid,
.insight-grid,
.stats-grid,
.overview-grid,
.control-grid {
  display: grid;
  gap: 16px;
  margin-top: 18px;
}

.hero-side-item,
.task-item,
.list-item,
.list-item-button,
.site-info-item {
  border-radius: 20px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  background: rgba(255, 255, 255, 0.78);
}

.hero-side-item {
  padding: 14px;
}

.hero-side-item span,
.distribution-meta span,
.site-info-item span,
.list-item span,
.list-item-button span,
.bar-item span,
.bar-item small {
  color: #64748b;
}

.hero-side-item strong {
  display: block;
  margin-top: 10px;
  font-size: 16px;
  line-height: 1.45;
}

.hero-side-item p {
  margin-top: 8px;
}

.stats-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.insight-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.control-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.control-card {
  padding: 20px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(240, 253, 250, 0.9)),
    rgba(255, 255, 255, 0.94);
}

.control-card strong {
  display: block;
  margin-top: 18px;
  font-size: 24px;
  line-height: 1.24;
}

.control-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 20px;
}

.control-card-footer small {
  color: #64748b;
}

.insight-card {
  padding: 20px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(240, 253, 250, 0.9)),
    rgba(255, 255, 255, 0.94);
}

.insight-card strong {
  display: block;
  margin-top: 18px;
  font-size: 24px;
  line-height: 1.24;
}

.overview-grid-two,
.site-info-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.bar-grid {
  display: grid;
  grid-template-columns: repeat(10, minmax(0, 1fr));
  gap: 10px;
  align-items: end;
}

.bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.bar-track {
  display: flex;
  align-items: end;
  width: 100%;
  height: 150px;
  padding: 6px;
  border-radius: 16px;
  background: rgba(15, 118, 110, 0.08);
}

.bar-track i {
  display: block;
  width: 100%;
  border-radius: 10px;
  background: linear-gradient(180deg, #2dd4bf, #0f766e);
}

.task-item,
.list-item,
.list-item-button,
.site-info-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
}

.list-item-button {
  width: 100%;
  text-align: left;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.list-item-button:hover {
  transform: translateY(-2px);
  border-color: rgba(20, 184, 166, 0.28);
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.08);
}

.list-item-copy {
  min-width: 0;
}

.list-item-copy strong,
.task-item strong {
  display: block;
  color: #0f172a;
}

.distribution-item {
  display: grid;
  gap: 8px;
}

.distribution-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.distribution-track {
  height: 10px;
  background: rgba(15, 118, 110, 0.08);
}

.distribution-track i {
  height: 100%;
  background: linear-gradient(90deg, #14b8a6, #0f766e);
}

.site-info-item {
  flex-direction: column;
}

@media (max-width: 1180px) {
  .overview-hero,
  .insight-grid,
  .stats-grid,
  .control-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .bar-grid {
    grid-template-columns: repeat(5, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .overview-hero,
  .overview-grid-two,
  .site-info-grid,
  .insight-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .hero-main,
  .hero-side,
  .insight-card,
  .control-card,
  .task-item,
  .list-item,
  .list-item-button,
  .site-info-item {
    padding: 18px;
  }

  .task-item,
  .list-item,
  .list-item-button {
    flex-direction: column;
  }

  .stats-grid,
  .bar-grid,
  .control-grid {
    grid-template-columns: 1fr;
  }

  .hero-actions {
    width: 100%;
    align-items: stretch;
  }
}
</style>
