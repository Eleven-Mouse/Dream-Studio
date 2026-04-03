<template>
  <div class="headlines-page">
    <section class="hero-panel">
      <div class="hero-copy">
        <p class="hero-eyebrow">专题</p>
        <h1 class="hero-title">新闻头条专栏</h1>
        <p class="hero-subtitle">
          按发布时间倒序浏览最新文章，支持快速阅读、点赞互动和就地评论。
        </p>
      </div>

      <div class="hero-metrics">
        <article class="metric-card">
          <span class="metric-label">总内容</span>
          <strong class="metric-value">{{ pagination.total }}</strong>
        </article>
        <article class="metric-card">
          <span class="metric-label">当前页</span>
          <strong class="metric-value">{{ pagination.page }}</strong>
        </article>
        <article class="metric-card">
          <span class="metric-label">页容量</span>
          <strong class="metric-value">{{ pagination.pageSize }}</strong>
        </article>
      </div>
    </section>

    <el-alert
      v-if="error"
      class="state-alert"
      type="error"
      :closable="false"
      :title="error"
      show-icon
    />

    <section v-if="loading && !headlines.length" class="skeleton-shell" aria-label="loading headlines">
      <el-skeleton v-for="index in 3" :key="index" animated>
        <template #template>
          <div class="skeleton-card">
            <el-skeleton-item variant="text" style="width: 90px; height: 16px" />
            <el-skeleton-item variant="h3" style="width: 58%; height: 32px; margin-top: 18px" />
            <el-skeleton-item variant="text" style="width: 100%; margin-top: 12px" />
            <el-skeleton-item variant="text" style="width: 94%" />
            <el-skeleton-item variant="image" style="width: 100%; height: 220px; margin-top: 18px" />
          </div>
        </template>
      </el-skeleton>
    </section>

    <template v-else>
      <article
        v-if="featuredHeadline"
        class="featured-headline"
        @click="openArticle(featuredHeadline.id)"
      >
        <div class="featured-overlay"></div>
        <img
          v-if="featuredHeadline.coverImage"
          class="featured-cover"
          :src="resolveCover(featuredHeadline.coverImage)"
          :alt="featuredHeadline.title"
        />
        <div v-else class="featured-placeholder"></div>

        <div class="featured-content">
          <span class="featured-badge">精选专题</span>
          <h2 class="featured-title">{{ featuredHeadline.title }}</h2>
          <p class="featured-summary">
            {{ featuredHeadline.summary || '这篇内容还没有摘要，点击进入阅读全文。' }}
          </p>

          <div class="featured-meta">
            <span>{{ featuredHeadline.authorNickname || featuredHeadline.authorUsername || '匿名作者' }}</span>
            <span>{{ formatTime(featuredHeadline.publishTime || featuredHeadline.createTime) }}</span>
            <span>阅读 {{ featuredHeadline.viewCount || 0 }}</span>
          </div>
        </div>
      </article>

      <section v-if="secondaryHeadlines.length" class="headline-list">
        <el-card
          v-for="item in secondaryHeadlines"
          :key="item.id"
          class="headline-card"
          shadow="never"
        >
          <div class="headline-card__header">
            <div class="headline-author">
              <el-avatar :size="44" :src="item.authorAvatar">{{ authorInitial(item) }}</el-avatar>
              <div class="headline-author__meta">
                <strong>{{ item.authorNickname || item.authorUsername || '匿名用户' }}</strong>
                <p>{{ formatTime(item.publishTime || item.createTime) }}</p>
              </div>
            </div>
            <el-tag type="info" effect="plain" round>发布中</el-tag>
          </div>

          <div class="headline-card__body">
            <div class="headline-copy">
              <h3 class="headline-title" @click="openArticle(item.id)">{{ item.title }}</h3>
              <p class="headline-summary">
                {{ item.summary || '这篇内容还没有摘要，点击查看完整正文。' }}
              </p>
            </div>

            <button
              v-if="item.coverImage"
              type="button"
              class="headline-cover-button"
              @click="openArticle(item.id)"
            >
              <img :src="resolveCover(item.coverImage)" :alt="item.title" class="headline-cover" />
            </button>
          </div>

          <div class="headline-card__footer">
            <div class="headline-stats">
              <span>阅读 {{ item.viewCount || 0 }}</span>
              <span>评论 {{ commentCount(item) }}</span>
              <span>点赞 {{ item.likeCount || 0 }}</span>
            </div>

            <div class="headline-actions">
              <el-button text type="primary" @click="openArticle(item.id)">阅读全文</el-button>
              <el-button text :icon="ChatDotRound" @click="openComments(item)">评论</el-button>
              <el-button
                text
                :type="item.liked ? 'danger' : 'default'"
                :icon="item.liked ? StarFilled : Star"
                :loading="Boolean(item._likeLoading)"
                @click="handleLike(item)"
              >
                {{ item.likeCount || 0 }}
              </el-button>
            </div>
          </div>
        </el-card>
      </section>

      <el-empty
        v-if="!featuredHeadline && !error"
        class="empty-state"
        description="这里还没有专题内容，稍后再来看看。"
      />
    </template>

    <div v-if="pagination.total > pagination.pageSize" class="pagination-shell">
      <el-pagination
        layout="prev, pager, next"
        :total="pagination.total"
        :page-size="pagination.pageSize"
        :current-page="pagination.page"
        @current-change="handlePageChange"
      />
    </div>

    <el-drawer
      v-model="commentDrawerVisible"
      size="min(720px, 100%)"
      :title="drawerTitle"
      destroy-on-close
    >
      <CommentsCard
        v-if="activeArticleId"
        :blog-id="activeArticleId"
        :require-login="true"
        @count-changed="updateCommentCount(activeArticleId, $event)"
      />
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Star, StarFilled } from '@element-plus/icons-vue'
import { fetchHeadlines, toggleHeadlineLike } from '@/api/headline'
import CommentsCard from '@/components/CommentsCard.vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const headlines = ref([])
const loading = ref(false)
const error = ref('')
const uploadBaseUrl = (import.meta.env.VITE_APP_UPLOAD_URL || '').trim()

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

const commentCache = reactive(new Map())
const commentDrawerVisible = ref(false)
const activeArticleId = ref(null)
const activeArticleTitle = ref('')

const isLoggedIn = computed(() => userStore.isLoggedIn)
const drawerTitle = computed(() => (activeArticleTitle.value ? `${activeArticleTitle.value} · 评论区` : '评论区'))
const featuredHeadline = computed(() => headlines.value[0] || null)
const secondaryHeadlines = computed(() => headlines.value.slice(1))

const parsePage = (value) => {
  const parsed = Number.parseInt(value, 10)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 1
}

const getHeadlines = async (page = 1) => {
  loading.value = true
  error.value = ''

  try {
    const response = await fetchHeadlines({ page, pageSize: pagination.pageSize })
    const list = Array.isArray(response?.list) ? response.list : []

    headlines.value = list.map((item) => ({
      ...item,
      _likeLoading: false,
    }))
    pagination.page = response?.page || page
    pagination.pageSize = response?.pageSize || pagination.pageSize
    pagination.total = response?.total || 0
  } catch (requestError) {
    error.value = requestError.message || '获取专题内容失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

const syncPageQuery = async (page) => {
  const nextQuery = page > 1 ? { page: String(page) } : {}
  await router.replace({ path: '/headlines', query: nextQuery })
}

const handlePageChange = async (page) => {
  if (page === pagination.page) return
  await syncPageQuery(page)
  document.querySelector('.main-scroll-container')?.scrollTo({ top: 0, behavior: 'smooth' })
}

const formatTime = (time) => {
  if (!time) return ''

  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(time))
}

const resolveCover = (cover) => {
  if (!cover) return ''
  if (cover.startsWith('http://') || cover.startsWith('https://')) return cover
  if (uploadBaseUrl) return `${uploadBaseUrl}${cover}`
  return cover.startsWith('/') ? cover : `/${cover}`
}

const openArticle = (id) => {
  router.push({ name: 'articleDetail', params: { id } })
}

const authorInitial = (item) => {
  const name = item.authorNickname || item.authorUsername || 'D'
  return name.slice(0, 1).toUpperCase()
}

const openComments = (item) => {
  activeArticleId.value = item.id
  activeArticleTitle.value = item.title || ''
  commentDrawerVisible.value = true
}

const updateCommentCount = (articleId, count) => {
  commentCache.set(articleId, count)
  const target = headlines.value.find((item) => item.id === articleId)
  if (target) target.commentCount = count
}

const commentCount = (item) => commentCache.get(item.id) ?? item.commentCount ?? 0

const handleLike = async (item) => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录后再点赞。')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }

  if (item._likeLoading) return

  item._likeLoading = true
  try {
    const response = await toggleHeadlineLike(item.id, !item.liked)
    item.likeCount = response?.likeCount ?? item.likeCount
    item.liked = Boolean(response?.liked)
  } catch (requestError) {
    ElMessage.error(requestError.message || '点赞失败，请稍后再试。')
  } finally {
    item._likeLoading = false
  }
}

onMounted(async () => {
  const initialPage = parsePage(route.query.page)
  await getHeadlines(initialPage)
})
</script>

<style scoped>
.headlines-page {
  width: 100%;
  max-width: 1120px;
  margin-top: 28px;
  padding-bottom: 40px;
}

.hero-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(280px, 0.9fr);
  gap: 20px;
  margin-bottom: 24px;
  padding: 28px;
  border-radius: 28px;
  background:
    radial-gradient(circle at top left, rgba(215, 104, 66, 0.16), transparent 42%),
    linear-gradient(135deg, rgba(255, 252, 247, 0.98), rgba(245, 239, 229, 0.96));
  border: 1px solid rgba(143, 114, 82, 0.14);
  box-shadow: 0 20px 45px rgba(76, 58, 34, 0.08);
}

.hero-copy {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.hero-eyebrow {
  margin: 0;
  color: #9d5f3d;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  font-size: 0.78rem;
  font-weight: 700;
}

.hero-title {
  margin: 0;
  color: #1f2937;
  font-size: clamp(2rem, 4vw, 3rem);
  line-height: 1.1;
}

.hero-subtitle {
  margin: 0;
  max-width: 680px;
  color: #5f6673;
  line-height: 1.8;
  font-size: 1rem;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  align-self: end;
}

.metric-card {
  padding: 16px 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(143, 114, 82, 0.12);
}

.metric-label {
  display: block;
  color: #8a6a50;
  font-size: 0.82rem;
}

.metric-value {
  display: block;
  margin-top: 8px;
  color: #243041;
  font-size: 1.55rem;
  font-weight: 700;
}

.state-alert {
  margin-bottom: 20px;
}

.skeleton-shell {
  display: grid;
  gap: 18px;
}

.skeleton-card {
  padding: 24px;
  border-radius: 24px;
  background: #fff;
  border: 1px solid #eef1f6;
}

.featured-headline {
  position: relative;
  display: grid;
  min-height: 360px;
  margin-bottom: 22px;
  overflow: hidden;
  border: 0;
  border-radius: 32px;
  cursor: pointer;
  background: #121922;
  box-shadow: 0 24px 60px rgba(14, 19, 27, 0.18);
}

.featured-cover,
.featured-placeholder,
.featured-overlay,
.featured-content {
  grid-area: 1 / 1;
}

.featured-cover,
.featured-placeholder {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.featured-placeholder {
  background:
    radial-gradient(circle at top right, rgba(250, 204, 21, 0.18), transparent 24%),
    linear-gradient(135deg, #2f3947, #111827 68%);
}

.featured-overlay {
  background:
    linear-gradient(90deg, rgba(10, 16, 25, 0.86) 0%, rgba(10, 16, 25, 0.56) 46%, rgba(10, 16, 25, 0.26) 100%);
}

.featured-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  gap: 14px;
  padding: 34px;
  color: #f8fafc;
}

.featured-badge {
  width: fit-content;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(8px);
  font-size: 0.82rem;
  letter-spacing: 0.08em;
}

.featured-title {
  margin: 0;
  max-width: 720px;
  font-size: clamp(1.8rem, 3vw, 2.8rem);
  line-height: 1.18;
}

.featured-summary {
  margin: 0;
  max-width: 720px;
  color: rgba(241, 245, 249, 0.88);
  line-height: 1.8;
}

.featured-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  color: rgba(226, 232, 240, 0.85);
  font-size: 0.95rem;
}

.headline-list {
  display: grid;
  gap: 18px;
}

.headline-card {
  border: 1px solid #edf1f5;
  border-radius: 24px;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.headline-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 36px rgba(51, 65, 85, 0.08);
}

.headline-card :deep(.el-card__body) {
  padding: 22px 24px 18px;
}

.headline-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.headline-author {
  display: flex;
  align-items: center;
  gap: 12px;
}

.headline-author__meta p {
  margin: 4px 0 0;
  color: #8f98a8;
  font-size: 0.9rem;
}

.headline-card__body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 240px;
  gap: 22px;
  margin-top: 18px;
}

.headline-title {
  margin: 0 0 10px;
  color: #1f2937;
  cursor: pointer;
  font-size: 1.4rem;
  line-height: 1.35;
}

.headline-title:hover {
  color: #b45309;
}

.headline-summary {
  margin: 0;
  color: #5f6673;
  line-height: 1.8;
}

.headline-cover-button {
  padding: 0;
  border: 0;
  border-radius: 18px;
  background: transparent;
  cursor: pointer;
  overflow: hidden;
}

.headline-cover {
  width: 100%;
  height: 180px;
  object-fit: cover;
  display: block;
}

.headline-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px dashed #e4e8ef;
}

.headline-stats,
.headline-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.headline-stats {
  color: #8b93a3;
  font-size: 0.95rem;
}

.empty-state {
  margin-top: 48px;
}

.pagination-shell {
  display: flex;
  justify-content: center;
  margin: 26px 0 0;
}

@media (max-width: 960px) {
  .hero-panel,
  .headline-card__body {
    grid-template-columns: 1fr;
  }

  .hero-metrics {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .headline-cover {
    height: 220px;
  }
}

@media (max-width: 640px) {
  .headlines-page {
    margin-top: 18px;
  }

  .hero-panel {
    padding: 22px 18px;
    border-radius: 24px;
  }

  .hero-metrics {
    grid-template-columns: 1fr;
  }

  .featured-headline {
    min-height: 320px;
    border-radius: 26px;
  }

  .featured-content,
  .headline-card :deep(.el-card__body) {
    padding: 20px;
  }

  .headline-card__header,
  .headline-card__footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .headline-cover {
    height: 200px;
  }
}
</style>
