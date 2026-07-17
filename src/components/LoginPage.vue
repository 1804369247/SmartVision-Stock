<template>
  <div class="login-container">
    <!-- 科幻背景 -->
    <div class="cyber-bg">
      <!-- 动态网格 -->
      <div class="grid-container">
        <div v-for="i in 20" :key="i" class="grid-line-h" :style="{top: `${i * 5}%`}"></div>
        <div v-for="i in 20" :key="'v'+i" class="grid-line-v" :style="{left: `${i * 5}%`}"></div>
      </div>

      <!-- 粒子效果 -->
      <div class="particles">
        <div v-for="i in 30" :key="i" class="particle" :style="getParticleStyle(i)"></div>
      </div>

      <!-- 光晕效果 -->
      <div class="glow-orb glow-orb-1"></div>
      <div class="glow-orb glow-orb-2"></div>
      <div class="glow-orb glow-orb-3"></div>
    </div>

    <!-- 登录/注册卡片 -->
    <div class="cyber-card">
      <!-- 头部装饰条 -->
      <div class="card-header">
        <div class="header-line"></div>
        <div class="header-glow"></div>
      </div>

      <!-- Logo区域 -->
      <div class="logo-section">
        <div class="logo-container">
          <div class="logo-ring"></div>
          <div class="logo-icon">
            <svg viewBox="0 0 64 64" class="logo-svg">
              <defs>
                <linearGradient id="logoGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" style="stop-color:#00f5ff;stop-opacity:1" />
                  <stop offset="100%" style="stop-color:#7b2fff;stop-opacity:1" />
                </linearGradient>
              </defs>
              <rect x="12" y="20" width="12" height="24" rx="2" fill="url(#logoGradient)"/>
              <rect x="26" y="12" width="12" height="40" rx="2" fill="url(#logoGradient)"/>
              <rect x="40" y="20" width="12" height="24" rx="2" fill="url(#logoGradient)"/>
              <line x1="4" y1="44" x2="60" y2="44" stroke="#00f5ff" stroke-width="2" stroke-dasharray="4 4"/>
              <circle cx="32" cy="32" r="4" fill="#00f5ff" class="logo-core"/>
            </svg>
          </div>
        </div>
        <h1 class="title">SmartVision Stock</h1>
        <p class="subtitle">智能仓储管理系统</p>
        <div class="subtitle-line"></div>
      </div>

      <!-- 登录 / 注册 切换标签 -->
      <div class="tab-switcher">
        <button
          class="tab-btn"
          :class="{ active: !isRegister }"
          @click="switchTab(false)"
        >登录</button>
        <button
          class="tab-btn"
          :class="{ active: isRegister }"
          @click="switchTab(true)"
        >注册</button>
        <div class="tab-slider" :class="{ right: isRegister }"></div>
      </div>

      <!-- 错误提示 -->
      <transition name="glitch">
        <div v-if="errorMsg" class="error-panel">
          <div class="error-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="#ff3366" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="8" x2="12" y2="12"/>
              <line x1="12" y1="16" x2="12" y2="16"/>
            </svg>
          </div>
          <span>{{ errorMsg }}</span>
        </div>
      </transition>

      <!-- ========== 登录表单 ========== -->
      <form v-if="!isRegister" class="login-form" @submit.prevent="handleLogin">
        <!-- 用户名输入 -->
        <div class="input-group">
          <div class="input-label">
            <svg viewBox="0 0 24 24" fill="none" stroke="#00f5ff" stroke-width="1.5">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
              <circle cx="12" cy="7" r="4"/>
            </svg>
            <span>用户名</span>
          </div>
          <div class="input-wrapper">
            <input
              v-model="loginForm.username"
              type="text"
              placeholder="请输入用户名"
              class="cyber-input"
              @input="clearError"
              autocomplete="username"
            />
            <div class="input-border"></div>
          </div>
        </div>

        <!-- 密码输入 -->
        <div class="input-group">
          <div class="input-label">
            <svg viewBox="0 0 24 24" fill="none" stroke="#00f5ff" stroke-width="1.5">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
            </svg>
            <span>密码</span>
          </div>
          <div class="input-wrapper">
            <input
              v-model="loginForm.password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="请输入密码"
              class="cyber-input"
              @input="clearError"
              @keyup.enter="handleLogin"
              autocomplete="current-password"
            />
            <button type="button" class="password-toggle" @click="showPassword = !showPassword">
              <svg v-if="!showPassword" viewBox="0 0 24 24" fill="none" stroke="#666" stroke-width="1.5">
                <path d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0"/>
                <path d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="#666" stroke-width="1.5">
                <path d="M13.875 18.825A10.05 10.05 0 0 1 12 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 0 1 1.563-3.029m5.858.908a3 3 0 1 1 4.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0 1 12 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 0 1-4.132 5.411m0 0L21 21"/>
              </svg>
            </button>
            <div class="input-border"></div>
          </div>
        </div>

        <!-- 记住我 -->
        <label class="remember-checkbox">
          <input type="checkbox" v-model="loginForm.rememberMe" />
          <span class="checkmark"></span>
          <span class="label-text">记住我</span>
        </label>

        <!-- 登录按钮 -->
        <button type="submit" class="login-btn" :disabled="loading || !loginForm.username || !loginForm.password">
          <span class="btn-text">{{ loading ? '验证中...' : '登 录' }}</span>
          <div class="btn-glow"></div>
          <div class="btn-pulse"></div>
        </button>
      </form>

      <!-- ========== 注册表单 ========== -->
      <form v-else class="login-form register-form" @submit.prevent="handleRegister">
        <!-- 用户名 -->
        <div class="input-group">
          <div class="input-label">
            <svg viewBox="0 0 24 24" fill="none" stroke="#00f5ff" stroke-width="1.5">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
              <circle cx="12" cy="7" r="4"/>
            </svg>
            <span>用户名</span>
          </div>
          <div class="input-wrapper">
            <input
              v-model="registerForm.username"
              type="text"
              placeholder="至少3个字符"
              class="cyber-input"
              @input="clearError"
              autocomplete="username"
            />
            <div class="input-border"></div>
          </div>
        </div>

        <!-- 邮箱（可选） -->
        <div class="input-group">
          <div class="input-label">
            <svg viewBox="0 0 24 24" fill="none" stroke="#00f5ff" stroke-width="1.5">
              <rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 6-10 7L2 6"/>
            </svg>
            <span>邮箱 <small style="opacity:0.5;font-size:9px">(选填)</small></span>
          </div>
          <div class="input-wrapper">
            <input
              v-model="registerForm.email"
              type="email"
              placeholder="example@mail.com"
              class="cyber-input"
              @input="clearError"
              autocomplete="email"
            />
            <div class="input-border"></div>
          </div>
        </div>

        <!-- 手机号（可选） -->
        <div class="input-group">
          <div class="input-label">
            <svg viewBox="0 0 24 24" fill="none" stroke="#00f5ff" stroke-width="1.5">
              <rect x="5" y="2" width="14" height="20" rx="2" ry="2"/><line x1="12" y1="18" x2="12.01" y2="18"/>
            </svg>
            <span>手机号 <small style="opacity:0.5;font-size:9px">(选填)</small></span>
          </div>
          <div class="input-wrapper">
            <input
              v-model="registerForm.phone"
              type="tel"
              placeholder="138xxxx8888"
              class="cyber-input"
              maxlength="11"
              @input="clearError"
            />
            <div class="input-border"></div>
          </div>
        </div>

        <!-- 密码 -->
        <div class="input-group">
          <div class="input-label">
            <svg viewBox="0 0 24 24" fill="none" stroke="#00f5ff" stroke-width="1.5">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
            </svg>
            <span>密码</span>
          </div>
          <div class="input-wrapper">
            <input
              v-model="registerForm.password"
              :type="showRegPassword ? 'text' : 'password'"
              placeholder="至少8位，含字母和数字"
              class="cyber-input"
              @input="clearError"
              autocomplete="new-password"
            />
            <button type="button" class="password-toggle" @click="showRegPassword = !showRegPassword">
              <svg v-if="!showRegPassword" viewBox="0 0 24 24" fill="none" stroke="#666" stroke-width="1.5">
                <path d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0"/><path d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="#666" stroke-width="1.5">
                <path d="M13.875 18.825A10.05 10.05 0 0 1 12 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 0 1 1.563-3.029m5.858.908a3 3 0 1 1 4.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0 1 12 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 0 1-4.132 5.411m0 0L21 21"/>
              </svg>
            </button>
            <div class="input-border"></div>
          </div>
        </div>

        <!-- 确认密码 -->
        <div class="input-group">
          <div class="input-label">
            <svg viewBox="0 0 24 24" fill="none" stroke="#00f5ff" stroke-width="1.5">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            </svg>
            <span>确认密码</span>
          </div>
          <div class="input-wrapper">
            <input
              v-model="registerForm.confirmPassword"
              :type="showRegConfirmPassword ? 'text' : 'password'"
              placeholder="再次输入密码"
              class="cyber-input"
              @input="clearError"
              @keyup.enter="handleRegister"
              autocomplete="new-password"
            />
            <button type="button" class="password-toggle" @click="showRegConfirmPassword = !showRegConfirmPassword">
              <svg v-if="!showRegConfirmPassword" viewBox="0 0 24 24" fill="none" stroke="#666" stroke-width="1.5">
                <path d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0"/><path d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="#666" stroke-width="1.5">
                <path d="M13.875 18.825A10.05 10.05 0 0 1 12 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 0 1 1.563-3.029m5.858.908a3 3 0 1 1 4.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0 1 12 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 0 1-4.132 5.411m0 0L21 21"/>
              </svg>
            </button>
            <div class="input-border"></div>
          </div>
        </div>

        <!-- 注册按钮 -->
        <button type="submit" class="login-btn register-btn" :disabled="loading || !canRegister">
          <span class="btn-text">{{ loading ? '注册中...' : '注 册' }}</span>
          <div class="btn-glow"></div>
          <div class="btn-pulse"></div>
        </button>

        <!-- 注册协议提示 -->
        <p class="register-agreement">
          注册即表示您同意系统使用规范，默认角色为普通用户，实名认证可在个人中心完成
        </p>
      </form>

      <!-- 底部信息 -->
      <div class="footer-info">
        <div class="footer-line"></div>
        <span class="version">v2.0.0</span>
        <div class="footer-line"></div>
      </div>

      <!-- 卡片底部装饰 -->
      <div class="card-footer">
        <div class="footer-glow"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { systemApi } from '../api/system'

const router = useRouter()

// ==========================================
// 模式切换
// ==========================================
const isRegister = ref(false)

const switchTab = (toRegister) => {
  clearError()
  isRegister.value = toRegister
}

// ==========================================
// 表单状态
// ==========================================
const loading = ref(false)
const errorMsg = ref('')
const showPassword = ref(false)
const showRegPassword = ref(false)
const showRegConfirmPassword = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
  rememberMe: false
})

const registerForm = reactive({
  username: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: ''
})

// 注册按钮可用性校验
const canRegister = computed(() =>
  registerForm.username.length >= 3 &&
  registerForm.password.length >= 8 &&
  registerForm.confirmPassword === registerForm.password
)

// ==========================================
// 粒子样式生成
// ==========================================
const getParticleStyle = (index) => {
  const left = Math.random() * 100
  const top = Math.random() * 100
  const size = Math.random() * 3 + 1
  const duration = Math.random() * 10 + 5
  const delay = Math.random() * 5
  return {
    left: `${left}%`,
    top: `${top}%`,
    width: `${size}px`,
    height: `${size}px`,
    animationDuration: `${duration}s`,
    animationDelay: `${delay}s`
  }
}

// ==========================================
// 初始化 — 恢复记住的用户名
// ==========================================
onMounted(() => {
  const saved = localStorage.getItem('savedUsername')
  const remembered = localStorage.getItem('rememberMe') === 'true'
  if (saved && remembered) {
    loginForm.username = saved
    loginForm.rememberMe = true
  }

  // 已登录用户直接跳转到首页
  if (localStorage.getItem('token')) {
    router.replace('/dashboard')
  }
})

// ==========================================
// 清除错误提示
// ==========================================
const clearError = () => {
  if (errorMsg.value) errorMsg.value = ''
}

// ==========================================
// 登录处理
// ==========================================
const handleLogin = async () => {
  errorMsg.value = ''

  if (!loginForm.username.trim()) {
    errorMsg.value = '请输入用户名'
    return
  }
  if (!loginForm.password) {
    errorMsg.value = '请输入密码'
    return
  }

  loading.value = true

  try {
    const res = await systemApi.login({
      username: loginForm.username.trim(),
      password: loginForm.password,
      rememberMe: loginForm.rememberMe
    })

    if (res.code === 200 && res.data?.token) {
      // 保存 Token
      localStorage.setItem('token', res.data.token)

      // 记住我
      if (loginForm.rememberMe) {
        localStorage.setItem('savedUsername', loginForm.username.trim())
        localStorage.setItem('rememberMe', 'true')
        if (res.data.refreshToken) {
          localStorage.setItem('refreshToken', res.data.refreshToken)
        }
      } else {
        localStorage.removeItem('savedUsername')
        localStorage.removeItem('rememberMe')
        localStorage.removeItem('refreshToken')
      }

      // 保存用户信息
      if (res.data.user) {
        localStorage.setItem('user', JSON.stringify(res.data.user))
      }

      ElMessage.success(`欢迎回来，${res.data.user?.realName || res.data.user?.username || loginForm.username}`)
      router.push('/dashboard')
    } else {
      errorMsg.value = res.message || '登录失败'
    }
  } catch (e) {
    const data = e?.response?.data
    if (data && data.message) {
      errorMsg.value = data.message
    } else if (e?.message) {
      errorMsg.value = e.message
    } else {
      errorMsg.value = '网络异常，请稍后重试'
    }
  } finally {
    loading.value = false
  }
}

// ==========================================
// 注册处理
// ==========================================
const handleRegister = async () => {
  errorMsg.value = ''

  // 前端校验
  if (registerForm.username.length < 3) {
    errorMsg.value = '用户名至少需要 3 个字符'
    return
  }
  if (registerForm.password.length < 8) {
    errorMsg.value = '密码至少需要 8 个字符'
    return
  }
  if (!/[a-zA-Z]/.test(registerForm.password) || !/[0-9]/.test(registerForm.password)) {
    errorMsg.value = '密码必须包含字母和数字'
    return
  }
  if (registerForm.confirmPassword !== registerForm.password) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }
  if (registerForm.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(registerForm.email)) {
    errorMsg.value = '邮箱格式不正确'
    return
  }

  loading.value = true

  try {
    const res = await systemApi.register({
      username: registerForm.username.trim(),
      email: registerForm.email.trim() || undefined,
      phone: registerForm.phone.trim() || undefined,
      password: registerForm.password,
      role: 'USER',
      enabled: true
    })

    if (res.code === 200) {
      ElMessage.success('注册成功！即将跳转到登录页面...')
      // 自动切换回登录页并填入用户名
      setTimeout(() => {
        loginForm.username = registerForm.username
        isRegister.value = false
        clearError()
      }, 1200)
    } else {
      errorMsg.value = res.message || '注册失败'
    }
  } catch (e) {
    const data = e?.response?.data
    if (data && data.message) {
      errorMsg.value = data.message
    } else if (e?.message) {
      errorMsg.value = e.message
    } else {
      errorMsg.value = '网络异常，请稍后重试'
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ==========================================
   整体容器
   ========================================== */
.login-container {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0a0a1a;
  overflow: hidden;
  font-family: 'Segoe UI', 'Roboto', sans-serif;
}

/* ==========================================
   科幻背景
   ========================================== */
.cyber-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 20% 80%, rgba(0, 245, 255, 0.08) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 20%, rgba(123, 47, 255, 0.08) 0%, transparent 50%),
    linear-gradient(180deg, #0a0a1a 0%, #0f0f2a 50%, #0a0a1a 100%);
}

/* 网格效果 */
.grid-container {
  position: absolute;
  inset: 0;
  opacity: 0.15;
}
.grid-line-h {
  position: absolute;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, #00f5ff, transparent);
}
.grid-line-v {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 1px;
  background: linear-gradient(180deg, transparent, #00f5ff, transparent);
}

/* 粒子效果 */
.particles {
  position: absolute;
  inset: 0;
}
.particle {
  position: absolute;
  background: #00f5ff;
  border-radius: 50%;
  opacity: 0.6;
  animation: float 10s infinite ease-in-out;
}
@keyframes float {
  0%, 100% { transform: translateY(0) translateX(0); opacity: 0.2; }
  25% { transform: translateY(-20px) translateX(10px); opacity: 0.8; }
  50% { transform: translateY(-10px) translateX(-5px); opacity: 0.4; }
  75% { transform: translateY(-30px) translateX(15px); opacity: 0.6; }
}

/* 光晕效果 */
.glow-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
}
.glow-orb-1 {
  width: 400px;
  height: 400px;
  background: #00f5ff;
  top: -100px;
  right: -100px;
  animation: pulse-glow 4s infinite;
}
.glow-orb-2 {
  width: 300px;
  height: 300px;
  background: #7b2fff;
  bottom: -50px;
  left: -50px;
  animation: pulse-glow 5s infinite 1s;
}
.glow-orb-3 {
  width: 250px;
  height: 250px;
  background: #ff0066;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation: pulse-glow 6s infinite 2s;
}
@keyframes pulse-glow {
  0%, 100% { transform: scale(1); opacity: 0.2; }
  50% { transform: scale(1.2); opacity: 0.4; }
}

/* ==========================================
   登录卡片
   ========================================== */
.cyber-card {
  position: relative;
  z-index: 10;
  width: 420px;
  padding: 36px 40px 32px;
  background: rgba(10, 10, 30, 0.9);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(0, 245, 255, 0.2);
  border-radius: 20px;
  box-shadow:
    0 0 40px rgba(0, 245, 255, 0.1),
    0 0 80px rgba(123, 47, 255, 0.05),
    inset 0 0 40px rgba(0, 245, 255, 0.02);
  overflow: hidden;
}

/* 卡片头部装饰 */
.card-header {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  overflow: hidden;
}
.header-line {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, #00f5ff, #7b2fff, #00f5ff, transparent);
  animation: scan-line 3s linear infinite;
}
@keyframes scan-line {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}
.header-glow {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100px;
  height: 2px;
  background: #00f5ff;
  filter: blur(10px);
}

/* Logo区域 */
.logo-section {
  text-align: center;
  margin-bottom: 24px;
}
.logo-container {
  position: relative;
  width: 80px;
  height: 80px;
  margin: 0 auto 16px;
}
.logo-ring {
  position: absolute;
  inset: -10px;
  border: 2px solid rgba(0, 245, 255, 0.3);
  border-radius: 50%;
  animation: rotate-ring 8s linear infinite;
}
@keyframes rotate-ring {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
.logo-icon {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.logo-svg {
  width: 60px;
  height: 60px;
}
.logo-core {
  animation: pulse-core 2s ease-in-out infinite;
}
@keyframes pulse-core {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.2); }
}

.title {
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 2px;
  color: #fff;
  margin: 0 0 6px;
  text-shadow: 0 0 20px rgba(0, 245, 255, 0.5);
}
.subtitle {
  font-size: 12px;
  letter-spacing: 3px;
  color: rgba(0, 245, 255, 0.8);
  margin: 0 0 10px;
}
.subtitle-line {
  width: 60px;
  height: 1px;
  margin: 0 auto;
  background: linear-gradient(90deg, transparent, #00f5ff, transparent);
}

/* ==========================================
   登录/注册 标签切换
   ========================================== */
.tab-switcher {
  display: flex;
  position: relative;
  margin-bottom: 22px;
  background: rgba(0, 0, 0, 0.35);
  border-radius: 10px;
  padding: 4px;
}
.tab-btn {
  flex: 1;
  padding: 9px 0;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.45);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 3px;
  cursor: pointer;
  transition: all 0.3s;
  z-index: 1;
  position: relative;
}
.tab-btn.active {
  color: #00f5ff;
}
.tab-btn:hover:not(.active) {
  color: rgba(255, 255, 255, 0.65);
}
.tab-slider {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc(50% - 4px);
  height: calc(100% - 8px);
  background: linear-gradient(135deg, rgba(0, 245, 255, 0.12), rgba(123, 47, 255, 0.12));
  border: 1px solid rgba(0, 245, 255, 0.25);
  border-radius: 8px;
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 0 12px rgba(0, 245, 255, 0.08);
}
.tab-slider.right {
  transform: translateX(100%);
}

/* 错误提示 */
.error-panel {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  margin-bottom: 18px;
  background: rgba(255, 51, 102, 0.15);
  border: 1px solid rgba(255, 51, 102, 0.3);
  border-radius: 8px;
  color: #ff3366;
  font-size: 13px;
}
.error-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}
.glitch-enter-active,
.glitch-leave-active {
  transition: all 0.3s;
}
.glitch-enter-from {
  opacity: 0;
  transform: translateX(-10px);
}
.glitch-leave-to {
  opacity: 0;
  transform: translateX(10px);
}

/* ==========================================
   表单样式
   ========================================== */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.register-form {
  gap: 14px;
}

/* 输入组 */
.input-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.input-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 10px;
  letter-spacing: 2px;
  color: rgba(0, 245, 255, 0.8);
}
.input-label svg {
  width: 14px;
  height: 14px;
}

/* 输入框容器 */
.input-wrapper {
  position: relative;
}
.cyber-input {
  width: 100%;
  height: 44px;
  padding: 0 16px;
  background: rgba(0, 0, 0, 0.4);
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  letter-spacing: 1px;
  outline: none;
  transition: all 0.3s;
  box-sizing: border-box;
}
.cyber-input::placeholder {
  color: rgba(255, 255, 255, 0.28);
  font-size: 13px;
}
.cyber-input:focus {
  box-shadow: 0 0 0 2px rgba(0, 245, 255, 0.3),
              inset 0 0 20px rgba(0, 245, 255, 0.05);
}
.input-border {
  position: absolute;
  inset: 0;
  border: 1px solid rgba(0, 245, 255, 0.2);
  border-radius: 8px;
  pointer-events: none;
  transition: all 0.3s;
}
.input-wrapper:focus-within .input-border {
  border-color: #00f5ff;
  box-shadow: 0 0 10px rgba(0, 245, 255, 0.3);
}

/* 密码切换按钮 */
.password-toggle {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
}
.password-toggle svg {
  width: 18px;
  height: 18px;
}

/* 记住我 */
.remember-checkbox {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  user-select: none;
}
.remember-checkbox input {
  display: none;
}
.checkmark {
  width: 18px;
  height: 18px;
  border: 1px solid rgba(0, 245, 255, 0.4);
  border-radius: 4px;
  position: relative;
  transition: all 0.3s;
}
.remember-checkbox input:checked + .checkmark {
  background: rgba(0, 245, 255, 0.2);
  border-color: #00f5ff;
}
.remember-checkbox input:checked + .checkmark::after {
  content: '';
  position: absolute;
  left: 6px;
  top: 2px;
  width: 5px;
  height: 10px;
  border: solid #00f5ff;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}
.label-text {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  letter-spacing: 1px;
}

/* 注册按钮特殊样式 */
.register-btn {
  background: linear-gradient(135deg, rgba(123, 47, 255, 0.2), rgba(0, 245, 255, 0.2));
  border-color: rgba(123, 47, 255, 0.4);
  color: #c08cff;
}
.register-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, rgba(123, 47, 255, 0.3), rgba(0, 245, 255, 0.3));
  border-color: #c08cff;
  box-shadow: 0 0 30px rgba(123, 47, 255, 0.25);
  transform: translateY(-2px);
  color: #d4b3ff;
}

/* 注册协议提示 */
.register-agreement {
  text-align: center;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.35);
  line-height: 1.5;
  margin-top: -4px;
  letter-spacing: 0.5px;
}

/* 登录按钮 */
.login-btn {
  position: relative;
  height: 48px;
  margin-top: 6px;
  background: linear-gradient(135deg, rgba(0, 245, 255, 0.2), rgba(123, 47, 255, 0.2));
  border: 1px solid rgba(0, 245, 255, 0.4);
  border-radius: 10px;
  color: #00f5ff;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 6px;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s;
}
.login-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, rgba(0, 245, 255, 0.3), rgba(123, 47, 255, 0.3));
  border-color: #00f5ff;
  box-shadow: 0 0 30px rgba(0, 245, 255, 0.3);
  transform: translateY(-2px);
}
.login-btn:active:not(:disabled) {
  transform: translateY(0);
}
.login-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.btn-text {
  position: relative;
  z-index: 1;
}
.btn-glow {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(0, 245, 255, 0.3), transparent);
  transform: translateX(-100%);
  transition: transform 0.5s;
}
.login-btn:hover:not(:disabled) .btn-glow {
  transform: translateX(100%);
}
.btn-pulse {
  position: absolute;
  inset: -2px;
  border-radius: 12px;
  border: 1px solid rgba(0, 245, 255, 0.5);
  opacity: 0;
  animation: btn-pulse 2s infinite;
}
@keyframes btn-pulse {
  0%, 100% { opacity: 0; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.02); }
}

/* 底部信息 */
.footer-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 22px;
  padding-top: 16px;
}
.footer-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0, 245, 255, 0.3), transparent);
}
.version {
  font-size: 10px;
  letter-spacing: 1px;
  color: rgba(255, 255, 255, 0.35);
}

/* 卡片底部装饰 */
.card-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  overflow: hidden;
}
.footer-glow {
  position: absolute;
  bottom: 0;
  left: 30%;
  width: 40%;
  height: 2px;
  background: linear-gradient(90deg, transparent, #00f5ff, transparent);
  filter: blur(5px);
}

/* ==========================================
   响应式
   ========================================== */
@media (max-width: 480px) {
  .cyber-card {
    width: calc(100% - 32px);
    padding: 28px 20px 24px;
    margin: 0 16px;
  }
  .title {
    font-size: 22px;
    letter-spacing: 1px;
  }
  .subtitle {
    font-size: 10px;
    letter-spacing: 2px;
  }
  .logo-container {
    width: 60px;
    height: 60px;
    margin-bottom: 12px;
  }
  .logo-svg {
    width: 45px;
    height: 45px;
  }
  /* 注册表单在小屏幕上间距更紧凑 */
  .register-form {
    gap: 12px;
  }
  .cyber-input {
    height: 40px;
  }
}
</style>
