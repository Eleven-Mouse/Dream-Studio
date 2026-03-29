<template>
  <div class="headlines-page">
    <section class="hero">
      <div>
        <p class="eyebrow">专栏</p>
        <h2>新闻头条 · 最新发布</h2>
        <p class="subtitle">按时间倒序展示用户文章，支持点赞与评论</p>
      </div>
    </section>

    <el-card v-for="item in headlines" :key="item.id" class="headline-card" shadow="never">
      <div class="headline-top">
        <div class="author">
          <el-avatar :size="48" :src="item.authorAvatar">{{ authorInitial(item) }}</el-avatar>
          <div class="author-meta">
            <strong>{{ item.authorNickname || item.authorUsername || '匿名用户' }}</strong>
            <p>{{ formatTime(item.publishTime || item.createTime) }}</p>
          </div>
        </div>
        <el-tag type="info" effect="plain">发布</el-tag>
      </div>

      <h3 class="headline-title" @click="openArticle(item.id)">{{ item.title }}</h3>
      <p class="headline-summary">{{ item.summary || '暂无摘要' }}</p>

      <div v-if="item.coverImage" class="cover-shell" @click="openArticle(item.id)">
        <img :src="resolveCover(item.coverImage)" :alt="item.title" />
      </div>

      <div class="headline-actions">
        <div class="headline-stats">
          <span>阅读 {{ item.viewCount || 0 }}</span>
          <span>评论 {{ commentCount(item) }}</span>
          <span>点赞 {{ item.likeCount || 0 }}</span>
        </div>
        <div class="action-buttons">
          <el-button text type="primary" @click="openArticle(item.id)">阅读全文</el-button>
          <el-button text :icon="ChatDotRound" @click="openComments(item)">
            评论
          </el-button>
          <el-button
            text
            :type="item.liked ? 'danger' : 'default'"
            :icon="item.liked ? StarFilled : Star"
            :loading="item._likeLoading"
            @click="handleLike(item)"
          >
            {{ item.likeCount || 0 }}
          </el-button>
        </div>
      </div>
    </el-card>

    <el-empty v-if="!loading && headlines.length === 0" description="暂无数据" />

    <div class="pagination-shell" v-if="pagination.total > pagination.pageSize">
      <el-pagination
        layout="prev, pager, next"
        :total="pagination.total"
        :page-size="pagination.pageSize"
        :current-page="pagination.page"
        @current-change="handlePageChange"
      />
    </div>

    <el-drawer v-model="commentDrawerVisible" size="50%" :title="drawerTitle" destroy-on-close>
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
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Star, StarFilled } from '@element-plus/icons-vue'
import { fetchHeadlines, toggleHeadlineLike } from '@/api/headline'
import CommentsCard from '@/components/CommentsCard.vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const headlines = ref([])
const loading = ref(false)
const error = ref(null)
const uploadBaseUrl = import.meta.env.VITE_APP_UPLOAD_URL || ''

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

const commentCache = reactive(new Map())
const commentDrawerVisible = ref(false)
const activeArticleId = ref(null)
const drawerTitle = computed(() => (activeArticleId.value ? `文章评论 #${activeArticleId.value}` : '评论'))
const isLoggedIn = computed(() => userStore.isLoggedIn)

const getHeadlines = async (page = 1) => {
  loading.value = true
  error.value = null
  try {
    const res = await fetchHeadlines({ page, pageSize: pagination.pageSize })
    headlines.value = res.list || []
    pagination.page = res.page || page
    pagination.pageSize = res.pageSize || pagination.pageSize
    pagination.total = res.total || 0
  } catch (err) {
    error.value = err.message || '获取头条失败'
    console.error(err)
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page) => {
  getHeadlines(page)
  document.querySelector('.main-scroll-container')?.scrollTo({ top: 0, behavior: 'smooth' })
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const resolveCover = (cover) => {
  if (!cover) return ''
  return cover.startsWith('http') ? cover : `${uploadBaseUrl}${cover}`
}

const openArticle = (id) => router.push(`/article/${id}`)

const authorInitial = (item) => {
  const name = item.authorNickname || item.authorUsername || 'A'
  return name.slice(0, 1).toUpperCase()
}

const openComments = (item) => {
  activeArticleId.value = item.id
  commentDrawerVisible.value = true
}

const updateCommentCount = (articleId, count) => {
  commentCache.set(articleId, count)
  const target = headlines.value.find((it) => it.id === articleId)
  if (target) target.commentCount = count
}

const commentCount = (item) => commentCache.get(item.id) ?? item.commentCount ?? 0

const handleLike = async (item) => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录后再点赞')
    router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }
  if (item._likeLoading) return
  item._likeLoading = true
  try {
    const res = await toggleHeadlineLike(item.id, !item.liked)
    item.likeCount = res.likeCount
    item.liked = res.liked
  } catch (err) {
    console.error(err)
  } finally {
    item._likeLoading = false
  }
}

onMounted(() => getHeadlines())
</script>

<style scoped>
.headlines-page {
  width: 100%;
  max-width: 1080px;
  margin-top: 42px;
}
.hero {
  width: 100%;
  padding: 18px 0 26px;
}
.eyebrow {
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #6f7a8a;
  margin: 0 0 6px;
}
.hero h2 {
  margin: 0;
  font-size: 28px;
}
.subtitle {
  margin: 6px 0 0;
  color: #6f7a8a;
}
.headline-card {
  margin-bottom: 18px;
  border: 1px solid #f0f2f5;
}
.headline-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.author {
  display: flex;
  align-items: center;
  gap: 12px;
}
.author-meta p {
  margin: 2px 0 0;
  color: #9aa3b1;
  font-size: 0.9rem;
}
.headline-title {
  margin: 16px 0 8px;
  cursor: pointer;
}
.headline-summary {
  color: #5f6570;
  line-height: 1.6;
}
.cover-shell {
  margin: 12px 0;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
}
.cover-shell img {
  width: 100%;
  height: 260px;
  object-fit: cover;
}
.headline-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}
.headline-stats {
  display: flex;
  gap: 14px;
  color: #8b93a3;
}
.action-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
}
.pagination-shell {
  display: flex;
  justify-content: center;
  margin: 18px 0 36px;
}
</style>
