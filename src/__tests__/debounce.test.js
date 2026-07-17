import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { debounce, throttle } from '@/utils/debounce.js'

describe('debounce 防抖函数', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('应该在延迟后执行函数', () => {
    const fn = vi.fn()
    const debouncedFn = debounce(fn, 300)

    debouncedFn()
    expect(fn).not.toHaveBeenCalled()

    vi.advanceTimersByTime(300)
    expect(fn).toHaveBeenCalledTimes(1)
  })

  it('多次调用只执行最后一次', () => {
    const fn = vi.fn()
    const debouncedFn = debounce(fn, 300)

    debouncedFn()
    debouncedFn()
    debouncedFn()

    vi.advanceTimersByTime(300)
    expect(fn).toHaveBeenCalledTimes(1)
  })

  it('应该在延迟内重置计时器', () => {
    const fn = vi.fn()
    const debouncedFn = debounce(fn, 300)

    debouncedFn()
    vi.advanceTimersByTime(200)
    debouncedFn() // 重置计时器
    vi.advanceTimersByTime(200)
    expect(fn).not.toHaveBeenCalled()

    vi.advanceTimersByTime(100)
    expect(fn).toHaveBeenCalledTimes(1)
  })

  it('应传递正确的参数', () => {
    const fn = vi.fn()
    const debouncedFn = debounce(fn, 300)

    debouncedFn('hello', 123)
    vi.advanceTimersByTime(300)

    expect(fn).toHaveBeenCalledWith('hello', 123)
  })

  it('默认延迟应为300ms', () => {
    const fn = vi.fn()
    const debouncedFn = debounce(fn)

    debouncedFn()
    vi.advanceTimersByTime(300)
    expect(fn).toHaveBeenCalledTimes(1)
  })
})

describe('throttle 节流函数', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('第一次调用立即执行', () => {
    const fn = vi.fn()
    const throttledFn = throttle(fn, 1000)

    throttledFn()
    expect(fn).toHaveBeenCalledTimes(1)
  })

  it('在间隔内重复调用只执行一次', () => {
    const fn = vi.fn()
    const throttledFn = throttle(fn, 1000)

    throttledFn()
    throttledFn()
    throttledFn()

    expect(fn).toHaveBeenCalledTimes(1)

    vi.advanceTimersByTime(500)
    throttledFn()
    expect(fn).toHaveBeenCalledTimes(1)
  })

  it('间隔结束后可以再次执行', () => {
    const fn = vi.fn()
    const throttledFn = throttle(fn, 1000)

    throttledFn()
    expect(fn).toHaveBeenCalledTimes(1)

    vi.advanceTimersByTime(1000)
    throttledFn()
    expect(fn).toHaveBeenCalledTimes(2)
  })

  it('应传递正确的参数', () => {
    const fn = vi.fn()
    const throttledFn = throttle(fn, 1000)

    throttledFn('arg1', 'arg2')
    expect(fn).toHaveBeenCalledWith('arg1', 'arg2')
  })
})
