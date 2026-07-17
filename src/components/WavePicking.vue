<template>
  <div class="wave-picking">
    <el-card class="header-card">
      <div class="header">
        <h2>波次拣货管理</h2>
        <el-button type="primary" @click="showCreateDialog = true">创建波次</el-button>
      </div>
    </el-card>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-value">{{ activeWaves.length }}</div>
          <div class="stat-label">活动波次</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-value">{{ totalTasks }}</div>
          <div class="stat-label">待处理任务</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-value">{{ completionRate }}%</div>
          <div class="stat-label">完成率</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-value">{{ estimatedTime }}min</div>
          <div class="stat-label">预计时间</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="waves-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="活动波次" name="active">
          <el-table :data="activeWaves" style="width: 100%">
            <el-table-column prop="waveId" label="波次ID" width="180" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="totalItems" label="商品数量" width="100" />
            <el-table-column prop="createTime" label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作">
              <template #default="{ row }">
                <el-button size="small" @click="viewWaveDetail(row.waveId)">查看</el-button>
                <el-button size="small" type="success" @click="optimizePath(row.waveId)">优化路径</el-button>
                <el-button size="small" type="danger" @click="cancelWave(row.waveId)">取消</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="拣货任务" name="tasks">
          <el-table :data="pickTasks" style="width: 100%">
            <el-table-column prop="taskId" label="任务ID" width="220" />
            <el-table-column prop="zone" label="区域" width="80" />
            <el-table-column prop="priority" label="优先级" width="100">
              <template #default="{ row }">
                <el-tag :type="row.priority === 'HIGH' ? 'danger' : 'info'">{{ row.priority }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="商品数">
              <template #default="{ row }">
                {{ row.items?.length || 0 }}
              </template>
            </el-table-column>
            <el-table-column label="操作">
              <template #default="{ row }">
                <el-button size="small" @click="viewTaskDetail(row.taskId)">详情</el-button>
                <el-button size="small" type="success" @click="completeTask(row.taskId)" 
                           :disabled="row.status === 'COMPLETED'">完成</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-card class="path-card" v-if="optimizedPath.length > 0">
      <h3>优化拣货路径</h3>
      <div class="path-info">
        <span>总距离: {{ pathDistance }}m</span>
        <span>预计时间: {{ pathTime }}min</span>
      </div>
      <div class="path-steps">
        <div v-for="(step, index) in optimizedPath" :key="index" class="path-step">
          <span class="step-number">{{ index + 1 }}</span>
          <span class="step-location">{{ step }}</span>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="showCreateDialog" title="创建波次拣货" width="500px">
      <el-form :model="createForm">
        <el-form-item label="选择订单">
          <el-select v-model="createForm.orderIds" multiple placeholder="选择要拣货的订单" style="width: 100%">
            <el-option v-for="order in availableOrders" :key="order.id" :label="order.orderNo" :value="order.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="createWave">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showTaskDialog" title="任务详情" width="600px">
      <el-descriptions :column="2" border v-if="currentTask">
        <el-descriptions-item label="任务ID">{{ currentTask.taskId }}</el-descriptions-item>
        <el-descriptions-item label="波次ID">{{ currentTask.waveId }}</el-descriptions-item>
        <el-descriptions-item label="区域">{{ currentTask.zone }}</el-descriptions-item>
        <el-descriptions-item label="优先级">{{ currentTask.priority }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ currentTask.status }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="currentTask?.items" style="width: 100%; margin-top: 20px">
        <el-table-column prop="sku" label="SKU" width="120" />
        <el-table-column prop="name" label="商品名称" />
        <el-table-column prop="quantity" label="需拣数量" width="100" />
        <el-table-column prop="picked" label="已拣数量" width="100" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pickApi } from '../api/pick'

const activeTab = ref('active')
const activeWaves = ref([])
const pickTasks = ref([])
const optimizedPath = ref([])
const pathDistance = ref(0)
const pathTime = ref(0)
const showCreateDialog = ref(false)
const showTaskDialog = ref(false)
const currentTask = ref(null)
const currentWaveId = ref('')

const createForm = ref({
  orderIds: []
})

const availableOrders = ref([])

const totalTasks = computed(() => pickTasks.value.filter(t => t.status === 'PENDING').length)
const completionRate = computed(() => {
  if (pickTasks.value.length === 0) return 0
  const completed = pickTasks.value.filter(t => t.status === 'COMPLETED').length
  return Math.round((completed / pickTasks.value.length) * 100)
})
const estimatedTime = computed(() => Math.round(pathTime.value ?? activeWaves.value.length * 15))

const getStatusType = (status) => {
  const types = {
    'CREATED': 'primary',
    'IN_PROGRESS': 'warning',
    'COMPLETED': 'success',
    'CANCELLED': 'danger',
    'PENDING': 'info'
  }
  return types[status] || 'info'
}

const formatTime = (timestamp) => {
  if (!timestamp) return ''
  return new Date(timestamp).toLocaleString()
}

const loadActiveWaves = async () => {
  try {
    const res = await pickApi.getActiveWaves()
    activeWaves.value = res.data?.data || res.data?.content || res.data || []
  } catch (error) {
    ElMessage.error('加载活动波次失败')
    activeWaves.value = []
  }
}

const loadPickTasks = async () => {
  try {
    const res = await pickApi.getPickTasks(currentWaveId.value || undefined)
    pickTasks.value = res.data?.data || res.data?.content || res.data || []
  } catch (error) {
    ElMessage.error('加载拣货任务失败')
    pickTasks.value = []
  }
}

const createWave = async () => {
  if (createForm.value.orderIds.length === 0) {
    ElMessage.warning('请选择至少一个订单')
    return
  }
  try {
    await pickApi.createWavePick(createForm.value.orderIds)
    ElMessage.success('波次创建成功')
    showCreateDialog.value = false
    createForm.value.orderIds = []
    loadActiveWaves()
  } catch (error) {
    ElMessage.error('创建波次失败')
  }
}

const viewWaveDetail = async (waveId) => {
  currentWaveId.value = waveId
  try {
    await pickApi.getWavePickDetail(waveId)
    await loadPickTasks()
    activeTab.value = 'tasks'
  } catch (error) {
    ElMessage.error('加载波次详情失败')
  }
}

const optimizePath = async (waveId) => {
  try {
    const res = await pickApi.optimizePickPath(waveId)
    if (res.data?.path) {
      optimizedPath.value = res.data.path
      pathDistance.value = res.data.distance ?? 0
      pathTime.value = res.data.time ?? 0
      ElMessage.success('路径优化完成')
    } else {
      ElMessage.warning('路径优化结果为空，请重试')
    }
  } catch (error) {
    ElMessage.error('路径优化失败')
    optimizedPath.value = []
    pathDistance.value = 0
    pathTime.value = 0
  }
}

const cancelWave = async (waveId) => {
  try {
    await ElMessageBox.confirm('确定要取消该波次吗？', {
      title: '提示',
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await pickApi.cancelWavePick(waveId)
    ElMessage.success('波次已取消')
    loadActiveWaves()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('取消波次失败')
    }
  }
}

const viewTaskDetail = async (taskId) => {
  try {
    const res = await pickApi.getPickTaskDetail(taskId)
    currentTask.value = res.data
  } catch (error) {
    currentTask.value = pickTasks.value.find(t => t.taskId === taskId)
  }
  showTaskDialog.value = true
}

const completeTask = async (taskId) => {
  try {
    await pickApi.completePickTask(taskId, { completed: true })
    ElMessage.success(`任务 ${taskId} 已完成`)
    loadPickTasks()
  } catch (error) {
    ElMessage.error('完成任务失败')
  }
}

onMounted(() => {
  loadActiveWaves()
  loadPickTasks()
})
</script>

<style scoped>
.wave-picking {
  padding: 20px;
}

.header-card {
  margin-bottom: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header h2 {
  margin: 0;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #409EFF;
}

.stat-label {
  color: #666;
  margin-top: 5px;
}

.waves-card {
  margin-bottom: 20px;
}

.path-card {
  margin-bottom: 20px;
}

.path-info {
  display: flex;
  gap: 20px;
  margin-bottom: 15px;
  color: #666;
}

.path-steps {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.path-step {
  display: flex;
  align-items: center;
  background: #f5f7fa;
  padding: 8px 12px;
  border-radius: 4px;
}

.step-number {
  background: #409EFF;
  color: white;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 8px;
  font-size: 12px;
}

.step-location {
  font-weight: 500;
}
</style>