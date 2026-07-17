import { describe, it, expect, beforeEach } from 'vitest'
import { get, post, put, del, clearCache } from '@/api/index-helpers.js'

// 模拟 axios
vi.mock('axios', () => {
  const mockAxios = {
    create: () => mockAxios,
    interceptors: {
      request: { use: () => {} },
      response: { use: () => {} }
    },
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    request: vi.fn()
  }
  mockAxios.default = mockAxios
  return { default: mockAxios }
})

// Mock localStorage
const localStorageMock = (() => {
  let store = {}
  return {
    getItem: (key) => store[key] || null,
    setItem: (key, value) => { store[key] = String(value) },
    removeItem: (key) => { delete store[key] },
    clear: () => { store = {} }
  }
})()

Object.defineProperty(globalThis, 'localStorage', {
  value: localStorageMock,
  writable: true
})

// Mock element-plus
vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
    warning: vi.fn(),
    success: vi.fn(),
    info: vi.fn()
  }
}))

describe('API Helpers 测试', () => {
  beforeEach(() => {
    localStorageMock.clear()
    clearCache()
  })

  describe('get 请求', () => {
    it('应返回 get 函数', () => {
      expect(typeof get).toBe('function')
    })

    it('应接受 url 参数', () => {
      // get 函数存在且可被调用
      expect(get).toBeDefined()
      expect(get.length >= 1).toBe(true) // 至少有 url 参数
    })
  })

  describe('post 请求', () => {
    it('应返回 post 函数', () => {
      expect(typeof post).toBe('function')
    })
  })

  describe('put 请求', () => {
    it('应返回 put 函数', () => {
      expect(typeof put).toBe('function')
    })
  })

  describe('del 请求', () => {
    it('应返回 del 函数', () => {
      expect(typeof del).toBe('function')
    })
  })

  describe('HTTP 方法导出', () => {
    it('应导出 get, post, put, del 四个方法', () => {
      expect(get).toBeDefined()
      expect(post).toBeDefined()
      expect(put).toBeDefined()
      expect(del).toBeDefined()
    })

    it('get 是一个函数', () => {
      expect(typeof get).toBe('function')
    })

    it('post 是一个函数', () => {
      expect(typeof post).toBe('function')
    })

    it('put 是一个函数', () => {
      expect(typeof put).toBe('function')
    })

    it('del 是一个函数', () => {
      expect(typeof del).toBe('function')
    })
  })

  describe('clearCache', () => {
    it('应导出 clearCache 函数', () => {
      expect(typeof clearCache).toBe('function')
    })
  })

  describe('import 完整性', () => {
    it('应能正常导入 index-helpers 模块', async () => {
      const module = await import('@/api/index-helpers.js')
      expect(module.default).toBeDefined()
      expect(module.get).toBeDefined()
      expect(module.post).toBeDefined()
      expect(module.put).toBeDefined()
      expect(module.del).toBeDefined()
      expect(module.clearCache).toBeDefined()
    })
  })
})
