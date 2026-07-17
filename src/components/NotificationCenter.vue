<template>
  <div class="notification-center-page">
    <div class="page-header">
      <h3>消息通知</h3>
      <div class="header-actions">
        <el-button size="small" type="primary" @click="loadData">🔄 刷新</el-button>
        <el-button size="small" type="warning" @click="markAllRead">✅ 全部已读</el-button>
        <el-button size="small" type="danger" @click="clearAll">🗑️ 清空</el-button>
      </div>
    </div>

    <div class="stats-row">
      <el-statistic title="全部消息" :value="totalElements || 0" class="stat-card" />
      <el-statistic title="未读消息" :value="unreadCount || 0" class="stat-card warning" />
    </div>

    <div class="filter-bar">
      <el-select v-model="filterType" placeholder="类型筛选" size="small" style="width: 140px;">
        <el-option label="全部" value="" />
        <el-option label="效期预警" value="EXPIRY_WARNING" />
        <el-option label="低库存预警" value="LOW_STOCK" />
        <el-option label="订单审核" value="ORDER_APPROVAL" />
        <el-option label="系统通知" value="SYSTEM" />
        <el-option label="盘点通知" value="STOCK_COUNT" />
      </el-select>
      <el-select v-model="filterRead" placeholder="阅读状态" size="small" style="margin-left: 8px;">
        <el-option label="全部" value="" />
        <el-option label="未读" :value="false" />
        <el-option label="已读" :value="true" />
      </el-select>
    </div>

    <div class="notification-list">
      <div
        v-for="item in tableData"
        :key="item.id"
        class="notification-item"
        :class="{ 'unread': !item.isRead }"
        @click="viewNotification(item)"
      >
        <div class="notification-icon">
          <span>{{ getNotificationIcon(item.type) }}</span>
        </div>
        <div class="notification-content">
          <div class="notification-title">{{ item.title }}</div>
          <div class="notification-message">{{ item.message }}</div>
          <div class="notification-meta">
            <span class="notification-type">{{ getNotificationTypeName(item.type) }}</span>
            <span class="notification-time">{{ formatTime(item.createTime) }}</span>
          </div>
        </div>
        <div class="notification-actions">
          <el-button size="small" @click.stop="markRead(item.id)">已读</el-button>
          <el-button size="small" type="danger" @click.stop="deleteNotification(item.id)">删除</el-button>
        </div>
      </div>
    </div>

    <el-pagination
      v-if="totalElements > 0"
      :current-page="pagination.page + 1"
      :page-size="pagination.size"
      :total="totalElements"
      @current-change="handlePageChange"
      layout="prev, pager, next, jumper, ->, total"
      class="pagination"
    />

    <el-dialog v-model="showDetail" title="通知详情" width="500px">
      <div class="detail-content">
        <div class="detail-header">
          <span class="detail-icon">{{ getNotificationIcon(detailData.type) }}</span>
          <span class="detail-title">{{ detailData.title }}</span>
        </div>
        <div class="detail-body">
          <p>{{ detailData.message }}</p>
        </div>
        <div class="detail-footer">
          <span class="detail-type">{{ getNotificationTypeName(detailData.type) }}</span>
          <span class="detail-time">{{ formatTime(detailData.createTime) }}</span>
        </div>
        <div v-if="detailData.data" class="detail-data">
          <h4>相关数据</h4>
          <pre>{{ JSON.stringify(detailData.data, null, 2) }}</pre>
        </div>
      </div>
      <template #footer>
        <el-button @click="showDetail = false">关闭</el-button>
        <el-button type="primary" @click="markRead(detailData.id)">标记已读</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { notificationApi } from '../api'

const filterType = ref('')
const filterRead = ref('')
const tableData = ref([])
const pagination = reactive({ page: 0, size: 10 })
const totalElements = ref(0)
const unreadCount = ref(0)

const showDetail = ref(false)
const detailData = ref({})

const userId = ref(1)

const loadData = async () => {
  try {
    const params = {
      userId: userId.value,
      page: pagination.page,
      size: pagination.size
    }
    if (filterType.value) params.type = filterType.value
    if (filterRead.value !== '') params.isRead = filterRead.value

    const result = await notificationApi.getList(userId.value, params)
    const data = result.data || result
    tableData.value = data.content || data
    totalElements.value = data.totalElements || data.length || 0
    unreadCount.value = data.unreadCount || 0
  } catch (e) {
    ElMessage.error('加载失败')
  }
}

const handlePageChange = (page) => {
  pagination.page = page - 1
  loadData()
}

const getNotificationIcon = (type) => {
  const icons = {
    EXPIRY_WARNING: '⏰',
    LOW_STOCK: '📦',
    ORDER_APPROVAL: '📝',
    SYSTEM: '📢',
    STOCK_COUNT: '🔍'
  }
  return icons[type] || '📩'
}

const getNotificationTypeName = (type) => {
  const names = {
    EXPIRY_WARNING: '效期预警',
    LOW_STOCK: '低库存预警',
    ORDER_APPROVAL: '订单审核',
    SYSTEM: '系统通知',
    STOCK_COUNT: '盘点通知'
  }
  return names[type] || '通知'
}

const formatTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
  
  return date.toLocaleDateString('zh-CN')
}

const viewNotification = async (item) => {
  detailData.value = item
  showDetail.value = true
  if (!item.isRead) {
    await markRead(item.id)
  }
}

const markRead = async (id) => {
  try {
    await notificationApi.markAsRead(id)
    loadData()
  } catch (e) {
    ElMessage.error('标记失败')
  }
}

const markAllRead = async () => {
  try {
    await notificationApi.markAllAsRead(userId.value)
    ElMessage.success('全部已读')
    loadData()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const deleteNotification = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这条通知吗？', '提示', { type: 'warning' })
    await notificationApi.delete(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

const clearAll = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有通知吗？', '提示', { type: 'warning' })
    for (const item of tableData.value) {
      await notificationApi.delete(item.id)
    }
    ElMessage.success('清空成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.notification-center-page { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.header-actions { display: flex; gap: 8px; }
.stats-row { display: flex; gap: 20px; margin-bottom: 20px; }
.stat-card { flex: 1; }
.filter-bar { display: flex; margin-bottom: 16px; }
.notification-list { max-height: 600px; overflow-y: auto; }
.notification-item {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.3s;
}
.notification-item:hover { border-color: #409eff; box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1); }
.notification-item.unread { background-color: #f0f5ff; border-left: 4px solid #409eff; }
.notification-icon { width: 40px; height: 40px; display: flex; align-items: center; justify-content: center; font-size: 24px; margin-right: 16px; }
.notification-content { flex: 1; }
.notification-title { font-weight: bold; margin-bottom: 4px; }
.notification-message { color: #606266; font-size: 14px; margin-bottom: 8px; }
.notification-meta { display: flex; gap: 12px; font-size: 12px; color: #909399; }
.notification-actions { display: flex; gap: 8px; }
.detail-content { padding: 10px; }
.detail-header { display: flex; align-items: center; margin-bottom: 16px; }
.detail-icon { font-size: 28px; margin-right: 12px; }
.detail-title { font-size: 18px; font-weight: bold; }
.detail-body { margin-bottom: 16px; }
.detail-footer { display: flex; justify-content: space-between; color: #909399; font-size: 12px; margin-bottom: 16px; }
.detail-data { background-color: #f5f7fa; padding: 12px; border-radius: 4px; }
.detail-data pre { white-space: pre-wrap; word-break: break-all; }
.pagination { text-align: right; margin-top: 20px; }
</style>