<template>
  <div class="forum-container">
    <div class="forum-shell">
      <aside class="left-sidebar">
        <div class="panel-card side-panel-shell left-panel-shell">
          <section class="left-section">
            <div class="sidebar-title">论坛页面</div>
            <div class="left-nav-list">
              <button
                v-for="item in leftNavItems"
                :key="item.key"
                type="button"
                class="left-nav-item"
                :class="{ active: sort === item.key }"
                @click="changeSort(item.key)"
              >
                <span>{{ item.label }}</span>
                <em>{{ item.count }}</em>
              </button>
            </div>
          </section>

          <div class="left-divider"></div>

          <section class="left-section left-section-info">
            <div class="sidebar-title secondary">参与关注</div>
            <div class="left-note">
              论坛页现在专注于浏览、评论、点赞与分享，发帖入口已经迁移到个人中心的独立发帖页。
            </div>
            <el-button class="left-action-button" round @click="goToProfileManager">去发帖页</el-button>
          </section>
        </div>
      </aside>

      <main class="center-content">
        <section class="feed-toolbar panel-card">
          <div class="feed-toolbar-row">
            <div class="section-title">
              帖子列表

              <button
                v-for="tag in hotTags"
                :key="tag.name"
                type="button"
                class="topic-chip"
                @click="applyTagFilter(tag.name)"
              >
                #{{ tag.name }}
              </button>
              <button v-if="activeTag" type="button" class="clear-chip" @click="activeTag = ''">
                清除筛选
              </button>
            </div>

            <el-button plain round @click="goToProfileManager">去发帖页</el-button>
          </div>
        </section>

        <div v-if="loading" class="loading-tip">正在加载帖子...</div>
        <div v-else-if="error" class="error-tip">{{ error }}</div>
        <div v-else-if="filteredPosts.length" class="post-list">
          <ForumPostCard v-for="post in filteredPosts" :key="post.id" :post="post" />
        </div>
        <el-empty v-else description="还没有帖子，去个人中心发帖页发布第一篇讨论">
          <el-button type="primary" round @click="goToProfileManager">前往发帖页</el-button>
        </el-empty>
      </main>

      <aside class="right-sidebar">
        <div class="panel-card side-panel-shell right-panel-shell">
          <div class="side-panel-scroll">
            <section class="sidebar-card side-inner-card">
              <div class="panel-title">热门帖子</div>
              <div v-if="hotPosts.length" class="hot-posts-list">
                <router-link
                  v-for="post in hotPosts"
                  :key="post.id"
                  :to="`/forum/${post.id}`"
                  class="hot-post-link"
                >
                  <span class="hot-post-title">{{ post.title }}</span>
                  <small>{{ post.commentCount || 0 }} 评论</small>
                </router-link>
              </div>
              <div v-else class="empty-tip">暂无热门帖子</div>
            </section>

            <section class="sidebar-card side-inner-card">
              <div class="panel-title">热门标签</div>
              <div v-if="hotTags.length" class="hot-tags-list">
                <button
                  v-for="tag in hotTags"
                  :key="tag.name"
                  type="button"
                  class="hot-tag-item"
                  @click="applyTagFilter(tag.name)"
                >
                  <span>#{{ tag.name }}</span>
                  <em>{{ tag.count }}</em>
                </button>
              </div>
              <div v-else class="empty-tip">等待第一批内容出现</div>
            </section>

            <section class="sidebar-card side-inner-card">
              <div class="panel-title">本期公告</div>
              <ul v-if="announcements.length" class="bullet-list">
                <li v-for="item in announcements" :key="item.id">{{ item.title }}：{{ item.content }}</li>
              </ul>
              <div v-else class="empty-tip">暂无公告</div>
            </section>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import ForumPostCard from '@/components/forum/ForumPostCard.vue'
import { fetchForumPosts } from '@/api/forum'
import { fetchSiteAnnouncements } from '@/api/site'

const router = useRouter()

const sort = ref('latest')
const activeTag = ref('')
const posts = ref([])
const announcements = ref([])
const loading = ref(false)
const error = ref('')
const featuredCount = computed(() => posts.value.filter((item) => item.isFeatured).length)

const extractTags = (post) => {
  const source = `${post?.title || ''} ${post?.summary || ''} ${post?.content || ''}`
  const matches = source.match(/[A-Za-z][A-Za-z0-9+#.-]{1,18}/g) || []
  return [...new Set(matches.map((item) => item.toLowerCase()))].slice(0, 6)
}

const hotPosts = computed(() => {
  return [...posts.value]
    .sort(
      (a, b) =>
        (b.commentCount || 0) + (b.viewCount || 0) - ((a.commentCount || 0) + (a.viewCount || 0)),
    )
    .slice(0, 5)
})

const hotTags = computed(() => {
  const counter = new Map()
  posts.value.forEach((post) => {
    extractTags(post).forEach((tag) => {
      counter.set(tag, (counter.get(tag) || 0) + 1)
    })
  })
  return [...counter.entries()]
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 8)
})

const filteredPosts = computed(() => {
  if (!activeTag.value) return posts.value
  return posts.value.filter((post) => extractTags(post).includes(activeTag.value))
})

const leftNavItems = computed(() => [
  { key: 'latest', label: '最新帖子', count: posts.value.length },
  { key: 'hot', label: '热门帖子', count: hotPosts.value.length },
  { key: 'featured', label: '精选文章', count: featuredCount.value },
])

const loadPosts = async () => {
  loading.value = true
  error.value = ''
  try {
    const response = await fetchForumPosts({ sort: sort.value, page: 1, size: 1000 })
    posts.value = response?.data || []
  } catch (err) {
    error.value = '获取论坛帖子失败，请稍后重试。'
    console.error(err)
  } finally {
    loading.value = false
  }
}

const loadAnnouncements = async () => {
  try {
    const response = await fetchSiteAnnouncements()
    announcements.value = Array.isArray(response) ? response.slice(0, 5) : []
  } catch (err) {
    console.error('获取站点公告失败', err)
    announcements.value = []
  }
}

const changeSort = (nextSort) => {
  sort.value = nextSort
  activeTag.value = ''
  loadPosts()
}

const applyTagFilter = (tagName) => {
  activeTag.value = tagName
}

const goToProfileManager = () => {
  router.push('/profile/forum-publish')
}

onMounted(() => {
  loadPosts()
  loadAnnouncements()
})
</script>

<style scoped>
.forum-container {
  width: min(1380px, calc(100vw - 60px));
  margin-top: 60px;
  margin-right: auto;
  animation: fadeIn 0.5s ease-out 0.3s forwards;
  opacity: 0;
  align-self: flex-start;
  --forum-left-width: 276px;
  --forum-right-width: 280px;
  --forum-gap: 18px;
  --forum-top-offset: 60px;
  --forum-primary-height: 300px;
}

.forum-shell {
  display: grid;
  grid-template-columns: var(--forum-left-width) minmax(0, 1fr) var(--forum-right-width);
  gap: var(--forum-gap);
  align-items: start;
}

.panel-card {
  background: var(--card-bg-color, #fff);
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 20px;
  box-shadow: none;
}

.right-panel-shell {
  position: sticky;
  top: var(--forum-top-offset);
  height: var(--forum-primary-height);
  overflow: hidden;
}

.left-panel-shell {
  position: sticky;
  top: var(--forum-top-offset);
  padding: 28px 20px 24px;
  border-radius: 26px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(255, 255, 255, 0.9) 100%);
  box-shadow: 0 18px 40px rgba(148, 163, 184, 0.12);
  max-height: calc(100vh - var(--forum-top-offset) - 24px);
  overflow-y: auto;
}

.right-panel-shell:hover {
  overflow-y: auto;
}

.left-panel-shell,
.right-panel-shell {
  scrollbar-width: thin;
  scrollbar-color: rgba(120, 120, 120, 0.35) transparent;
}

.left-panel-shell::-webkit-scrollbar,
.right-panel-shell::-webkit-scrollbar {
  width: 6px;
}

.left-panel-shell::-webkit-scrollbar-thumb,
.right-panel-shell::-webkit-scrollbar-thumb {
  background: rgba(120, 120, 120, 0.35);
  border-radius: 999px;
}

.sidebar-title {
  margin-bottom: 18px;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.2;
  color: #2f3747;
}

.sidebar-title.secondary {
  margin-top: 0;
  margin-bottom: 16px;
  font-size: 16px;
  color: #3e4553;
}

.left-section + .left-section {
  margin-top: 20px;
}

.left-nav-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.left-nav-item {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  border: 1px solid transparent;
  border-radius: 18px;
  background: transparent;
  color: #5e6572;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
}

.left-nav-item.active,
.left-nav-item:hover {
  background: #f0f0f0;
  border-color: rgba(15, 23, 42, 0.04);
  color: #2f3747;
}

.left-nav-item em {
  font-style: normal;
  color: #9aa1ac;
  font-size: 28px;
  line-height: 1;
  font-weight: 300;
}

.left-nav-item.active em,
.left-nav-item:hover em {
  color: #a0a6b1;
}

.left-divider {
  height: 1px;
  background: rgba(15, 23, 42, 0.08);
  margin: 26px 0 22px;
}

.left-note {
  line-height: 1.85;
  color: #818895;
  font-size: 15px;
}

.left-action-button {
  margin-top: 16px;
}

.center-content {
  min-width: 0;
  width: 85%;
  justify-self: center;
}

.feed-toolbar,
.sidebar-card {
  margin-bottom: 16px;
}

.right-panel-shell {
  padding: 18px 16px;
}

.side-panel-scroll {
  display: flex;
  flex-direction: column;
}

.side-inner-card {
  padding: 0;
}

.feed-toolbar-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.feed-toolbar {
  padding: 16px 18px;
}

.section-title,
.panel-title {
  color: var(--app-text-color);
  font-weight: 600;
}

.section-title {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.panel-title {
  font-size: 18px;
  margin-bottom: 14px;
}

.topic-chip,
.clear-chip,
.hot-tag-item {
  border: 0;
  background: rgba(0, 0, 0, 0.05);
  color: #666;
  border-radius: 999px;
  padding: 7px 12px;
  cursor: pointer;
}

.clear-chip {
  background: rgba(0, 0, 0, 0.08);
}

.hot-posts-list,
.hot-tags-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.hot-post-link,
.hot-tag-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  text-decoration: none;
  color: #666;
  padding: 12px 14px;
  border-radius: 14px;
}

.hot-post-link {
  background: rgba(0, 0, 0, 0.03);
}

.hot-post-link:hover,
.hot-tag-item:hover,
.topic-chip:hover,
.clear-chip:hover {
  color: var(--app-text-color);
  background: rgba(0, 0, 0, 0.07);
}

.hot-post-title {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-post-link small,
.hot-tag-item em {
  font-style: normal;
  color: #999;
}

.bullet-list {
  margin: 0;
  padding-left: 18px;
  color: #7d7d7d;
  line-height: 1.8;
}

.empty-tip {
  color: #999;
  font-size: 14px;
}

.loading-tip,
.error-tip {
  padding: 20px 0;
  text-align: center;
  color: #888;
}

.error-tip {
  color: #f56c6c;
}

@media screen and (max-width: 1200px) {
  .forum-shell {
    grid-template-columns: 1fr;
  }

  .left-sidebar,
  .right-sidebar {
    min-width: unset;
  }

  .left-panel-shell,
  .right-panel-shell {
    position: static;
    height: auto;
    overflow: visible;
  }

  .left-panel-shell {
    max-height: none;
  }

  .left-sidebar {
    margin-bottom: 16px;
  }

  .right-sidebar {
    margin-top: 16px;
    height: 700px;
  }

  .center-content {
    width: 100%;
  }

}

@media screen and (max-width: 900px) {
  .forum-container {
    width: calc(100vw - 24px);
  }

  .left-panel-shell {
    padding: 22px 18px;
    border-radius: 22px;
  }

  .feed-toolbar-row {
    flex-direction: column;
    align-items: flex-start;
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
