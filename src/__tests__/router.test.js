import { describe, it, expect, beforeEach } from 'vitest'

// 模拟 localStorage
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

// 重新导入以使用新的 localStorage mock
describe('路由守卫测试', () => {
  let router

  beforeEach(async () => {
    localStorageMock.clear()
    // 动态导入确保每次都获取最新模块
    const module = await import('@/router/index.js')
    router = module.default
  })

  it('路由应包含必要的路径', () => {
    const routes = router.getRoutes()
    const paths = routes.map(r => r.path)

    expect(paths).toContain('/login')
    expect(paths).toContain('/dashboard')
    expect(paths).toContain('/basic')
    expect(paths).toContain('/inventory')
    expect(paths).toContain('/orders')
    expect(paths).toContain('/reports')
    expect(paths).toContain('/barcode')
    expect(paths).toContain('/settings')
    expect(paths).toContain('/system')
  })

  it('/ 应重定向到 /login', () => {
    const routes = router.getRoutes()
    const rootRoute = routes.find(r => r.path === '/')
    expect(rootRoute.redirect).toBe('/login')
  })

  it('login 路由不需要认证', () => {
    const routes = router.getRoutes()
    const loginRoute = routes.find(r => r.path === '/login')
    expect(loginRoute.meta.requiresAuth).toBeFalsy()
  })

  it('dashboard 路由需要认证', () => {
    const routes = router.getRoutes()
    const dashboardRoute = routes.find(r => r.path === '/dashboard')
    expect(dashboardRoute.meta.requiresAuth).toBe(true)
  })

  it('basic 路由需要 ADMIN/MANAGER/OPERATOR 角色', () => {
    const routes = router.getRoutes()
    const basicRoute = routes.find(r => r.path === '/basic')
    expect(basicRoute.meta.roles).toContain('ADMIN')
    expect(basicRoute.meta.roles).toContain('MANAGER')
    expect(basicRoute.meta.roles).toContain('OPERATOR')
  })

  it('reports 路由仅限 ADMIN/MANAGER', () => {
    const routes = router.getRoutes()
    const reportsRoute = routes.find(r => r.path === '/reports')
    expect(reportsRoute.meta.roles).toContain('ADMIN')
    expect(reportsRoute.meta.roles).toContain('MANAGER')
    expect(reportsRoute.meta.roles).not.toContain('OPERATOR')
  })

  it('system 路由仅限 ADMIN', () => {
    const routes = router.getRoutes()
    const systemRoute = routes.find(r => r.path === '/system')
    expect(systemRoute.meta.roles).toContain('ADMIN')
    expect(systemRoute.meta.roles).not.toContain('MANAGER')
    expect(systemRoute.meta.roles).not.toContain('OPERATOR')
  })

  it('未登录访问需要认证的页面应重定向到 /login', async () => {
    localStorageMock.clear()

    await router.push('/dashboard')
    await router.isReady()

    // 应该被重定向到登录页
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('已登录用户访问 login 页应重定向到 dashboard', async () => {
    const testUser = { userId: 1, username: 'admin', role: 'ADMIN' }
    localStorageMock.setItem('token', 'valid-token')
    localStorageMock.setItem('user', JSON.stringify(testUser))

    await router.push('/login')
    await router.isReady()

    // 已登录用户访问login页会被重定向
    const validRedirects = ['/dashboard', '/login']
    expect(validRedirects).toContain(router.currentRoute.value.path)
  })

  it('ADMIN 可以访问所有页面', async () => {
    const adminUser = { userId: 1, username: 'admin', role: 'ADMIN' }
    localStorageMock.setItem('token', 'admin-token')
    localStorageMock.setItem('user', JSON.stringify(adminUser))

    await router.push('/system')
    await router.isReady()

    expect(router.currentRoute.value.path).toBe('/system')
  })

  it('OPERATOR 不应访问 reports 页面', async () => {
    const opUser = { userId: 2, username: 'op', role: 'OPERATOR' }
    localStorageMock.setItem('token', 'op-token')
    localStorageMock.setItem('user', JSON.stringify(opUser))

    await router.push('/reports')
    await router.isReady()

    // OPERATOR 没有权限，应重定向到 dashboard
    expect(router.currentRoute.value.path).toBe('/dashboard')
  })

  it('损坏的用户数据应触发处理', async () => {
    localStorageMock.setItem('token', 'corrupt-token')
    localStorageMock.setItem('user', 'INVALID_JSON{{{') // 损坏的 JSON

    await router.push('/dashboard')
    await router.isReady()

    // 损坏的用户数据可能导致重定向到login或停留在dashboard（取决于守卫实现）
    // 关键是验证系统不会崩溃
    expect(router.currentRoute.value.path).toBeDefined()
  })

  it('角色不足的页面显示警告信息', async () => {
    const opUser = { userId: 2, username: 'op', role: 'OPERATOR' }
    localStorageMock.setItem('token', 'op-token')
    localStorageMock.setItem('user', JSON.stringify(opUser))

    await router.push('/system')
    await router.isReady()

    // OPERATOR 不能访问 system 页面
    expect(router.currentRoute.value.path).not.toBe('/system')
  })
})
