import { ElMessage, ElMessageBox } from 'element-plus'
import router from '@/router'

export function handleApiError(error, showRetry = true) {
  console.error('API Error:', error)
  
  let message = '网络请求失败'
  if (error.response) {
    const status = error.response.status
    switch (status) {
      case 400:
        message = '请求参数错误'
        break
      case 401:
        message = '未授权，请重新登录'
        // 401 自动跳转到登录页，清除所有登录数据
        localStorage.removeItem('token')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('user')
        router.push('/login')
        return
      case 403:
        message = '无权限访问'
        break
      case 404:
        message = '请求资源不存在'
        break
      case 500:
        message = '服务器内部错误'
        break
      default:
        message = `请求失败，状态码: ${status}`
    }
  } else if (error.message) {
    message = error.message
  }

  if (showRetry) {
    ElMessageBox.confirm(
      `${message}，是否重试？`,
      {
        title: '提示',
        confirmButtonText: '重试',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(() => {
      // 重新发送当前请求（由调用方传入 retryCallback）
      if (error.config && error.config._retryCallback) {
        error.config._retryCallback()
      } else {
        ElMessage.warning('请刷新页面后重试')
      }
    }).catch(() => {
      // 用户取消重试
    })
  } else {
    ElMessage.error(message)
  }
}

export function showError(message) {
  ElMessage.error(message)
}

export function showSuccess(message) {
  ElMessage.success(message)
}

export function showWarning(message) {
  ElMessage.warning(message)
}

export function showInfo(message) {
  ElMessage.info(message)
}