import qs from 'qs'
import { instance, NotInterceptorInstance } from '@/axios'

/** 登录（不需要旧 token） */
export function login(username: string, password: string) {
  return NotInterceptorInstance.post('/login', { username, password })
}

/** 获取当前登录用户信息（需要 token） */
export function getUserInfo() {
  return instance.post('/user/info')
}

/** 注册用户：后端使用 @RequestParam 接收，因此提交表单编码 */
export function registerUser(username: string, password: string, confirmPassword: string) {
  const data = qs.stringify({ username, password, confirmPassword })
  return NotInterceptorInstance.post('/register', data, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })
}
