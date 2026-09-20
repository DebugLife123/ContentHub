import axios, { type AxiosInstance } from 'axios'

/** localStorage 中存放 JWT 的 key */
export const TOKEN_KEY = 'contenthub_token'

/**
 * 需要携带 JWT 的实例：请求拦截器会自动附加 Authorization 头。
 * 内容列表、内容详情等业务接口都用它。
 */
export const instance: AxiosInstance = axios.create({ baseURL: '/api', timeout: 7000 })

instance.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * 不挂拦截器的实例：登录、注册等不需要（也不应该）携带旧 token 的接口用它。
 */
export const NotInterceptorInstance: AxiosInstance = axios.create({ baseURL: '/api', timeout: 7000 })

export default instance
export { instance as api }
