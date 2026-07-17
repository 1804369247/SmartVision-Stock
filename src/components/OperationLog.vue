<template>
  <div class="operation-log">
    <div class="log-header">
      <div class="search-bar">
        <el-input placeholder="操作员" v-model="searchForm.operatorName" class="search-input" clearable />
        <el-select v-model="searchForm.module" placeholder="操作模块" clearable class="search-select">
          <el-option label="库存管理" value="库存管理" />
          <el-option label="订单管理" value="订单管理" />
          <el-option label="库位管理" value="库位管理" />
          <el-option label="商品管理" value="商品管理" />
          <el-option label="系统管理" value="系统管理" />
        </el-select>
        <el-select v-model="searchForm.operationType" placeholder="操作类型" clearable class="search-select">
          <el-option label="新增" value="新增" />
          <el-option label="修改" value="修改" />
          <el-option label="删除" value="删除" />
          <el-option label="查询" value="查询" />
          <el-option label="入库" value="入库" />
          <el-option label="出库" value="出库" />
        </el-select>
        <el-button type="primary" @click="searchLogs">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
        <el-button type="success" @click="exportLogs">导出日志</el-button>
      </div>
    </div>

    <!-- 时间筛选单独一行 -->
    <div class="time-filter">
      <el-date-picker v-model="searchForm.startTime" type="datetime" placeholder="开始时间" value-format="x" />
      <span class="time-sep">至</span>
      <el-date-picker v-model="searchForm.endTime" type="datetime" placeholder="结束时间" value-format="x" />
    </div>
    
    <el-table :data="logList" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="operatorName" label="操作员" width="120" />
      <el-table-column prop="operationType" label="操作类型" width="100" />
      <el-table-column prop="module" label="模块" width="120" />
      <el-table-column prop="detail" label="操作详情" />
      <el-table-column prop="ipAddress" label="IP地址" width="140" />
      <el-table-column prop="operateTime" label="操作时间" width="180" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'SUCCESS' ? 'success' : 'danger'">
            {{ scope.row.status === 'SUCCESS' ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="duration" label="耗时(ms)" width="100" />
      <el-table-column label="操作" width="120">
        <template #default="scope">
          <el-button size="small" @click="viewDetail(scope.row)">详情</el-button>
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
    
    <el-dialog title="日志详情" :visible="showDetail" width="600px" @close="showDetail = false">
      <div v-if="currentLog" class="log-detail">
        <div class="detail-row">
          <span class="label">ID:</span>
          <span>{{ currentLog.id }}</span>
        </div>
        <div class="detail-row">
          <span class="label">操作员:</span>
          <span>{{ currentLog.operatorName }}</span>
        </div>
        <div class="detail-row">
          <span class="label">操作类型:</span>
          <span>{{ currentLog.operationType }}</span>
        </div>
        <div class="detail-row">
          <span class="label">模块:</span>
          <span>{{ currentLog.module }}</span>
        </div>
        <div class="detail-row">
          <span class="label">操作详情:</span>
          <span>{{ currentLog.detail }}</span>
        </div>
        <div class="detail-row">
          <span class="label">目标ID:</span>
          <span>{{ currentLog.targetId || '-' }}</span>
        </div>
        <div class="detail-row">
          <span class="label">IP地址:</span>
          <span>{{ currentLog.ipAddress }}</span>
        </div>
        <div class="detail-row">
          <span class="label">操作时间:</span>
          <span>{{ currentLog.operateTime }}</span>
        </div>
        <div class="detail-row">
          <span class="label">URL:</span>
          <span>{{ currentLog.url }}</span>
        </div>
        <div class="detail-row">
          <span class="label">方法:</span>
          <span>{{ currentLog.method }}</span>
        </div>
        <div class="detail-row">
          <span class="label">状态:</span>
          <span :class="currentLog.status === 'SUCCESS' ? 'success' : 'error'">
            {{ currentLog.status === 'SUCCESS' ? '成功' : '失败' }}
          </span>
        </div>
        <div class="detail-row">
          <span class="label">耗时:</span>
          <span>{{ currentLog.duration }}ms</span>
        </div>
        <div class="detail-section">
          <span class="label">请求参数:</span>
          <pre>{{ formatJson(currentLog.params) }}</pre>
        </div>
        <div class="detail-section">
          <span class="label">响应结果:</span>
          <pre>{{ formatJson(currentLog.result) }}</pre>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { logApi } from '../api'

const logList = ref([])
const showDetail = ref(false)
const currentLog = ref(null)

const searchForm = reactive({
  operatorName: '',
  module: '',
  operationType: '',
  startTime: '',
  endTime: ''
})

const pagination = reactive({ page: 1, size: 10, total: 0 })

const loadLogs = async (page = 1) => {
  try {
    const params = {
      page: page - 1,
      size: pagination.size,
      module: searchForm.module || undefined,
    }

    const res = await logApi.getOperationLogs(params)
    // 后端返回 Spring Data Page 格式: { content: [...], totalElements: N }
    const data = res.data || res
    if (Array.isArray(data?.content)) {
      logList.value = data.content.map(log => ({
        ...log,
        operateTime: formatDateTime(log.operateTime),
        operatorName: log.operatorName || log.operatorId || '-',
        operationType: log.operationType || '-',
        module: log.module || '-',
        detail: log.detail || '-',
      }))
      pagination.total = data.totalElements || data.content.length
    } else if (Array.isArray(data)) {
      // 兼容直接返回数组的情况
      logList.value = data.map(log => ({
        ...log,
        operateTime: formatDateTime(log.operateTime),
      }))
      pagination.total = data.length
    } else {
      logList.value = []
      pagination.total = 0
    }
    pagination.page = page
  } catch (e) {
    console.error('加载日志失败:', e)
    logList.value = []
    pagination.total = 0
  }
}

const searchLogs = () => loadLogs(1)

const resetSearch = () => {
  searchForm.operatorName = ''
  searchForm.module = ''
  searchForm.operationType = ''
  searchForm.startTime = ''
  searchForm.endTime = ''
  loadLogs(1)
}

const handlePageChange = (page) => loadLogs(page)

const viewDetail = (log) => {
  currentLog.value = log
  showDetail.value = true
}

const exportLogs = async () => {
  const params = {
    module: searchForm.module || undefined,
  }

  try {
    const response = await logApi.exportLogs(params)
    const blob = new Blob([response], { type: 'text/csv; charset=UTF-8' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `operation_logs_${new Date().getTime()}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('日志导出成功')
  } catch (e) {
    ElMessage.error('导出失败: ' + (e.message || '未知错误'))
  }
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  try {
    const date = new Date(dateTime)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  } catch {
    return dateTime
  }
}

const formatJson = (jsonStr) => {
  if (!jsonStr) return '-'
  try {
    const obj = JSON.parse(jsonStr)
    return JSON.stringify(obj, null, 2)
  } catch {
    return jsonStr
  }
}

onMounted(() => {
  loadLogs()
})
</script>

<style scoped>
.operation-log {
  padding: 16px;
  max-width: 100%;
  box-sizing: border-box;
  overflow-x: hidden;
}

.log-header {
  margin-bottom: 12px;
}

.search-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.search-input {
  width: 130px;
  flex-shrink: 0;
}

.search-select {
  width: 130px;
  flex-shrink: 0;
}

.time-filter {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.time-sep {
  color: #999;
  font-size: 13px;
}

.pagination {
  margin-top: 16px;
  text-align: right;
}

.log-detail {
  padding: 16px;
}

.detail-row {
  display: flex;
  padding: 8px 0;
  border-bottom: 1px solid #eee;
}

.detail-row .label {
  width: 100px;
  font-weight: bold;
  color: #666;
}

.detail-row .success {
  color: #67c23a;
}

.detail-row .error {
  color: #f56c6c;
}

.detail-section {
  margin-top: 16px;
}

.detail-section .label {
  display: block;
  font-weight: bold;
  color: #666;
  margin-bottom: 8px;
}

.detail-section pre {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  max-height: 200px;
  overflow-y: auto;
  font-size: 12px;
}
</style>