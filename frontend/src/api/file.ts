import api from '../axios'
import type { ApiResponse } from './types'

/**
 * 文件上传。
 *
 * <p>正文里的图片/附件、内容封面、头像都走这里。返回体里已经带好
 * 可直接粘贴的 Markdown 写法，省得调用方自己拼。</p>
 */
export interface UploadedFile {
  /** 可直接使用的地址，形如 /api/files/2026/09/uuid.png */
  url: string
  /** 原始文件名 */
  name: string
  size: number
  contentType?: string | null
  /** ![name](url) */
  markdownImage: string
  /** @file: name | url */
  markdownFile: string
}

export function uploadFile(file: File) {
  const form = new FormData()
  form.append('file', file)

  return api.post<ApiResponse<UploadedFile>>('/files/upload', form, {
    // axios 自带 7 秒超时，对大文件太短，这里单独放宽到 5 分钟
    timeout: 5 * 60 * 1000,
    // 不手动设 Content-Type：交给浏览器带 boundary，写死会丢 boundary 导致后端解析失败
  })
}
