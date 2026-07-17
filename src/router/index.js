import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/components/LoginPage.vue'),
    meta: { guest: true, title: '登录' }
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: () => import('@/components/Dashboard.vue'),
    meta: { requiresAuth: true, title: '主控制台' }
  },
  {
    path: '/basic',
    name: 'basic',
    component: () => import('@/components/BasicManagement.vue'),
    meta: { requiresAuth: true, title: '基础档案', roles: ['ADMIN', 'MANAGER', 'OPERATOR'] }
  },
  {
    path: '/inventory',
    name: 'inventory',
    component: () => import('@/components/InventoryManagement.vue'),
    meta: { requiresAuth: true, title: '库存管理', roles: ['ADMIN', 'MANAGER', 'OPERATOR'] }
  },
  {
    path: '/orders',
    name: 'orders',
    component: () => import('@/components/OrderManagement.vue'),
    meta: { requiresAuth: true, title: '出入库管理', roles: ['ADMIN', 'MANAGER', 'OPERATOR'] }
  },
  {
    path: '/reports',
    name: 'reports',
    component: () => import('@/components/ReportCenter.vue'),
    meta: { requiresAuth: true, title: '报表中心', roles: ['ADMIN', 'MANAGER'] }
  },
  {
    path: '/barcode',
    name: 'barcode',
    component: () => import('@/components/BarcodeScanner.vue'),
    meta: { requiresAuth: true, title: '条码扫描', roles: ['ADMIN', 'MANAGER', 'OPERATOR'] }
  },
  {
    path: '/settings',
    name: 'settings',
    component: () => import('@/components/SettingsPanel.vue'),
    meta: { requiresAuth: true, title: '系统设置', roles: ['ADMIN', 'MANAGER'] }
  },
  {
    path: '/system',
    name: 'system',
    component: () => import('@/components/UserManagement.vue'),
    meta: { requiresAuth: true, title: '用户管理', roles: ['ADMIN'] }
  },
  {
    path: '/expiry-warning',
    name: 'expiry-warning',
    component: () => import('@/components/ExpiryWarning.vue'),
    meta: { requiresAuth: true, title: '效期预警', roles: ['ADMIN', 'MANAGER', 'OPERATOR'] }
  },
  {
    path: '/notification',
    name: 'notification',
    component: () => import('@/components/NotificationCenter.vue'),
    meta: { requiresAuth: true, title: '消息通知', roles: ['ADMIN', 'MANAGER', 'OPERATOR'] }
  },
  {
    path: '/stock-count',
    name: 'stock-count',
    component: () => import('@/components/StockCount.vue'),
    meta: { requiresAuth: true, title: '库存盘点', roles: ['ADMIN', 'MANAGER', 'OPERATOR'] }
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('@/components/Profile.vue'),
    meta: { requiresAuth: true, title: '个人信息', roles: ['ADMIN', 'MANAGER', 'OPERATOR'] }
  },
  {
    path: '/help',
    name: 'help',
    component: () => import('@/components/HelpCenter.vue'),
    meta: { requiresAuth: true, title: '帮助中心', roles: ['ADMIN', 'MANAGER', 'OPERATOR'] }
  },
  {
    path: '/wave-picking',
    name: 'wave-picking',
    component: () => import('@/components/WavePicking.vue'),
    meta: { requiresAuth: true, title: '波次拣货', roles: ['ADMIN', 'MANAGER', 'OPERATOR'] }
  },
  {
    path: '/return',
    name: 'return',
    component: () => import('@/components/ReturnManagement.vue'),
    meta: { requiresAuth: true, title: '退货管理', roles: ['ADMIN', 'MANAGER', 'OPERATOR'] }
  },
  {
    path: '/warehouse',
    name: 'warehouse',
    component: () => import('@/components/WarehouseManagement.vue'),
    meta: { requiresAuth: true, title: '仓库管理', roles: ['ADMIN', 'MANAGER'] }
  },
  {
    path: '/operation-log',
    name: 'operation-log',
    component: () => import('@/components/OperationLog.vue'),
    meta: { requiresAuth: true, title: '操作日志', roles: ['ADMIN', 'MANAGER'] }
  },
  {
    path: '/vision',
    name: 'vision',
    component: () => import('@/components/VisionCapture.vue'),
    meta: { requiresAuth: true, title: '视觉识别', roles: ['ADMIN', 'MANAGER', 'OPERATOR'] }
  },
  {
    path: '/vision-result',
    name: 'vision-result',
    component: () => import('@/components/VisionResult.vue'),
    meta: { requiresAuth: true, title: '识别结果', roles: ['ADMIN', 'MANAGER', 'OPERATOR'] }
  },
  {
    path: '/print',
    name: 'print',
    component: () => import('@/components/PrintManager.vue'),
    meta: { requiresAuth: true, title: '打印管理', roles: ['ADMIN', 'MANAGER', 'OPERATOR'] }
  },
  {
    path: '/order-print/:orderId',
    name: 'order-print',
    component: () => import('@/components/OrderPrint.vue'),
    meta: { requiresAuth: true, title: '订单打印', roles: ['ADMIN', 'MANAGER', 'OPERATOR'] }
  },
  {
    path: '/model',
    name: 'model',
    component: () => import('@/components/ModelPreview.vue'),
    meta: { requiresAuth: true, title: '模型管理', roles: ['ADMIN', 'MANAGER'] }
  },
  {
    path: '/stock-scene',
    name: 'stock-scene',
    component: () => import('@/components/StockScene.vue'),
    meta: { requiresAuth: true, title: '库存场景', roles: ['ADMIN', 'MANAGER', 'OPERATOR'] }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * 路由守卫：检查认证状态和角色权限。
 * 一次性读取 localStorage，避免重复读取。
 */
router.beforeEach((to, from, next) => {
  // 一次性读取认证数据
  const token = localStorage.getItem('token')
  let user = null
  try {
    const userStr = localStorage.getItem('user')
    if (userStr) {
      user = JSON.parse(userStr)
    }
  } catch (e) {
    // 用户数据损坏，清除
    localStorage.removeItem('user')
  }

  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - SmartVision Stock`
  } else {
    document.title = 'SmartVision Stock - 智能仓储管理系统'
  }

  // 已登录用户访问登录页或其他guest页面，直接重定向到首页
  if (to.meta.guest && token) {
    next('/dashboard')
    return
  }

  // 访问需要认证的页面但无 token
  if (to.meta.requiresAuth && !token) {
    ElMessage.warning('请先登录系统')
    next('/login')
    return
  }

  // 需要认证的页面，检查用户信息是否完整
  if (to.meta.requiresAuth && !user) {
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    ElMessage.warning('用户信息失效，请重新登录')
    next('/login')
    return
  }

  // 检查角色权限
  if (to.meta.roles && user) {
    if (!user.role || !to.meta.roles.includes(user.role)) {
      ElMessage.error('无权限访问此页面')
      next('/dashboard')
      return
    }
  }

  next()
})

export default router
