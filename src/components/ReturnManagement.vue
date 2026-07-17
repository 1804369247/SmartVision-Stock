<template>
  <div class="return-management">
    <el-card class="header-card">
      <div class="header">
        <h2>退货管理</h2>
        <el-button type="primary" @click="showCreateDialog = true">新建退货申请</el-button>
      </div>
    </el-card>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-value">{{ statistics.totalReturns || 0 }}</div>
          <div class="stat-label">退货总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-value">{{ statistics.pendingReturns || 0 }}</div>
          <div class="stat-label">待处理</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-value">{{ statistics.inspectedReturns || 0 }}</div>
          <div class="stat-label">已质检</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-value">{{ statistics.totalRefundAmount || 0 }}</div>
          <div class="stat-label">退款总额</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="main-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="退货列表" name="list">
          <div class="search-bar">
            <el-select v-model="statusFilter" placeholder="筛选状态">
              <el-option label="全部" value="" />
              <el-option label="待处理" value="PENDING" />
              <el-option label="已质检" value="INSPECTED" />
              <el-option label="已确认" value="CONFIRMED" />
              <el-option label="已拒绝" value="REJECTED" />
              <el-option label="已退款" value="REFUNDED" />
            </el-select>
            <el-input v-model="searchReturnId" placeholder="搜索退货单号" style="width: 200px" @keyup.enter="loadReturns" />
            <el-button @click="loadReturns">搜索</el-button>
          </div>
          <el-table :data="returnRequests" style="width: 100%">
            <el-table-column prop="returnId" label="退货单号" width="180" />
            <el-table-column prop="orderId" label="关联订单" width="120" />
            <el-table-column prop="customerId" label="客户ID" width="100" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="qualityGrade" label="质检等级" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.qualityGrade" :type="getGradeType(row.qualityGrade)">{{ row.qualityGrade }}</el-tag>
                <span v-else>待质检</span>
              </template>
            </el-table-column>
            <el-table-column prop="refundAmount" label="退款金额" width="100">
              <template #default="{ row }">
                ¥{{ row.refundAmount || 0 }}
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作">
              <template #default="{ row }">
                <el-button size="small" @click="viewReturnDetail(row.returnId)">详情</el-button>
                <el-button v-if="row.status === 'PENDING'" size="small" type="primary" @click="inspectReturn(row.returnId)">质检</el-button>
                <el-button v-if="row.status === 'INSPECTED'" size="small" type="success" @click="confirmReturn(row.returnId)">确认入库</el-button>
                <el-button v-if="row.status === 'PENDING' || row.status === 'INSPECTED'" size="small" type="danger" @click="rejectReturn(row.returnId)">拒绝</el-button>
                <el-button v-if="row.status === 'CONFIRMED'" size="small" type="primary" @click="processRefund(row.returnId)">处理退款</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="退货统计" name="statistics">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-card>
                <h3>状态分布</h3>
                <div class="status-chart">
                  <div v-for="(count, status) in statusStats" :key="status" class="status-item">
                    <span class="status-label">{{ getStatusText(status) }}</span>
                    <div class="status-bar">
                      <div class="status-fill" :style="{ width: getStatusPercent(count) + '%', backgroundColor: getStatusColor(status) }"></div>
                    </div>
                    <span class="status-count">{{ count }}</span>
                  </div>
                </div>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card>
                <h3>质检等级分布</h3>
                <div class="grade-chart">
                  <div v-for="grade in ['A', 'B', 'C', 'D']" :key="grade" class="grade-item">
                    <span class="grade-label">{{ grade }}级</span>
                    <div class="grade-bar">
                      <div class="grade-fill" :style="{ width: getGradePercent(grade) + '%', backgroundColor: getGradeColor(grade) }"></div>
                    </div>
                    <span class="grade-count">{{ gradeStats[grade] || 0 }}</span>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="showCreateDialog" title="新建退货申请" width="600px">
      <el-form :model="returnForm" label-width="100px">
        <el-form-item label="订单ID">
          <el-input v-model="returnForm.orderId" />
        </el-form-item>
        <el-form-item label="客户ID">
          <el-input v-model="returnForm.customerId" />
        </el-form-item>
        <el-form-item label="退货原因">
          <el-select v-model="returnForm.reason">
            <el-option label="商品质量问题" value="quality" />
            <el-option label="商品与描述不符" value="description" />
            <el-option label="商品损坏" value="damage" />
            <el-option label="买家不想要了" value="no_need" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="退货商品">
          <div v-for="(item, index) in returnForm.items" :key="index" class="item-row">
            <el-input v-model="item.sku" placeholder="SKU" style="width: 150px" />
            <el-input v-model="item.name" placeholder="商品名称" style="width: 150px" />
            <el-input v-model="item.quantity" type="number" placeholder="数量" style="width: 100px" />
            <el-input v-model="item.price" type="number" placeholder="单价" style="width: 100px" />
            <el-button v-if="returnForm.items.length > 1" size="small" type="danger" @click="removeItem(index)">删除</el-button>
          </div>
          <el-button size="small" type="primary" @click="addItem">添加商品</el-button>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="returnForm.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="createReturn">提交申请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showInspectDialog" title="质检" width="500px">
      <el-form :model="inspectForm" label-width="100px">
        <el-form-item label="质检等级">
          <el-select v-model="inspectForm.qualityGrade">
            <el-option label="A级 - 完好" value="A" />
            <el-option label="B级 - 轻微瑕疵" value="B" />
            <el-option label="C级 - 明显瑕疵" value="C" />
            <el-option label="D级 - 无法使用" value="D" />
          </el-select>
        </el-form-item>
        <el-form-item label="质检人员">
          <el-input v-model="inspectForm.inspector" />
        </el-form-item>
        <el-form-item label="质检备注">
          <el-input v-model="inspectForm.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showInspectDialog = false">取消</el-button>
        <el-button type="primary" @click="submitInspect">提交质检</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showRejectDialog" title="拒绝退货" width="400px">
      <el-form :model="rejectForm" label-width="80px">
        <el-form-item label="拒绝原因">
          <el-input v-model="rejectForm.reason" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRejectDialog = false">取消</el-button>
        <el-button type="danger" @click="submitReject">确认拒绝</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showDetailDialog" title="退货详情" width="600px">
      <el-descriptions :column="2" border v-if="currentReturn">
        <el-descriptions-item label="退货单号">{{ currentReturn.returnId }}</el-descriptions-item>
        <el-descriptions-item label="关联订单">{{ currentReturn.orderId }}</el-descriptions-item>
        <el-descriptions-item label="客户ID">{{ currentReturn.customerId }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ getStatusText(currentReturn.status) }}</el-descriptions-item>
        <el-descriptions-item label="退货原因">{{ getReasonText(currentReturn.reason) }}</el-descriptions-item>
        <el-descriptions-item label="质检等级" v-if="currentReturn.qualityGrade">{{ currentReturn.qualityGrade }}</el-descriptions-item>
        <el-descriptions-item label="退款金额">¥{{ currentReturn.refundAmount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatTime(currentReturn.createTime) }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="currentReturn?.items" style="width: 100%; margin-top: 20px">
        <el-table-column prop="sku" label="SKU" />
        <el-table-column prop="name" label="商品名称" />
        <el-table-column prop="quantity" label="数量" />
        <el-table-column prop="price" label="单价" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { returnApi } from '../api/return'

const activeTab = ref('list')
const returnRequests = ref([])
const statistics = ref({})
const statusFilter = ref('')
const searchReturnId = ref('')

const showCreateDialog = ref(false)
const showInspectDialog = ref(false)
const showRejectDialog = ref(false)
const showDetailDialog = ref(false)

const currentReturn = ref(null)
const currentReturnId = ref('')

const returnForm = ref({
  orderId: '',
  customerId: '',
  reason: 'quality',
  items: [{ sku: '', name: '', quantity: 1, price: 0 }],
  remark: ''
})

const inspectForm = ref({
  qualityGrade: 'A',
  inspector: '',
  remark: ''
})

const rejectForm = ref({
  reason: ''
})

const statusStats = computed(() => {
  const stats = { PENDING: 0, INSPECTED: 0, CONFIRMED: 0, REJECTED: 0, REFUNDED: 0 }
  returnRequests.value.forEach(r => {
    if (stats[r.status] !== undefined) {
      stats[r.status]++
    }
  })
  return stats
})

const gradeStats = computed(() => {
  const stats = { A: 0, B: 0, C: 0, D: 0 }
  returnRequests.value.forEach(r => {
    if (r.qualityGrade && stats[r.qualityGrade] !== undefined) {
      stats[r.qualityGrade]++
    }
  })
  return stats
})

const formatTime = (timestamp) => {
  if (!timestamp) return ''
  return new Date(timestamp).toLocaleString()
}

const getStatusType = (status) => {
  const types = {
    'PENDING': 'info',
    'INSPECTED': 'warning',
    'CONFIRMED': 'primary',
    'REJECTED': 'danger',
    'REFUNDED': 'success'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    'PENDING': '待处理',
    'INSPECTED': '已质检',
    'CONFIRMED': '已确认',
    'REJECTED': '已拒绝',
    'REFUNDED': '已退款'
  }
  return texts[status] || status
}

const getStatusColor = (status) => {
  const colors = {
    'PENDING': '#909399',
    'INSPECTED': '#e6a23c',
    'CONFIRMED': '#409eff',
    'REJECTED': '#f56c6c',
    'REFUNDED': '#67c23a'
  }
  return colors[status] || '#909399'
}

const getGradeType = (grade) => {
  const types = {
    'A': 'success',
    'B': 'primary',
    'C': 'warning',
    'D': 'danger'
  }
  return types[grade] || 'info'
}

const getGradeColor = (grade) => {
  const colors = {
    'A': '#67c23a',
    'B': '#409eff',
    'C': '#e6a23c',
    'D': '#f56c6c'
  }
  return colors[grade] || '#909399'
}

const getReasonText = (reason) => {
  const texts = {
    'quality': '商品质量问题',
    'description': '商品与描述不符',
    'damage': '商品损坏',
    'no_need': '买家不想要了',
    'other': '其他'
  }
  return texts[reason] || reason
}

const getStatusPercent = (count) => {
  const total = Object.values(statusStats.value).reduce((a, b) => a + b, 0)
  return total > 0 ? (count / total) * 100 : 0
}

const getGradePercent = (grade) => {
  const total = Object.values(gradeStats.value).reduce((a, b) => a + b, 0)
  const count = gradeStats.value[grade] || 0
  return total > 0 ? (count / total) * 100 : 0
}

const loadReturns = async () => {
  try {
    const res = await returnApi.getReturnRequests(statusFilter.value)
    returnRequests.value = res.data?.data || res.data?.content || res.data || []
  } catch (error) {
    returnRequests.value = []
  }
}

const loadStatistics = async () => {
  try {
    const res = await returnApi.getReturnStatistics()
    statistics.value = res.data || {}
  } catch (error) {
    statistics.value = {}
  }
}

const createReturn = async () => {
  if (!returnForm.value.orderId || !returnForm.value.customerId) {
    ElMessage.warning('请填写订单ID和客户ID')
    return
  }
  if (!returnForm.value.items.some(i => i.sku && i.quantity > 0)) {
    ElMessage.warning('请填写退货商品')
    return
  }
  try {
    await returnApi.createReturnRequest({
      orderId: parseInt(returnForm.value.orderId),
      customerId: parseInt(returnForm.value.customerId),
      reason: returnForm.value.reason,
      items: returnForm.value.items.filter(i => i.sku),
      remark: returnForm.value.remark
    })
    ElMessage.success('退货申请创建成功')
    showCreateDialog.value = false
    returnForm.value = {
      orderId: '',
      customerId: '',
      reason: 'quality',
      items: [{ sku: '', name: '', quantity: 1, price: 0 }],
      remark: ''
    }
    loadReturns()
  } catch (error) {
    ElMessage.error('创建失败')
  }
}

const addItem = () => {
  returnForm.value.items.push({ sku: '', name: '', quantity: 1, price: 0 })
}

const removeItem = (index) => {
  returnForm.value.items.splice(index, 1)
}

const viewReturnDetail = async (returnId) => {
  try {
    const res = await returnApi.getReturnRequest(returnId)
    currentReturn.value = res.data
  } catch (error) {
    currentReturn.value = returnRequests.value.find(r => r.returnId === returnId)
  }
  showDetailDialog.value = true
}

const inspectReturn = (returnId) => {
  currentReturnId.value = returnId
  inspectForm.value = { qualityGrade: 'A', inspector: '', remark: '' }
  showInspectDialog.value = true
}

const submitInspect = async () => {
  try {
    await returnApi.inspectReturn(currentReturnId.value, {
      qualityGrade: inspectForm.value.qualityGrade,
      inspector: inspectForm.value.inspector || 'admin',
      remark: inspectForm.value.remark
    })
    ElMessage.success('质检完成')
    showInspectDialog.value = false
    loadReturns()
  } catch (error) {
    ElMessage.error('质检失败')
  }
}

const confirmReturn = async (returnId) => {
  try {
    await returnApi.confirmReturn(returnId)
    ElMessage.success('确认入库成功')
    loadReturns()
  } catch (error) {
    ElMessage.error('确认失败')
  }
}

const rejectReturn = (returnId) => {
  currentReturnId.value = returnId
  rejectForm.value = { reason: '' }
  showRejectDialog.value = true
}

const submitReject = async () => {
  if (!rejectForm.value.reason.trim()) {
    ElMessage.warning('请填写拒绝原因')
    return
  }
  try {
    await returnApi.rejectReturn(currentReturnId.value, rejectForm.value.reason)
    ElMessage.success('已拒绝退货')
    showRejectDialog.value = false
    loadReturns()
  } catch (error) {
    ElMessage.error('拒绝失败')
  }
}

const processRefund = async (returnId) => {
  try {
    await ElMessageBox.confirm('确定要处理退款吗？', {
      title: '提示',
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await returnApi.processRefund(returnId)
    ElMessage.success('退款处理成功')
    loadReturns()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('退款失败')
    }
  }
}

onMounted(() => {
  loadReturns()
  loadStatistics()
})
</script>

<style scoped>
.return-management {
  padding: 16px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-top: 4px;
}

.main-card {
  margin-top: 20px;
}

.search-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  align-items: center;
}

.item-row {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
  align-items: center;
}

.status-chart, .grade-chart {
  padding: 16px;
}

.status-item, .grade-item {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.status-label, .grade-label {
  width: 80px;
  font-size: 14px;
}

.status-bar, .grade-bar {
  flex: 1;
  height: 20px;
  background: #f5f5f5;
  border-radius: 10px;
  overflow: hidden;
  margin: 0 12px;
}

.status-fill, .grade-fill {
  height: 100%;
  transition: width 0.3s;
}

.status-count, .grade-count {
  width: 40px;
  text-align: right;
  font-weight: bold;
}
</style>