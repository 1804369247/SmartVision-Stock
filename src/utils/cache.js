const memoryCache = new Map()
const defaultExpire = 5 * 60 * 1000

export const memoryCacheUtil = {
  set(key, value, expire = defaultExpire) {
    memoryCache.set(key, {
      value,
      expire: Date.now() + expire
    })
  },

  get(key) {
    const item = memoryCache.get(key)
    if (!item) return null
    if (Date.now() > item.expire) {
      memoryCache.delete(key)
      return null
    }
    return item.value
  },

  has(key) {
    const item = memoryCache.get(key)
    if (!item) return false
    if (Date.now() > item.expire) {
      memoryCache.delete(key)
      return false
    }
    return true
  },

  delete(key) {
    memoryCache.delete(key)
  },

  clear() {
    memoryCache.clear()
  },

  size() {
    return memoryCache.size
  }
}

export function withCache(fn, getKey, expire = defaultExpire) {
  return async function (...args) {
    const key = typeof getKey === 'function' ? getKey(...args) : getKey
    if (memoryCacheUtil.has(key)) {
      return memoryCacheUtil.get(key)
    }
    const result = await fn.apply(this, args)
    memoryCacheUtil.set(key, result, expire)
    return result
  }
}