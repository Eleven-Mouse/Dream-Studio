import request from '@/utils/request'

export function fetchHeadlines(params) {
  return request({
    url: '/headlines',
    method: 'get',
    params,
  })
}

export function toggleHeadlineLike(articleId, like = true) {
  return request({
    url: `/headlines/${articleId}/like`,
    method: 'post',
    data: { like },
  })
}
