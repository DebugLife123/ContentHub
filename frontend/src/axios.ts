import axios, { AxiosError, type AxiosInstance } from 'axios'

/** localStorage 中存放 JWT 的 key */
export const TOKEN_KEY = 'contenthub_token'

const BASE_OPTIONS = { baseURL: '/api', timeout: 7000 }

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

/**
 * 401 说明 token 已失效（过期，或已在别处退出登录）。
 *
 * 这里只清掉本地 token；跳转交给路由守卫处理，
 * 避免在拦截器里直接操作 router 造成循环依赖。
 */
instance.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
    }
    return Promise.reject(error)
  }
)

/**
 * 不挂拦截器的实例：登录、注册等不需要（也不应该）携带旧 token 的接口用它。
 */
export const NotInterceptorInstance: AxiosInstance = axios.create(BASE_OPTIONS)

export default instance
export { instance as api }
