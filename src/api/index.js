import { ref } from 'vue'
export { goodsApi } from './goods'
export { locationApi } from './location'
export { orderApi } from './order'
export { stockApi } from './stock'
export { basicApi } from './basic'
export { exportApi } from './export'

const BASE_URL = '/api'
const cache = ref({})
const cacheExpireTime = 5 * 60 * 1000

export const apiCache = {
  get(key) {
    const item = cache.value[key]
    if (item && Date.now() < item.expire) {
      return item.data
    }
    return null
  },
  set(key, data) {
    cache.value[key] = {
      data,
      expire: Date.now() + cacheExpireTime
    }
  },
  clear(key) {
    if (key) {
      delete cache.value[key]
    } else {
      cache.value = {}
    }
  }
}

export const request = async (url, options = {}) => {
  const fullUrl = `${BASE_URL}${url}`
  const cacheKey = `${fullUrl}_${JSON.stringify(options.body || options.params)}`
  
  const cachedData = apiCache.get(cacheKey)
  if (cachedData && options.cache !== false) {
    return cachedData
  }

  const defaultOptions = {
    headers: {
      'Content-Type': 'application/json'
    },
    ...options
  }

  try {
    const response = await fetch(fullUrl, defaultOptions)
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const data = await response.json()
    
    if (options.cache !== false) {
      apiCache.set(cacheKey, data)
    }

    return data
  } catch (error) {
    console.error('API request error:', error)
    throw error
  }
}

export const get = (url, params = {}, options = {}) => {
  const queryString = new URLSearchParams(params).toString()
  const fullUrl = queryString ? `${url}?${queryString}` : url
  return request(fullUrl, { method: 'GET', ...options })
}

export const post = (url, body = {}, options = {}) => {
  return request(url, {
    method: 'POST',
    body: JSON.stringify(body),
    cache: false,
    ...options
  })
}

export const put = (url, body = {}, options = {}) => {
  return request(url, {
    method: 'PUT',
    body: JSON.stringify(body),
    cache: false,
    ...options
  })
}

export const del = (url, options = {}) => {
  return request(url, {
    method: 'DELETE',
    cache: false,
    ...options
  })
}