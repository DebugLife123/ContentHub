import axios, { AxiosError, type AxiosInstance } from 'axios'

/** localStorage 中存放 JWT 的 key */
export const TOKEN_KEY = 'contenthub_token'

const BASE_OPTIONS = { baseURL: '/api', timeout: 7000 }

interface ErrorEnvelope {
  success?: boolean
  message?: string | null
  errorCode?: string | null
}

/**
 * 把后端统一响应体里的 message 提升到 Error.message。
 *
 * <p>后端的错误分两类：业务规则拒绝返回 HTTP 200 + success=false，
 * 参数/协议错误返回 400/404/405 等状态码 + 同样的信封。
 * 若不在这里归一化，catch 里拿到的会是 axios 的
 * 「Request failed with status code 400」而不是「标题不能为空」。</p>
 *
 * <p>只改 message、不替换整个 error，这样原先读
 * {@code err.response.data.message} 的地方依然可用。</p>
 */
function normalizeError(error: AxiosError<ErrorEnvelope>) {
  const backendMessage = error.response?.data?.message
  if (backendMessage && typeof error.message === 'string') {
    error.message = backendMessage
  }

  // 401 说明 token 已失效（过期，或已在别处退出登录）。
  // 这里只清本地 token，跳转交给路由守卫处理，
  // 避免在拦截器里直接操作 router 造成循环依赖。
  if (error.response?.status === 401) {
    localStorage.removeItem(TOKEN_KEY)
  }

  return Promise.reject(error)
}

/**
 * 需要携带 JWT 的实例：请求拦截器自动附加 Authorization 头。
 * 业务接口都用它。
 */
export const instance: AxiosInstance = axios.create(BASE_OPTIONS)

instance.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

instance.interceptors.response.use((response) => response, normalizeError)

/**
 * 不挂请求拦截器的实例：登录、注册等不需要（也不应该）携带旧 token 的接口用它。
 * 仍然挂响应拦截器，让登录失败的提示也能正确显示。
 */
export const NotInterceptorInstance: AxiosInstance = axios.create(BASE_OPTIONS)

NotInterceptorInstance.interceptors.response.use((response) => response, normalizeError)

export default instance
export { instance as api }
