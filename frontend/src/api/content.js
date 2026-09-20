import api from '../axios'

export function listContents(contentType) {
  return api.get('/contents', { params: contentType ? { contentType } : {} })
}

export function getContent(id) {
  return api.get(`/contents/${id}`)
}
