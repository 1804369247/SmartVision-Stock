<template>
  <div class="expiry-warning-page">
    <div class="page-header">
      <h3>效期预警管理</h3>
      <div class="header-actions">
        <el-select v-model="daysThreshold" placeholder="预警天数" size="small" style="width: 120px; margin-right: 8px;">
          <el-option label="7天" :value="7" />
          <el-option label="15天" :value="15" />
          <el-option label="30天" :value="30" />
          <el-option label="60天" :value="60" />
        </el-select>
        <el-button size="small" type="primary" @click="loadData">🔄 查询</el-button>
        <el-button size="small" @click="loadStatistics">📊 统计</el-button>
      </div>
    </div>

    <div class="stats-row" v-if="statistics">
      <el-statistic title="临期商品数量" :value="statistics.expiringCount || 0" suffix="件" class="stat-card" />
      <el-statistic title="已过期商品" :value="statistics.expiredCount || 0" suffix="件" class="stat-card warning" />
      <el-statistic title="预警总数" :value="statistics.alertCount || 0" suffix="条" class="stat-card" />
    </div>

    <div class="search-bar">
      <el-input v-model="searchKeyword" placeholder="搜索货物名称/编码" size="small" clearable @keyup.enter="loadData">
        <template #prefix>🔍</template>
      </el-input>
      <el-select v-model="filterStatus" placeholder="状态筛选" size="small" style="margin-left: 8px;">
        <el-option label="全部" value="" />
        <el-option label="临期预警" :value="1" />
        <el-option label="已过期" :value="2" />
      </el-select>
    </div>

    <el-table :data="tableData" border size="small" class="data-table">
      <el-table-column prop="goodsName" label="货物名称" min-width="150" />
      <el-table-column prop="goodsCode" label="货物编码" width="120" />
      <el-table-column prop="batchNo" label="批次号" width="130" />
      <el-table-column prop="quantity" label="数量" width="70" />
      <el-table-column prop="expiryDate" label="过期日期" width="120">
        <template #default="scope">
          <span :class="{ 'expiry-warning': isExpiringSoon(scope.row.expiryDate) }">
            {{ formatDate(scope.row.expiryDate) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="daysRemaining" label="剩余天数" width="90">
        <template #default="scope">
          <el-tag :type="getDaysTagType(scope.row.daysRemaining)" size="small">
            {{ scope.row.daysRemaining }}天
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="locationCode" label="库位" width="100" />
      <el-table-column prop="supplier" label="供应商" width="120" />
      <el-table-column label="操作" width="150">
        <template #default="scope">
          <el-button size="small" @click="viewDetail(scope.row)">查看详情</el-button>
          <el-button size="small" type="warning" @click="processAlert(scope.row)">处理</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="totalElements > 0"
      :current-page="pagination.page + 1"
      :page-size="pagination.size"
      :total="totalElements"
      @current-change="handlePageChange"
      layout="prev, pager, next, jumper, ->, total"
      class="pagination"
    />

    <el-dialog v-model="showDetail" title="预警详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="货物名称">{{ detailData.goodsName }}</el-descriptions-item>
        <el-descriptions-item label="货物编码">{{ detailData.goodsCode }}</el-descriptions-item>
        <el-descriptions-item label="批次号">{{ detailData.batchNo }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ detailData.quantity }}</el-descriptions-item>
        <el-descriptions-item label="过期日期">{{ formatDate(detailData.expiryDate) }}</el-descriptions-item>
        <el-descriptions-item label="剩余天数">
          <el-tag :type="getDaysTagType(detailData.daysRemaining)" size="small">
            {{ detailData.daysRemaining }}天
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="库位">{{ detailData.locationCode }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ detailData.supplier }}</el-descriptions-item>
        <el-descriptions-item label="入库时间">{{ formatDate(detailData.inTime) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getDaysTagType(detailData.daysRemaining)" size="small">
            {{ detailData.daysRemaining > 0 ? '临期预警' : '已过期' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="showDetail = false">关闭</el-button>
        <el-button type="primary" @click="processAlert(detailData)">处理预警</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showProcess" title="处理预警" width="400px">
      <el-form :model="processForm" label-width="80px">
        <el-form-item label="处理方式">
          <el-select v-model="processForm.action" placeholder="请选择处理方式">
            <el-option label="出库处理" value="OUTBOUND" />
            <el-option label="标记过期" value="MARK_EXPIRED" />
            <el-option label="延期申请" value="EXTEND" />
            <el-option label="忽略预警" value="IGNORE" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="processForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showProcess = false">取消</el-button>
        <el-button type="primary" @click="confirmProcess">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { expiryApi } from '../api'

const daysThreshold = ref(30)
const searchKeyword = ref('')
const filterStatus = ref('')
const tableData = ref([])
const statistics = ref(null)
const pagination = reactive({ page: 0, size: 10 })
const totalElements = ref(0)

const showDetail = ref(false)
const detailData = ref({})
const showProcess = ref(false)
const processForm = reactive({ action: '', remark: '' })

const loadData = async () => {
  try {
    const result = await expiryApi.checkExpiringProducts(daysThreshold.value)
    let data = result.data || result || []
    if (searchKeyword.value) {
      const kw = searchKeyword.value.toLowerCase()
      data = data.filter(item =>
        (item.goodsName && item.goodsName.toLowerCase().includes(kw)) ||
        (item.goodsCode && item.goodsCode.toLowerCase().includes(kw))
      )
    }
    tableData.value = data.slice(pagination.page * pagination.size, (pagination.page + 1) * pagination.size)
    totalElements.value = data.length
  } catch (e) {
    ElMessage.error('加载失败')
  }
}

const loadStatistics = async () => {
  try {
    const result = await expiryApi.getExpiryStatistics()
    statistics.value = result.data || result
  } catch (e) {
    ElMessage.error('获取统计失败')
  }
}

const handlePageChange = (page) => {
  pagination.page = page - 1
  loadData()
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const isExpiringSoon = (date) => {
  if (!date) return false
  const days = Math.ceil((new Date(date) - new Date()) / (1000 * 60 * 60 * 24))
  return days >= 0 && days <= daysThreshold.value
}

const getDaysTagType = (days) => {
  if (days === undefined || days === null) return 'info'
  if (days < 0) return 'danger'
  if (days <= 7) return 'danger'
  if (days <= 15) return 'warning'
  return 'success'
}

const viewDetail = (row) => {
  detailData.value = row
  showDetail.value = true
}

const processAlert = (row) => {
  detailData.value = row
  processForm.action = ''
  processForm.remark = ''
  showProcess.value = true
}

const confirmProcess = async () => {
  if (!processForm.action) {
    ElMessage.warning('请选择处理方式')
    return
  }
  try {
    await expiryApi.processAlert(detailData.value.alertId || detailData.value.id, processForm.action)
    ElMessage.success('处理成功')
    showProcess.value = false
    loadData()
    loadStatistics()
  } catch (e) {
    ElMessage.error('处理失败')
  }
}

onMounted(() => {
  loadData()
  loadStatistics()
})
</script>

<style scoped>
.expiry-warning-page { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.header-actions { display: flex; align-items: center; }
.stats-row { display: flex; gap: 20px; margin-bottom: 20px; }
.stat-card { flex: 1; }
.search-bar { display: flex; margin-bottom: 16px; }
.data-table { margin-bottom: 16px; }
.expiry-warning { color: #f56c6c; font-weight: bold; }
.pagination { text-align: right; }
</style>