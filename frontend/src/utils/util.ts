import { ElMessage, ElMessageBox } from 'element-plus'

type MessageType = 'success' | 'warning' | 'info' | 'error'

// 消息提示
export function showMessage(message = '提示内容', type: MessageType = 'success', customClass = '') {
  return ElMessage({
    type,
    message,
    customClass,
  })
}

// 弹出确认框
export function showModel(content = '提示内容', type: MessageType = 'warning', title = '') {
  return ElMessageBox.confirm(content, title, {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type,
  })
}
