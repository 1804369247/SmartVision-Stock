<template>
  <div class="profile-page">
    <div class="page-header">
      <h3>个人信息</h3>
    </div>

    <el-card class="profile-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="basic">
          <el-form :model="basicForm" label-width="100px" style="margin-top: 20px;">
            <el-form-item label="用户名">
              <el-input :value="basicForm.username" disabled />
            </el-form-item>
            <el-form-item label="真实姓名" required>
              <el-input v-model="basicForm.realName" placeholder="请输入真实姓名" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="basicForm.email" type="email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="basicForm.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="角色">
              <el-input :value="basicForm.role" disabled />
            </el-form-item>
            <el-form-item label="创建时间">
              <el-input :value="formatDate(basicForm.createTime)" disabled />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="updateBasic">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="修改密码" name="password">
          <el-form :model="passwordForm" label-width="120px" style="margin-top: 20px;">
            <el-form-item label="原密码" required>
              <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入原密码" />
            </el-form-item>
            <el-form-item label="新密码" required>
              <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" />
            </el-form-item>
            <el-form-item label="确认密码" required>
              <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="updatePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { systemApi } from '../api/system'

const activeTab = ref('basic')
const userId = ref(null)

const basicForm = reactive({
  username: '',
  realName: '',
  email: '',
  phone: '',
  role: '',
  createTime: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const loadProfile = async () => {
  try {
    const userStr = localStorage.getItem('user')
    if (userStr) {
      const user = JSON.parse(userStr)
      userId.value = user.userId || user.id
    }

    if (!userId.value) {
      const res = await systemApi.getCurrentUser()
      if (res.code === 200 && res.data?.user) {
        userId.value = res.data.user.userId
        basicForm.username = res.data.user.username
        basicForm.realName = res.data.user.realName || ''
        basicForm.email = res.data.user.email || ''
        basicForm.phone = res.data.user.phone || ''
        basicForm.role = res.data.user.role === 'ADMIN' ? '管理员' : '操作员'
      }
    } else {
      const res = await systemApi.getUser(userId.value)
      if (res.code === 200 && res.data) {
        basicForm.username = res.data.username || ''
        basicForm.realName = res.data.realName || ''
        basicForm.email = res.data.email || ''
        basicForm.phone = res.data.phone || ''
        basicForm.role = res.data.role === 'ADMIN' ? '管理员' : '操作员'
        basicForm.createTime = res.data.createTime || ''
      }
    }
  } catch (e) {
    console.error('加载个人信息失败:', e)
    basicForm.username = '用户'
    basicForm.realName = ''
    basicForm.email = ''
    basicForm.phone = ''
    basicForm.role = '操作员'
  }
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const updateBasic = async () => {
  if (!basicForm.realName) {
    ElMessage.warning('请填写真实姓名')
    return
  }
  if (!userId.value) {
    ElMessage.warning('用户信息无效')
    return
  }
  try {
    const result = await systemApi.updateUser(userId.value, {
      realName: basicForm.realName,
      email: basicForm.email,
      phone: basicForm.phone
    })
    if (result.code === 200) {
      ElMessage.success('修改成功')
      const userStr = localStorage.getItem('user')
      if (userStr) {
        const user = JSON.parse(userStr)
        user.realName = basicForm.realName
        localStorage.setItem('user', JSON.stringify(user))
      }
    } else {
      ElMessage.error(result.message || '修改失败')
    }
  } catch (e) {
    ElMessage.error('修改失败')
  }
}

const updatePassword = async () => {
  if (!passwordForm.oldPassword) {
    ElMessage.warning('请输入原密码')
    return
  }
  if (!passwordForm.newPassword) {
    ElMessage.warning('请输入新密码')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  if (!userId.value) {
    ElMessage.warning('用户信息无效')
    return
  }
  try {
    const result = await systemApi.changePassword(userId.value, {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    if (result.code === 200) {
      ElMessage.success('密码修改成功，请重新登录')
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')
      window.location.href = '/login'
    } else {
      ElMessage.error(result.message || '修改失败')
    }
  } catch (e) {
    ElMessage.error('修改失败')
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped>
.profile-page { padding: 20px; }
.page-header { margin-bottom: 20px; }
.profile-card { max-width: 600px; }
</style>