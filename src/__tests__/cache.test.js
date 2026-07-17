import { describe, it, expect, beforeEach } from 'vitest'
import { memoryCacheUtil, withCache } from '@/utils/cache.js'

describe('memoryCacheUtil 内存缓存工具', () => {
  beforeEach(() => {
    memoryCacheUtil.clear()
  })

  it('应能设置和获取缓存值', () => {
    memoryCacheUtil.set('key1', 'value1')
    expect(memoryCacheUtil.get('key1')).toBe('value1')
  })

  it('不存在的key应返回null', () => {
    expect(memoryCacheUtil.get('nonexistent')).toBeNull()
  })

  it('应能检查key是否存在', () => {
    memoryCacheUtil.set('key1', 'value1')
    expect(memoryCacheUtil.has('key1')).toBe(true)
    expect(memoryCacheUtil.has('nonexistent')).toBe(false)
  })

  it('过期的缓存应返回null', () => {
    // 设置已过期的缓存（负的过期时间）
    memoryCacheUtil.set('key1', 'value1', -1000)

    expect(memoryCacheUtil.get('key1')).toBeNull()
  })

  it('应能删除指定缓存', () => {
    memoryCacheUtil.set('key1', 'value1')
    memoryCacheUtil.set('key2', 'value2')

    memoryCacheUtil.delete('key1')
    expect(memoryCacheUtil.get('key1')).toBeNull()
    expect(memoryCacheUtil.get('key2')).toBe('value2')
  })

  it('应能清空所有缓存', () => {
    memoryCacheUtil.set('key1', 'value1')
    memoryCacheUtil.set('key2', 'value2')

    memoryCacheUtil.clear()
    expect(memoryCacheUtil.size()).toBe(0)
  })

  it('应能获取缓存大小', () => {
    expect(memoryCacheUtil.size()).toBe(0)
    memoryCacheUtil.set('key1', 'value1')
    memoryCacheUtil.set('key2', 'value2')
    expect(memoryCacheUtil.size()).toBe(2)
  })

  it('设置不同类型的值', () => {
    memoryCacheUtil.set('num', 123)
    memoryCacheUtil.set('arr', [1, 2, 3])
    memoryCacheUtil.set('obj', { a: 1 })

    expect(memoryCacheUtil.get('num')).toBe(123)
    expect(memoryCacheUtil.get('arr')).toEqual([1, 2, 3])
    expect(memoryCacheUtil.get('obj')).toEqual({ a: 1 })
  })
})

describe('withCache 缓存包装函数', () => {
  beforeEach(() => {
    memoryCacheUtil.clear()
  })

  it('首次调用执行原函数', async () => {
    const fn = vi.fn().mockResolvedValue('result')
    const cachedFn = withCache(fn, 'test-key')

    const result = await cachedFn()
    expect(result).toBe('result')
    expect(fn).toHaveBeenCalledTimes(1)
  })

  it('缓存命中时不再次调用原函数', async () => {
    const fn = vi.fn().mockResolvedValue('result')
    const cachedFn = withCache(fn, 'test-key')

    await cachedFn() // 首次调用
    expect(fn).toHaveBeenCalledTimes(1)

    const result = await cachedFn() // 缓存命中
    expect(result).toBe('result')
    expect(fn).toHaveBeenCalledTimes(1) // 不增加
  })

  it('支持动态key生成函数', async () => {
    const fn = vi.fn().mockResolvedValue('result')
    const getKey = (id) => `item-${id}`
    const cachedFn = withCache(fn, getKey)

    await cachedFn(1)
    await cachedFn(2)

    expect(fn).toHaveBeenCalledTimes(2) // 不同的key，都应调用
  })
})
