import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const RETRY_CONFIG = {
  maxRetries: 3,
  retryDelay: 1000,
  retryStatusCodes: [408, 500, 502, 503, 504]
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true
})

request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

let refreshPromise = null

const refreshAccessToken = async () => {
  if (refreshPromise) {
    return refreshPromise
  }

  const storedRefreshToken = localStorage.getItem('refreshToken')
  if (!storedRefreshToken) {
    return null
  }

  refreshPromise = (async () => {
    try {
      const res = await axios.post(`${API_BASE_URL}/auth/refresh`, { refreshToken: storedRefreshToken })
      if (res.data.code === 200 && res.data.data?.token) {
        localStorage.setItem('token', res.data.data.token)
        if (res.data.data.refreshToken) {
          localStorage.setItem('refreshToken', res.data.data.refreshToken)
        }
        return res.data.data.token
      }
      return null
    } catch (e) {
      console.error('Token 刷新失败:', e)
      return null
    } finally {
      refreshPromise = null
    }
  })()

  return refreshPromise
}

request.interceptors.response.use(
  (response) => {
    const res = response.data

    if (res && typeof res.code === 'number' && res.code !== 200) {
      const url = response.config.url
      if (url.includes('/auth/login') || url.includes('/auth/register')) {
        return Promise.reject({ response })
      }

      if (res.code === 401) {
        handleUnauthorized()
        return Promise.reject(new Error(res.message || '未授权'))
      }

      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }

    return res
  },

  async (error) => {
    const { config, response } = error

    if (config) {
      config.__retryCount = config.__retryCount || 0
    }

    if (response) {
      const status = response.status

      if (config && RETRY_CONFIG.retryStatusCodes.includes(status)
          && config.__retryCount < RETRY_CONFIG.maxRetries) {
        config.__retryCount++
        await new Promise(resolve => setTimeout(resolve,
          RETRY_CONFIG.retryDelay * config.__retryCount))
        return request(config)
      }

      if (status === 401) {
        if (config.url.includes('/auth/login')) {
          return Promise.reject(error)
        }

        if (config.__isRetry) {
          handleUnauthorized()
          return Promise.reject(error)
        }

        const newToken = await refreshAccessToken()
        if (newToken) {
          config.__isRetry = true
          config.headers.Authorization = `Bearer ${newToken}`
          return request(config)
        }

        handleUnauthorized()
        return Promise.reject(error)
      }

      if (status === 403) {
        ElMessage.error('无权限访问此资源')
        return Promise.reject(error)
      }

      if (status === 429) {
        const message = response.data?.message || '请求过于频繁，请稍后再试'
        ElMessage.warning(message)
        return Promise.reject(error)
      }

      if (status === 500) {
        ElMessage.error('服务器内部错误，请稍后重试')
        return Promise.reject(error)
      }

      const message = response.data?.message || `请求失败 (${status})`
      ElMessage.error(message)
      return Promise.reject(error)

    } else {
      if (config && config.__retryCount < RETRY_CONFIG.maxRetries) {
        config.__retryCount++
        await new Promise(resolve => setTimeout(resolve,
          RETRY_CONFIG.retryDelay * config.__retryCount))
        return request(config)
      }
      ElMessage.error('网络连接失败，请检查网络后重试')
    }

    return Promise.reject(error)
  }
)

const handleUnauthorized = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('user')
  router.push('/login')
}

const cache = new Map()
const CACHE_TTL = 5 * 60 * 1000

const invalidateCache = (url) => {
  const prefix = '/' + url.replace(/^\//, '').split('/')[0]
  for (const key of cache.keys()) {
    if (key.startsWith(prefix)) cache.delete(key)
  }
}

export const get = (url, params, options = {}) => {
  const { cache: useCache = true, ...axiosOptions } = options
  const cacheKey = url + (params ? JSON.stringify(params) : '')

  if (useCache && cache.has(cacheKey)) {
    const cached = cache.get(cacheKey)
    if (Date.now() - cached.timestamp < CACHE_TTL) {
      return Promise.resolve(cached.data)
    }
    cache.delete(cacheKey)
  }

  return request({ url, method: 'get', params, ...axiosOptions }).then(data => {
    if (useCache) cache.set(cacheKey, { data, timestamp: Date.now() })
    return data
  })
}

export const post = (url, data, options = {}) => {
  invalidateCache(url)
  return request({ url, method: 'post', data, ...options })
}

export const put = (url, data, options = {}) => {
  invalidateCache(url)
  return request({ url, method: 'put', data, ...options })
}

export const del = (url, params, options = {}) => {
  invalidateCache(url)
  return request({ url, method: 'delete', params, ...options })
}

export const clearCache = () => cache.clear()

export default request