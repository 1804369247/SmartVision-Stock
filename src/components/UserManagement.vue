<template>
  <div class="user-management">
    <el-tabs v-model="activeTab" type="card">
      <el-tab-pane label="用户管理" name="users">
        <div class="user-header">
          <div class="search-bar">
            <el-input placeholder="搜索用户名" v-model="searchForm.username" @keyup.enter="searchUsers" class="search-input">
              <template #append>
                <el-button @click="searchUsers">搜索</el-button>
              </template>
            </el-input>
            <el-select v-model="searchForm.roleId" placeholder="选择角色" clearable>
              <el-option v-for="role in roles" :key="role.id" :label="role.name" :value="role.id" />
            </el-select>
          </div>
          <el-button type="primary" @click="showUserDialog = true">新增用户</el-button>
        </div>
        
        <el-table :data="userList" border>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="realName" label="真实姓名" />
          <el-table-column prop="email" label="邮箱" />
          <el-table-column prop="phone" label="手机号" />
          <el-table-column prop="roleName" label="角色" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
                {{ scope.row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" width="200">
            <template #default="scope">
              <el-button size="small" @click="editUser(scope.row)">编辑</el-button>
              <el-button size="small" @click="resetUserPassword(scope.row)">重置密码</el-button>
              <el-button size="small" :type="scope.row.status === 1 ? 'warning' : 'success'" 
                        @click="toggleUserStatus(scope.row)">
                {{ scope.row.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button size="small" type="danger" @click="deleteUser(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <el-pagination 
          :current-page="pagination.page" 
          :page-size="pagination.size"
          :total="pagination.total"
          @current-change="handlePageChange"
          layout="prev, pager, next, jumper, ->, total"
          class="pagination"
        />
      </el-tab-pane>
      
      <el-tab-pane label="角色管理" name="roles">
        <div class="role-header">
          <div class="search-bar">
            <el-input placeholder="搜索角色名称" v-model="roleSearch" @keyup.enter="searchRoles" class="search-input">
              <template #append>
                <el-button @click="searchRoles">搜索</el-button>
              </template>
            </el-input>
          </div>
          <el-button type="primary" @click="showRoleDialog = true">新增角色</el-button>
        </div>
        
        <el-table :data="roleList" border>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="name" label="角色名称" />
          <el-table-column prop="description" label="描述" />
          <el-table-column prop="dataScope" label="数据范围" width="120">
            <template #default="scope">
              {{ scope.row.dataScope === 'ALL' ? '全部数据' : scope.row.dataScope === 'SELF' ? '本人数据' : scope.row.dataScope }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
                {{ scope.row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" width="200">
            <template #default="scope">
              <el-button size="small" @click="editRole(scope.row)">编辑</el-button>
              <el-button size="small" :type="scope.row.status === 1 ? 'warning' : 'success'" 
                        @click="toggleRoleStatus(scope.row)">
                {{ scope.row.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button size="small" type="danger" @click="deleteRole(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <el-pagination 
          :current-page="rolePagination.page" 
          :page-size="rolePagination.size"
          :total="rolePagination.total"
          @current-change="handleRolePageChange"
          layout="prev, pager, next, jumper, ->, total"
          class="pagination"
        />
      </el-tab-pane>
      
      <el-tab-pane label="操作日志" name="logs">
        <OperationLog />
      </el-tab-pane>
    </el-tabs>
    
    <el-dialog v-model="showUserDialog" title="用户信息" @close="resetUserForm">
      <el-form :model="userForm" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!userForm.id">
          <el-input v-model="userForm.password" type="password" placeholder="不填默认为123456" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="userForm.realName" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="userForm.roleId" placeholder="请选择角色">
            <el-option v-for="role in roles" :key="role.id" :label="role.name" :value="role.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUserDialog = false">取消</el-button>
        <el-button type="primary" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>
    
    <el-dialog v-model="showRoleDialog" title="角色信息" @close="resetRoleForm">
      <el-form :model="roleForm" label-width="100px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="roleForm.name" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="roleForm.description" />
        </el-form-item>
        <el-form-item label="数据范围" prop="dataScope">
          <el-select v-model="roleForm.dataScope">
            <el-option label="全部数据" value="ALL" />
            <el-option label="本人数据" value="SELF" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRoleDialog = false">取消</el-button>
        <el-button type="primary" @click="saveRole">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { systemApi } from '../api'
import OperationLog from './OperationLog.vue'

const activeTab = ref('users')
const userList = ref([])
const roleList = ref([])
const roles = ref([])

const searchForm = reactive({ username: '', roleId: '' })
const roleSearch = ref('')

const pagination = reactive({ page: 1, size: 10, total: 0 })
const rolePagination = reactive({ page: 1, size: 10, total: 0 })

const showUserDialog = ref(false)
const showRoleDialog = ref(false)

const userForm = reactive({
  id: null,
  username: '',
  password: '',
  realName: '',
  email: '',
  phone: '',
  roleId: null
})

const roleForm = reactive({
  id: null,
  name: '',
  description: '',
  dataScope: 'ALL'
})

const loadUsers = async (page = 1) => {
  const params = { page: page - 1, size: pagination.size, ...searchForm }
  const res = await systemApi.getUsers(params)
  if (res && res.code === 200) {
    userList.value = (res.data?.data || res.data?.content || []).map(u => ({
      ...u,
      roleName: roles.value.find(r => r.id === u.roleId)?.name || ''
    }))
    pagination.total = res.data?.total || 0
    pagination.page = page
  }
}

const loadRoles = async () => {
  const res = await systemApi.getAllRoles({ size: 1000 })
  if (res && res.code === 200) {
    roles.value = res.data?.data || res.data?.content || res.data || []
  }
}

const loadRoleList = async (page = 1) => {
  const params = { page: page - 1, size: rolePagination.size, name: roleSearch.value }
  const res = await systemApi.getRoles(params)
  if (res && res.code === 200) {
    roleList.value = res.data?.data || res.data || []
    rolePagination.total = res.data?.total || 0
    rolePagination.page = page
  }
}

const searchUsers = () => loadUsers(1)
const searchRoles = () => loadRoleList(1)

const handlePageChange = (page) => loadUsers(page)
const handleRolePageChange = (page) => loadRoleList(page)

const resetUserForm = () => {
  userForm.id = null
  userForm.username = ''
  userForm.password = ''
  userForm.realName = ''
  userForm.email = ''
  userForm.phone = ''
  userForm.roleId = null
}

const resetRoleForm = () => {
  roleForm.id = null
  roleForm.name = ''
  roleForm.description = ''
  roleForm.dataScope = 'ALL'
}

const editUser = (user) => {
  userForm.id = user.id
  userForm.username = user.username
  userForm.realName = user.realName
  userForm.email = user.email
  userForm.phone = user.phone
  userForm.roleId = user.roleId
  showUserDialog.value = true
}

const saveUser = async () => {
  if (!userForm.username) {
    ElMessage.warning('请填写用户名')
    return
  }
  
  const data = { ...userForm }
  if (!data.password) delete data.password
  
  let res
  if (userForm.id) {
    res = await systemApi.updateUser(userForm.id, data)
  } else {
    res = await systemApi.createUser(data)
  }
  
  if (res && res.code === 200) {
    ElMessage.success(userForm.id ? '更新成功' : '创建成功')
    showUserDialog.value = false
    resetUserForm()
    loadUsers()
  } else {
    ElMessage.error(res.message)
  }
}

const deleteUser = async (user) => {
  const res = await systemApi.deleteUser(user.id)
  if (res && res.code === 200) {
    ElMessage.success('删除成功')
    loadUsers()
  } else {
    ElMessage.error(res.message)
  }
}

const resetUserPassword = async (user) => {
  const res = await systemApi.resetPassword(user.id)
  if (res && res.code === 200) {
    ElMessage.success('密码已重置为123456')
  } else {
    ElMessage.error(res.message)
  }
}

const toggleUserStatus = async (user) => {
  const newStatus = user.status === 1 ? 0 : 1
  const res = await systemApi.updateUserStatus(user.id, newStatus)
  if (res && res.code === 200) {
    ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
    loadUsers()
  } else {
    ElMessage.error(res.message)
  }
}

const editRole = (role) => {
  roleForm.id = role.id
  roleForm.name = role.name
  roleForm.description = role.description
  roleForm.dataScope = role.dataScope || 'ALL'
  showRoleDialog.value = true
}

const saveRole = async () => {
  if (!roleForm.name) {
    ElMessage.warning('请填写角色名称')
    return
  }
  
  let res
  if (roleForm.id) {
    res = await systemApi.updateRole(roleForm.id, roleForm)
  } else {
    res = await systemApi.createRole(roleForm)
  }
  
  if (res && res.code === 200) {
    ElMessage.success(roleForm.id ? '更新成功' : '创建成功')
    showRoleDialog.value = false
    resetRoleForm()
    loadRoleList()
    loadRoles()
  } else {
    ElMessage.error(res.message)
  }
}

const deleteRole = async (role) => {
  const res = await systemApi.deleteRole(role.id)
  if (res && res.code === 200) {
    ElMessage.success('删除成功')
    loadRoleList()
    loadRoles()
  } else {
    ElMessage.error(res.message)
  }
}

const toggleRoleStatus = async (role) => {
  const newStatus = role.status === 1 ? 0 : 1
  const res = await systemApi.updateRoleStatus(role.id, newStatus)
  if (res && res.code === 200) {
    ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
    loadRoleList()
  } else {
    ElMessage.error(res.message)
  }
}

onMounted(() => {
  loadRoles()
  loadUsers()
  loadRoleList()
})
</script>

<style scoped>
.user-management {
  padding: 16px;
}

.user-header, .role-header, .log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.search-bar {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-input {
  width: 200px;
}

.pagination {
  margin-top: 16px;
  text-align: right;
}

.el-date-picker {
  width: 200px;
}
</style>