import request from '@/utils/request'

const uploadBaseUrl = import.meta.env.VITE_APP_UPLOAD_URL || ''

export function uploadResourceFile(formData, config = {}) {
  const { headers = {}, ...restConfig } = config

  return request({
    ...restConfig,
    baseURL: '',
    url: '/admin/upload/images',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
      ...headers,
    },
  })
}

export function deleteResourceFile(url) {
  return request({
    baseURL: '',
    url: '/admin/upload/images',
    method: 'delete',
    params: { url },
  })
}

export function resolveUploadedResourceUrl(path) {
  if (!path) return ''
  return path.startsWith('http') ? path : `${uploadBaseUrl}${path}`
}
