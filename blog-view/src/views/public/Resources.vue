<template>
  <div class="resources-page">
    <div class="notice-strip">
      <span class="notice-label">站点播报</span>
      <div class="notice-track">
        <span>{{ noticeText }}</span>
      </div>
    </div>

    <section class="hero-panel">
      <div class="hero-copy">
        <p class="hero-kicker">Dream Studio Resource Deck</p>
        <h1>资源中心</h1>
        <p class="hero-description">
          这里继续沿用现有后端上传接口，上传后立即拿到静态资源地址。现在页面已经补上资源分类、下载次数、上传者信息和时间排序交互，便于把上传结果组织成真正可用的资源页。
        </p>

        <div class="hero-metrics">
          <div class="metric-item">
            <strong>{{ resources.length }}</strong>
            <span>资源总数</span>
          </div>
          <div class="metric-item">
            <strong>{{ categoryCount }}</strong>
            <span>资源分类</span>
          </div>
          <div class="metric-item">
            <strong>{{ totalDownloadCount }}</strong>
            <span>累计下载</span>
          </div>
        </div>
      </div>

      <div class="hero-tools">
        <div class="search-shell">
          <el-icon class="search-icon"><Search /></el-icon>
          <input
            v-model.trim="searchKeyword"
            type="text"
            class="search-input"
            placeholder="搜索资源名、后缀、上传者或 MIME 类型..."
          />
        </div>

        <div class="hero-actions">
          <button
            type="button"
            class="upload-button"
            :disabled="uploading"
            @click="openFilePicker"
          >
            <el-icon><UploadFilled /></el-icon>
            <span>{{ uploading ? '上传处理中' : '上传资源' }}</span>
          </button>
          <input
            ref="fileInput"
            class="native-file-input"
            type="file"
            accept=".jpg,.jpeg,.png,.gif,.webp,.bmp,.svg,image/*"
            multiple
            @change="handleFileSelection"
          />
        </div>

        <p class="upload-hint">
          当前接入 <code>POST /admin/upload/images</code>，以 <code>multipart/form-data</code>
          提交 <code>file</code> 字段，后端成功后返回 <code>Result&lt;String&gt;</code>，资源地址落在
          对象存储公开地址或 <code>/images/**</code>。当前后端只接受常见图片格式，且单文件不能超过 10MB。
        </p>

        <div v-if="uploading" class="progress-card">
          <div class="progress-head">
            <span>{{ uploadStatusText || '正在上传资源...' }}</span>
            <strong>{{ uploadProgress }}%</strong>
          </div>
          <div class="progress-bar">
            <span class="progress-fill" :style="{ width: `${uploadProgress}%` }"></span>
          </div>
        </div>
      </div>
    </section>

    <section class="resource-panel">
      <div class="panel-header">
        <div class="panel-copy">
          <p class="panel-kicker">Upload Result</p>
          <h2>资源列表</h2>
          <p class="panel-description">
            当前后端已经提供真实的图片资源上传和删除接口，页面会直接调用这些接口；列表维度暂时仍然基于本地缓存记录组织，因此分类筛选、下载统计、上传者展示和时间排序会围绕这些真实上传结果展开。
          </p>
        </div>

        <div class="summary-strip">
          <div class="summary-chip">
            <span>本地缓存体积</span>
            <strong>{{ totalResourceSize }}</strong>
          </div>
          <div class="summary-chip">
            <span>最近上传</span>
            <strong>{{ latestUploadLabel }}</strong>
          </div>
          <div class="summary-chip">
            <span>当前上传者</span>
            <strong>{{ currentUploaderName }}</strong>
          </div>
        </div>
      </div>

      <div class="toolbar-grid">
        <div class="toolbar-card">
          <div class="toolbar-head">
            <span class="toolbar-label">资源分类</span>
            <small>按类型筛选当前资源</small>
          </div>
          <div class="filter-group">
            <button
              v-for="filter in resourceFilters"
              :key="filter.key"
              type="button"
              class="filter-chip"
              :class="{ active: activeFilter === filter.key }"
              @click="activeFilter = filter.key"
            >
              <span>{{ filter.label }}</span>
              <em>{{ filter.count }}</em>
            </button>
          </div>
        </div>

        <div class="toolbar-card">
          <div class="toolbar-head">
            <span class="toolbar-label">时间排序</span>
            <small>结合下载量和名称切换排序方式</small>
          </div>
          <div class="sort-group">
            <button
              v-for="option in sortOptions"
              :key="option.value"
              type="button"
              class="sort-chip"
              :class="{ active: sortMode === option.value }"
              @click="sortMode = option.value"
            >
              {{ option.label }}
            </button>
          </div>
        </div>
      </div>

      <div v-if="resourceSections.length" class="resource-sections">
        <section v-for="section in resourceSections" :key="section.key" class="resource-section">
          <div class="section-header">
            <div>
              <p class="section-kicker">Category Section</p>
              <h3>{{ section.label }}</h3>
            </div>
            <div class="section-meta">
              <span>{{ section.count }} 个资源</span>
              <span>{{ sortModeLabel }}</span>
            </div>
          </div>

          <div class="resource-grid">
            <article v-for="resource in section.items" :key="resource.id" class="resource-card">
              <div class="resource-card-head">
                <div class="resource-icon" :class="`is-${resource.categoryKey}`">
                  <el-icon>
                    <component :is="resolveFileIcon(resource.extension)" />
                  </el-icon>
                </div>

                <div class="resource-badges">
                  <span class="resource-category-tag">{{ resource.category }}</span>
                  <span class="resource-extension-tag">{{ resource.extension.toUpperCase() }}</span>
                </div>
              </div>

              <h4>{{ resource.originalName }}</h4>
              <p class="resource-path">{{ resource.relativeUrl }}</p>

              <div class="resource-meta-grid">
                <span>
                  <el-icon><Calendar /></el-icon>
                  {{ resource.uploadedAt }}
                </span>
                <span>
                  <el-icon><Download /></el-icon>
                  {{ resource.downloadCount }} 次下载
                </span>
                <span>
                  <el-icon><Clock /></el-icon>
                  {{ resource.fileSizeFormatted }}
                </span>
                <span>
                  <el-icon><User /></el-icon>
                  {{ resource.mimeType }}
                </span>
              </div>

              <div class="resource-footer">
                <div class="resource-owner">
                  <el-avatar :size="42" :src="resource.uploaderAvatar" class="owner-avatar">
                    {{ resource.uploaderInitial }}
                  </el-avatar>
                  <div>
                    <strong>{{ resource.uploaderName }}</strong>
                    <small>上传于 {{ resource.uploadedAt }}</small>
                  </div>
                </div>

                <div class="resource-actions-row">
                  <button type="button" class="ghost-action" @click="copyResourceLink(resource)">
                    <el-icon><Link /></el-icon>
                    <span>复制链接</span>
                  </button>
                  <button
                    type="button"
                    class="ghost-action danger-action"
                    :disabled="isRemovingResource(resource.id)"
                    @click="removeResource(resource)"
                  >
                    <el-icon><Delete /></el-icon>
                    <span>{{ isRemovingResource(resource.id) ? '删除中' : '删除资源' }}</span>
                  </button>
                  <button type="button" class="primary-action" @click="downloadResource(resource)">
                    <el-icon><Download /></el-icon>
                    <span>下载资源</span>
                  </button>
                </div>
              </div>
            </article>
          </div>
        </section>
      </div>

      <el-empty v-else :description="emptyDescription" class="resource-empty">
        <button
          type="button"
          class="upload-button empty-button"
          :disabled="uploading"
          @click="openFilePicker"
        >
          <el-icon><UploadFilled /></el-icon>
          <span>先上传一个资源</span>
        </button>
      </el-empty>
    </section>

    <section class="api-grid">
      <article class="api-card">
        <p class="api-kicker">上传接口</p>
        <h3><code>POST /admin/upload/images</code></h3>
        <p>页面上传动作直接调用当前后端接口，继续以接口返回值作为资源地址来源。</p>
      </article>

      <article class="api-card">
        <p class="api-kicker">资源统计</p>
        <h3>分类 + 下载 + 上传者</h3>
        <p>前端会为上传结果追加资源分类、下载次数和上传者信息，用于构建完整交互。</p>
      </article>

      <article class="api-card">
        <p class="api-kicker">排序模式</p>
        <h3>最新 / 最早 / 热门 / 名称</h3>
        <p>默认按时间倒序展示，必要时可以按下载次数或名称快速切换浏览视角。</p>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Calendar,
  Clock,
  Delete,
  Document,
  Download,
  Files,
  FolderOpened,
  Link,
  Search,
  UploadFilled,
  User,
} from '@element-plus/icons-vue'
import { fetchSiteAnnouncements } from '@/api/site'
import { deleteResourceFile, resolveUploadedResourceUrl, uploadResourceFile } from '@/api/resource'
import { useUserStore } from '@/store/user'

const LOCAL_RESOURCE_CACHE_KEY = 'dream-studio:resource-uploads'
const MAX_IMAGE_SIZE = 10 * 1024 * 1024
const SUPPORTED_IMAGE_EXTENSIONS = new Set(['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg'])

const userStore = useUserStore()
const fileInput = ref(null)
const searchKeyword = ref('')
const activeFilter = ref('all')
const sortMode = ref('latest')
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadStatusText = ref('')
const resources = ref([])
const announcements = ref([])
const removingResourceIds = ref([])

const sortOptions = [
  { value: 'latest', label: '最新上传' },
  { value: 'oldest', label: '最早上传' },
  { value: 'downloads', label: '下载最多' },
  { value: 'name', label: '名称排序' },
]

const fileCategoryMap = {
  zip: '压缩包',
  rar: '压缩包',
  '7z': '压缩包',
  tar: '压缩包',
  gz: '压缩包',
  bz2: '压缩包',
  xz: '压缩包',
  exe: '安装程序',
  msi: '安装程序',
  dmg: '安装程序',
  pkg: '安装程序',
  deb: '安装程序',
  rpm: '安装程序',
  apk: '安装程序',
  ipa: '安装程序',
  pdf: '文档资料',
  doc: '文档资料',
  docx: '文档资料',
  ppt: '文档资料',
  pptx: '文档资料',
  xls: '文档资料',
  xlsx: '文档资料',
  txt: '文档资料',
  md: '文档资料',
  markdown: '文档资料',
  jpg: '媒体资源',
  jpeg: '媒体资源',
  png: '媒体资源',
  gif: '媒体资源',
  webp: '媒体资源',
  svg: '媒体资源',
  mp4: '媒体资源',
  mp3: '媒体资源',
  wav: '媒体资源',
}

const categoryKeyMap = {
  压缩包: 'package',
  安装程序: 'installer',
  文档资料: 'document',
  媒体资源: 'media',
  通用文件: 'generic',
}

const getFileExtension = (filename = '') => {
  const lastDotIndex = filename.lastIndexOf('.')
  if (lastDotIndex < 0 || lastDotIndex === filename.length - 1) {
    return 'file'
  }
  return filename.slice(lastDotIndex + 1).toLowerCase()
}

const getCategoryLabel = (extension) => fileCategoryMap[extension] || '通用文件'

const getCategoryKey = (category) => categoryKeyMap[category] || 'generic'

const formatFileSize = (size = 0) => {
  if (!Number.isFinite(size) || size <= 0) return '0 B'

  const units = ['B', 'KB', 'MB', 'GB']
  let value = size
  let unitIndex = 0

  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024
    unitIndex += 1
  }

  return `${value >= 10 || unitIndex === 0 ? value.toFixed(0) : value.toFixed(1)} ${units[unitIndex]}`
}

const formatDateTime = (timestamp) =>
  new Date(timestamp).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })

const currentUploaderName = computed(
  () => userStore.profile?.nickname || userStore.profile?.username || 'Dream 用户',
)

const currentUploaderAvatar = computed(() => userStore.profile?.avatar || '')

const currentUploaderInitial = computed(
  () => currentUploaderName.value?.slice(0, 1)?.toUpperCase() || 'D',
)

const normalizeResourceRecord = (record) => {
  const originalName = record.originalName || record.displayName || '未命名资源'
  const extension = getFileExtension(originalName)
  const category = record.category || getCategoryLabel(extension)
  const relativeUrl = record.relativeUrl || record.filePath || record.fileUrl || ''
  const uploadedAtValue =
    Number(record.uploadedAtValue) ||
    Date.parse(record.uploadedAt || '') ||
    Date.now()
  const uploaderName = record.uploaderName || 'Dream 用户'

  return {
    id: record.id || `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    originalName,
    extension,
    category,
    categoryKey: record.categoryKey || getCategoryKey(category),
    fileSize: Number(record.fileSize) || 0,
    fileSizeFormatted: record.fileSizeFormatted || formatFileSize(Number(record.fileSize) || 0),
    relativeUrl,
    fileUrl: resolveUploadedResourceUrl(relativeUrl),
    mimeType: record.mimeType || 'application/octet-stream',
    uploadedAtValue,
    uploadedAt: formatDateTime(uploadedAtValue),
    uploaderName,
    uploaderAvatar: record.uploaderAvatar || '',
    uploaderInitial: record.uploaderInitial || uploaderName.slice(0, 1)?.toUpperCase() || 'D',
    downloadCount: Number(record.downloadCount) || 0,
  }
}

const persistResources = () => {
  localStorage.setItem(LOCAL_RESOURCE_CACHE_KEY, JSON.stringify(resources.value.slice(0, 80)))
}

const restoreResources = () => {
  try {
    const cachedValue = localStorage.getItem(LOCAL_RESOURCE_CACHE_KEY)
    if (!cachedValue) return

    const parsedValue = JSON.parse(cachedValue)
    if (!Array.isArray(parsedValue)) return

    resources.value = parsedValue.map((item) => normalizeResourceRecord(item))
  } catch (error) {
    console.error('恢复资源缓存失败', error)
    resources.value = []
  }
}

const noticeText = computed(() => {
  if (!announcements.value.length) {
    return '资源页已接入后端上传接口，当前支持分类筛选、下载统计、上传者信息和时间排序。'
  }

  return announcements.value
    .slice(0, 3)
    .map((item) => `${item.title}：${item.content}`)
    .join('    ·    ')
})

const totalResourceSize = computed(() =>
  formatFileSize(resources.value.reduce((sum, item) => sum + (Number(item.fileSize) || 0), 0)),
)

const totalDownloadCount = computed(() =>
  resources.value.reduce((sum, item) => sum + (Number(item.downloadCount) || 0), 0),
)

const categoryCount = computed(() => new Set(resources.value.map((item) => item.category)).size)

const latestUploadLabel = computed(() => {
  if (!resources.value.length) return '--'

  const latestItem = [...resources.value].sort((a, b) => b.uploadedAtValue - a.uploadedAtValue)[0]
  return latestItem?.uploadedAt || '--'
})

const resourceFilters = computed(() => {
  const counts = resources.value.reduce((accumulator, item) => {
    accumulator[item.category] = (accumulator[item.category] || 0) + 1
    return accumulator
  }, {})

  return [
    { key: 'all', label: '全部资源', count: resources.value.length },
    ...Object.entries(counts)
      .map(([key, count]) => ({ key, label: key, count }))
      .sort((a, b) => b.count - a.count || a.label.localeCompare(b.label, 'zh-CN')),
  ]
})

const filteredResources = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()

  return resources.value.filter((item) => {
    const matchesFilter = activeFilter.value === 'all' || item.category === activeFilter.value
    const searchableText = [
      item.originalName,
      item.extension,
      item.category,
      item.mimeType,
      item.relativeUrl,
      item.uploaderName,
    ]
      .join(' ')
      .toLowerCase()

    return matchesFilter && (!keyword || searchableText.includes(keyword))
  })
})

const sortedResources = computed(() => {
  const list = [...filteredResources.value]

  switch (sortMode.value) {
    case 'oldest':
      return list.sort((a, b) => a.uploadedAtValue - b.uploadedAtValue)
    case 'downloads':
      return list.sort(
        (a, b) =>
          b.downloadCount - a.downloadCount || b.uploadedAtValue - a.uploadedAtValue,
      )
    case 'name':
      return list.sort((a, b) => a.originalName.localeCompare(b.originalName, 'zh-CN'))
    case 'latest':
    default:
      return list.sort((a, b) => b.uploadedAtValue - a.uploadedAtValue)
  }
})

const sortModeLabel = computed(
  () => sortOptions.find((item) => item.value === sortMode.value)?.label || '最新上传',
)

const resourceSections = computed(() => {
  const groupedMap = new Map()

  sortedResources.value.forEach((item) => {
    if (!groupedMap.has(item.category)) {
      groupedMap.set(item.category, [])
    }
    groupedMap.get(item.category).push(item)
  })

  return [...groupedMap.entries()].map(([label, items]) => ({
    key: label,
    label,
    count: items.length,
    items,
  }))
})

const emptyDescription = computed(() => {
  if (searchKeyword.value || activeFilter.value !== 'all') {
    return '没有找到符合当前筛选条件的资源。'
  }

  return '还没有资源记录，先通过上传接口添加一个资源吧。'
})

const resolveFileIcon = (extension) => {
  const mediaExtensions = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg', 'mp4', 'mp3', 'wav']
  const packageExtensions = ['zip', 'rar', '7z', 'tar', 'gz', 'bz2', 'xz', 'exe', 'msi', 'dmg', 'pkg']

  if (mediaExtensions.includes(extension)) return FolderOpened
  if (packageExtensions.includes(extension)) return Files
  return Document
}

const openFilePicker = () => {
  if (uploading.value) return
  fileInput.value?.click()
}

const isSupportedImageFile = (file) => {
  const extension = getFileExtension(file?.name || '')
  return file?.type?.toLowerCase().startsWith('image/') || SUPPORTED_IMAGE_EXTENSIONS.has(extension)
}

const validateSelectedFile = (file) => {
  if (!isSupportedImageFile(file)) {
    ElMessage.error(`${file.name} 不是后端允许的图片资源格式`)
    return false
  }

  if (file.size > MAX_IMAGE_SIZE) {
    ElMessage.error(`${file.name} 超过 10MB，无法上传`)
    return false
  }

  return true
}

const buildResourceRecord = (file, relativeUrl) => {
  const extension = getFileExtension(file.name)
  const category = getCategoryLabel(extension)
  const uploadedAtValue = Date.now()

  return normalizeResourceRecord({
    id: `${uploadedAtValue}-${Math.random().toString(36).slice(2, 8)}`,
    originalName: file.name,
    extension,
    category,
    categoryKey: getCategoryKey(category),
    fileSize: file.size,
    fileSizeFormatted: formatFileSize(file.size),
    relativeUrl,
    mimeType: file.type || 'application/octet-stream',
    uploadedAtValue,
    uploaderName: currentUploaderName.value,
    uploaderAvatar: currentUploaderAvatar.value,
    uploaderInitial: currentUploaderInitial.value,
    downloadCount: 0,
  })
}

const uploadSingleFile = async (file, currentIndex, total) => {
  const formData = new FormData()
  formData.append('file', file)

  uploadProgress.value = 0
  uploadStatusText.value = `正在上传 ${currentIndex}/${total} · ${file.name}`

  const relativeUrl = await uploadResourceFile(formData, {
    onUploadProgress: ({ loaded, total: totalSize }) => {
      if (!totalSize) return
      uploadProgress.value = Math.min(100, Math.round((loaded / totalSize) * 100))
    },
  })

  const resourceRecord = buildResourceRecord(file, relativeUrl)
  resources.value = [resourceRecord, ...resources.value].slice(0, 80)
  persistResources()
}

const handleFileSelection = async (event) => {
  const files = Array.from(event.target?.files || []).filter((file) => validateSelectedFile(file))
  if (!files.length || uploading.value) {
    if (event.target) {
      event.target.value = ''
    }
    return
  }

  uploading.value = true
  let successCount = 0

  try {
    for (const [index, file] of files.entries()) {
      try {
        await uploadSingleFile(file, index + 1, files.length)
        successCount += 1
      } catch (error) {
        console.error('资源上传失败', error)
        ElMessage.error(`${file.name} 上传失败，请稍后重试。`)
      }
    }

    uploadProgress.value = successCount ? 100 : 0
    uploadStatusText.value = successCount
      ? `上传完成，成功处理 ${successCount}/${files.length} 个文件`
      : '本次上传未成功写入资源'

    if (successCount) {
      ElMessage.success(`资源上传完成，成功 ${successCount} 个`)
    }
  } finally {
    window.setTimeout(() => {
      uploading.value = false
      uploadProgress.value = 0
      uploadStatusText.value = ''
    }, 480)

    if (event.target) {
      event.target.value = ''
    }
  }
}

const updateResourceRecord = (resourceId, updater) => {
  resources.value = resources.value.map((item) => {
    if (item.id !== resourceId) return item
    return normalizeResourceRecord(updater(item))
  })
  persistResources()
}

const isRemovingResource = (resourceId) => removingResourceIds.value.includes(resourceId)

const copyResourceLink = async (resource) => {
  try {
    if (!navigator.clipboard?.writeText) {
      throw new Error('clipboard API unavailable')
    }

    await navigator.clipboard.writeText(resource.fileUrl)
    ElMessage.success('资源链接已复制')
  } catch (error) {
    console.error('复制资源链接失败', error)
    window.prompt('当前环境不支持自动复制，请手动复制资源链接', resource.fileUrl)
  }
}

const downloadResource = (resource) => {
  updateResourceRecord(resource.id, (item) => ({
    ...item,
    downloadCount: Number(item.downloadCount || 0) + 1,
  }))

  const anchor = document.createElement('a')
  anchor.href = resource.fileUrl
  anchor.target = '_blank'
  anchor.rel = 'noopener'
  anchor.download = resource.originalName
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
}

const removeResource = async (resource) => {
  if (isRemovingResource(resource.id)) return

  removingResourceIds.value = [...removingResourceIds.value, resource.id]

  try {
    await deleteResourceFile(resource.fileUrl || resource.relativeUrl)
    resources.value = resources.value.filter((item) => item.id !== resource.id)
    persistResources()
    ElMessage.success('资源已删除')
  } finally {
    removingResourceIds.value = removingResourceIds.value.filter((id) => id !== resource.id)
  }
}

const loadAnnouncements = async () => {
  try {
    const response = await fetchSiteAnnouncements()
    announcements.value = Array.isArray(response) ? response : []
  } catch (error) {
    console.error('获取站点公告失败', error)
    announcements.value = []
  }
}

onMounted(() => {
  restoreResources()
  loadAnnouncements()
})
</script>

<style scoped>
.resources-page {
  width: min(1420px, calc(100vw - 40px));
  margin-top: 28px;
  padding: 18px 0 56px;
  color: #f3f7fb;
  align-self: flex-start;
  animation: fadeIn 0.58s ease-out forwards;
  opacity: 0;
}

.notice-strip {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 18px;
  border-radius: 18px 18px 0 0;
  background: linear-gradient(90deg, rgba(211, 235, 255, 0.94), rgba(180, 223, 255, 0.94));
  color: #2f70d8;
  overflow: hidden;
}

.notice-label {
  flex: none;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.62);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.notice-track {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
}

.notice-track span {
  display: inline-block;
  padding-left: 100%;
  animation: marquee 18s linear infinite;
}

.hero-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(360px, 0.85fr);
  gap: 28px;
  padding: 42px 44px;
  border-radius: 0 0 32px 32px;
  background:
    radial-gradient(circle at 18% 18%, rgba(226, 233, 240, 0.72) 0%, rgba(226, 233, 240, 0.12) 32%, transparent 56%),
    radial-gradient(circle at 68% 42%, rgba(27, 72, 166, 0.32) 0%, transparent 34%),
    linear-gradient(135deg, rgba(92, 87, 130, 0.9) 0%, rgba(15, 24, 43, 0.98) 46%, rgba(59, 74, 95, 0.9) 100%);
  box-shadow: 0 28px 48px rgba(18, 26, 42, 0.18);
}

.hero-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}

.hero-kicker,
.panel-kicker,
.api-kicker,
.section-kicker {
  margin: 0 0 10px;
  font-size: 13px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.hero-kicker {
  color: rgba(174, 191, 212, 0.84);
}

.panel-kicker,
.api-kicker,
.section-kicker {
  color: #7c8da5;
}

.hero-copy h1,
.panel-copy h2,
.api-card h3,
.section-header h3 {
  margin: 0;
}

.hero-copy h1 {
  font-size: clamp(42px, 4vw, 64px);
  color: #4e5cff;
  line-height: 1.02;
}

.hero-description {
  max-width: 560px;
  margin: 18px 0 24px;
  line-height: 1.9;
  color: rgba(224, 231, 240, 0.78);
  font-size: 16px;
}

.hero-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
}

.metric-item {
  min-width: 120px;
  padding: 16px 18px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 22px;
  background: rgba(16, 24, 40, 0.38);
}

.metric-item strong {
  display: block;
  font-size: 24px;
  color: #ffffff;
}

.metric-item span {
  display: block;
  margin-top: 6px;
  color: rgba(193, 204, 217, 0.78);
  font-size: 13px;
}

.hero-tools {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 18px;
}

.search-shell {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 70px;
  padding: 0 22px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 24px;
  background: rgba(20, 24, 32, 0.96);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.search-icon {
  color: rgba(205, 213, 223, 0.72);
  font-size: 18px;
}

.search-input {
  width: 100%;
  border: none;
  outline: none;
  background: transparent;
  color: #edf3fa;
  font-size: 16px;
}

.search-input::placeholder {
  color: rgba(201, 210, 220, 0.52);
}

.hero-actions {
  display: flex;
  justify-content: flex-end;
}

.upload-button,
.primary-action,
.ghost-action,
.filter-chip,
.sort-chip {
  border: none;
  cursor: pointer;
  transition:
    transform 0.24s ease,
    background 0.24s ease,
    border-color 0.24s ease,
    color 0.24s ease,
    box-shadow 0.24s ease;
}

.upload-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-width: 188px;
  min-height: 70px;
  padding: 0 28px;
  border-radius: 24px;
  background: linear-gradient(135deg, #5ab2ff 0%, #3d8df4 100%);
  color: #ffffff;
  font-size: 22px;
  font-weight: 700;
  box-shadow: 0 18px 28px rgba(61, 141, 244, 0.22);
}

.upload-button:hover:not(:disabled),
.primary-action:hover,
.ghost-action:hover,
.filter-chip:hover,
.sort-chip:hover {
  transform: translateY(-2px);
}

.upload-button:disabled,
.ghost-action:disabled,
.primary-action:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.native-file-input {
  display: none;
}

.upload-hint {
  margin: 0;
  color: rgba(186, 198, 212, 0.78);
  line-height: 1.8;
  font-size: 14px;
}

.upload-hint code,
.api-card code {
  padding: 2px 6px;
  border-radius: 8px;
  background: rgba(79, 112, 168, 0.08);
  color: inherit;
}

.progress-card {
  padding: 18px 20px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  background: rgba(14, 20, 34, 0.82);
}

.progress-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
  color: #eef6ff;
  font-size: 14px;
}

.progress-bar {
  width: 100%;
  height: 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
}

.progress-fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #6ac0ff 0%, #5580ff 100%);
}

.resource-panel {
  margin-top: 28px;
  padding: 30px;
  border-radius: 28px;
  background: linear-gradient(180deg, rgba(245, 248, 252, 0.96) 0%, rgba(238, 244, 249, 0.98) 100%);
  box-shadow: 0 24px 44px rgba(19, 25, 35, 0.12);
  color: #243448;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
  margin-bottom: 24px;
}

.panel-copy h2 {
  font-size: 34px;
  color: #223042;
}

.panel-description {
  max-width: 780px;
  margin: 12px 0 0;
  color: #67778c;
  line-height: 1.85;
}

.summary-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  min-width: 420px;
}

.summary-chip,
.toolbar-card,
.api-card,
.resource-card,
.resource-section {
  background: #ffffff;
  border: 1px solid rgba(33, 51, 75, 0.08);
  box-shadow: 0 16px 30px rgba(33, 51, 75, 0.08);
}

.summary-chip {
  padding: 16px 18px;
  border-radius: 18px;
}

.summary-chip span {
  display: block;
  color: #8795a9;
  font-size: 13px;
}

.summary-chip strong {
  display: block;
  margin-top: 8px;
  color: #243448;
  font-size: 16px;
}

.toolbar-grid {
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  gap: 18px;
}

.toolbar-card {
  padding: 18px;
  border-radius: 22px;
}

.toolbar-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 14px;
}

.toolbar-label {
  font-size: 15px;
  font-weight: 700;
  color: #26374c;
}

.toolbar-head small {
  color: #8393a9;
}

.filter-group,
.sort-group {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-chip,
.sort-chip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border: 1px solid rgba(33, 51, 75, 0.08);
  border-radius: 14px;
  background: #f4f7fb;
  color: #556579;
  font-weight: 600;
}

.filter-chip em {
  font-style: normal;
  color: #8a99ad;
}

.filter-chip.active,
.sort-chip.active {
  border-color: rgba(71, 133, 255, 0.28);
  background: linear-gradient(135deg, rgba(89, 160, 255, 0.14), rgba(93, 122, 255, 0.14));
  color: #2551c7;
}

.resource-sections {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-top: 20px;
}

.resource-section {
  padding: 22px;
  border-radius: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}

.section-header h3 {
  font-size: 24px;
  color: #243448;
}

.section-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.section-meta span {
  padding: 8px 12px;
  border-radius: 999px;
  background: #f2f6fb;
  color: #65768c;
  font-size: 13px;
}

.resource-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.resource-card {
  display: flex;
  flex-direction: column;
  min-height: 320px;
  padding: 22px;
  border-radius: 22px;
  color: #25364c;
}

.resource-card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.resource-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 18px;
  font-size: 34px;
}

.resource-icon.is-package {
  background: linear-gradient(135deg, rgba(255, 203, 107, 0.18), rgba(255, 160, 79, 0.22));
  color: #d9822b;
}

.resource-icon.is-installer {
  background: linear-gradient(135deg, rgba(118, 173, 255, 0.18), rgba(78, 111, 255, 0.18));
  color: #3a66d5;
}

.resource-icon.is-document {
  background: linear-gradient(135deg, rgba(125, 213, 172, 0.18), rgba(63, 179, 126, 0.2));
  color: #21915d;
}

.resource-icon.is-media,
.resource-icon.is-generic {
  background: linear-gradient(135deg, rgba(172, 179, 255, 0.18), rgba(111, 133, 255, 0.2));
  color: #4f68d9;
}

.resource-badges {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.resource-category-tag,
.resource-extension-tag {
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.resource-category-tag {
  background: #eef4ff;
  color: #4272d7;
}

.resource-extension-tag {
  background: #f3f5f8;
  color: #6a788e;
}

.resource-card h4 {
  margin: 18px 0 10px;
  font-size: 18px;
  line-height: 1.5;
  color: #233348;
  word-break: break-word;
}

.resource-path {
  margin: 0;
  color: #73849b;
  line-height: 1.75;
  word-break: break-all;
}

.resource-meta-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 12px;
  margin-top: 18px;
}

.resource-meta-grid span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  color: #607187;
  font-size: 13px;
}

.resource-footer {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: auto;
  padding-top: 20px;
  border-top: 1px solid rgba(33, 51, 75, 0.08);
}

.resource-owner {
  display: flex;
  align-items: center;
  gap: 12px;
}

.owner-avatar {
  flex: none;
}

.resource-owner strong,
.resource-owner small {
  display: block;
}

.resource-owner strong {
  color: #243448;
}

.resource-owner small {
  margin-top: 4px;
  color: #8393a7;
}

.resource-actions-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.ghost-action,
.primary-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex: 1;
  min-height: 44px;
  padding: 0 14px;
  border-radius: 14px;
  font-size: 14px;
  font-weight: 700;
}

.ghost-action {
  border: 1px solid rgba(33, 51, 75, 0.08);
  background: #f6f8fb;
  color: #53657c;
}

.danger-action {
  background: #fff4f2;
  color: #d15d53;
}

.primary-action {
  background: linear-gradient(135deg, #5aa9ff 0%, #4b7cff 100%);
  color: #ffffff;
}

.resource-empty {
  padding: 28px 0 8px;
}

.empty-button {
  margin-top: 12px;
  min-height: 56px;
  font-size: 16px;
}

.api-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  margin-top: 24px;
}

.api-card {
  padding: 24px;
  border-radius: 22px;
  color: #25354a;
}

.api-card h3 {
  margin-bottom: 12px;
  font-size: 24px;
  word-break: break-word;
}

.api-card p:last-child {
  margin: 0;
  color: #607086;
  line-height: 1.85;
}

@media screen and (max-width: 1360px) {
  .resource-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media screen and (max-width: 1180px) {
  .hero-panel,
  .panel-header,
  .toolbar-grid,
  .api-grid {
    grid-template-columns: 1fr;
  }

  .hero-panel {
    padding: 32px 28px;
  }

  .panel-header {
    display: block;
  }

  .summary-strip {
    min-width: 0;
    margin-top: 18px;
  }

  .resource-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media screen and (max-width: 760px) {
  .resources-page {
    width: calc(100vw - 20px);
    padding-bottom: 36px;
  }

  .notice-strip {
    border-radius: 16px 16px 0 0;
  }

  .hero-panel,
  .resource-panel,
  .resource-section {
    padding: 24px 18px;
  }

  .hero-panel {
    gap: 22px;
    border-radius: 0 0 24px 24px;
  }

  .hero-copy h1 {
    font-size: 40px;
  }

  .search-shell,
  .upload-button {
    min-height: 58px;
  }

  .upload-button {
    width: 100%;
    font-size: 18px;
  }

  .summary-strip,
  .resource-grid,
  .api-grid,
  .resource-meta-grid {
    grid-template-columns: 1fr;
  }

  .section-header,
  .toolbar-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .resource-actions-row {
    flex-direction: column;
  }
}

@keyframes marquee {
  from {
    transform: translateX(0);
  }
  to {
    transform: translateX(-100%);
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
