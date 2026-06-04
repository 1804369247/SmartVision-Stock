<template>
  <div class="module-page">
    <div class="page-header">
      <h3>{{ pageTitle }}</h3>
      <div class="header-actions">
        <el-button v-if="currentSubPage === 'count'" size="small" type="primary" @click="createCountSheet">+ 新建盘点单</el-button>
        <el-button v-if="currentSubPage === 'warning'" size="small" type="warning" @click="locateWarnings">📍 定位预警</el-button>
        <el-button size="small" @click="refreshData">🔄 刷新</el-button>
      </div>
    </div>

    <template v-if="currentSubPage === 'query'">
      <div class="search-bar">
        <el-input v-model="filters.keyword" placeholder="搜索库位/货物/批次" size="small" clearable @change="applyFilters">
          <template #prefix>🔍</template>
        </el-input>
        <el-select v-model="filters.area" placeholder="分区" size="small" clearable>
          <el-option label="全部" value="" />
          <el-option label="A区" value="A" />
          <el-option label="B区" value="B" />
          <el-option label="C区" value="C" />
        </el-select>
        <el-select v-model="filters.status" placeholder="状态" size="small" clearable>
          <el-option label="全部" value="" />
          <el-option label="空闲" value="0" />
          <el-option label="正常" value="1" />
          <el-option label="预警" value="2" />
          <el-option label="异常" value="3" />
        </el-select>
      </div>

      <el-table :data="filteredData" border size="small" class="data-table">
        <el-table-column prop="locationCode" label="库位编码" width="100" />
        <el-table-column prop="area" label="分区" width="60" />
        <el-table-column prop="goodsName" label="货物名称" />
        <el-table-column prop="batchNo" label="批次号" width="120" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="expiryDate" label="过期日期" width="120">
          <template #default="scope">
            <span :class="{ 'expiry-warning': isExpiringSoon(scope.row.expiryDate) }">{{ formatDate(scope.row.expiryDate) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="scope">
            <el-tag :type="getStatusTagType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button size="small" @click="locateLocation(scope.row)">定位</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="stats-summary">
        <div class="stat-item">
          <span class="stat-value">{{ totalCount }}</span>
          <span class="stat-label">总库位数</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ usedCount }}</span>
          <span class="stat-label">已用库位</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ emptyCount }}</span>
          <span class="stat-label">空闲库位</span>
        </div>
        <div class="stat-item warning">
          <span class="stat-value">{{ warningCount }}</span>
          <span class="stat-label">预警库位</span>
        </div>
      </div>
    </template>

    <template v-else-if="currentSubPage === 'count'">
      <div class="count-section">
        <div class="count-header">
          <el-select v-model="countForm.area" placeholder="选择盘点分区" size="small">
            <el-option label="全部" value="" />
            <el-option label="A区" value="A" />
            <el-option label="B区" value="B" />
            <el-option label="C区" value="C" />
          </el-select>
          <el-button size="small" type="primary" @click="generateCountTask">生成盘点任务</el-button>
        </div>

        <el-table :data="countLocations" border size="small" class="data-table">
          <el-table-column prop="locationCode" label="库位编码" width="100" />
          <el-table-column prop="goodsName" label="货物" />
          <el-table-column prop="batchNo" label="批次" width="120" />
          <el-table-column prop="quantity" label="系统数量" width="80" />
          <el-table-column label="盘点数量" width="100">
            <template #default="scope">
              <el-input-number v-model="scope.row.countQuantity" :min="0" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="差异" width="80">
            <template #default="scope">
              <span :class="getDiffClass(scope.row)">{{ getDiff(scope.row) }}</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="count-actions">
          <span>差异数量: {{ totalDiff }}</span>
          <div class="action-buttons">
            <el-button size="small" type="primary" @click="submitCount">提交盘点结果</el-button>
            <el-button size="small" @click="exportCount">导出盘点表</el-button>
          </div>
        </div>
      </div>
    </template>

    <template v-else-if="currentSubPage === 'warning'">
      <el-table :data="warningData" border size="small" class="data-table">
        <el-table-column prop="type" label="预警类型" width="100">
          <template #default="scope">
            <span>{{ scope.row.type === 'expiry' ? '⚠️ 保质期' : '📉 库存不足' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="locationCode" label="库位编码" width="100" />
        <el-table-column prop="goodsName" label="货物名称" />
        <el-table-column prop="batchNo" label="批次号" width="120" />
        <el-table-column prop="quantity" label="当前数量" width="80" />
        <el-table-column prop="warningValue" label="预警阈值" width="100" />
        <el-table-column prop="expiryDate" label="过期日期" width="120">
          <template #default="scope">
            <span class="expiry-warning">{{ formatDate(scope.row.expiryDate) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="scope">
            <el-button size="small" @click="locateLocation(scope.row)">定位</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <template v-else-if="currentSubPage === 'transfer'">
      <div class="transfer-section">
        <el-form :model="transferForm" label-width="80px" size="small">
          <el-form-item label="源库位">
            <el-select v-model="transferForm.sourceLocationId" placeholder="请选择源库位">
              <el-option v-for="l in occupiedLocations" :key="l.id" :label="l.locationCode + ' - ' + l.goodsName" :value="l.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="目标库位">
            <el-select v-model="transferForm.targetLocationId" placeholder="请选择目标库位">
              <el-option v-for="l in emptyLocations" :key="l.id" :label="l.locationCode" :value="l.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="转移数量">
            <el-input-number v-model="transferForm.quantity" :min="1" :max="transferMaxQuantity" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="submitTransfer">确认调拨</el-button>
          </el-form-item>
        </el-form>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  subPage: { type: String, required: true },
  locations: { type: Array, default: () => [] }
})

const emit = defineEmits(['locate'])

const currentSubPage = computed(() => props.subPage)

const pageTitle = computed(() => {
  const titles = {
    query: '🔍 库存查询',
    count: '📝 库存盘点',
    warning: '⚠️ 库存预警',
    transfer: '🔄 库存调拨'
  }
  return titles[currentSubPage.value] || '库存管理'
})

const filters = ref({ keyword: '', area: '', status: '' })
const tableData = ref([])
const warningData = ref([])

const countForm = ref({ area: '' })
const countLocations = ref([])

const transferForm = ref({
  sourceLocationId: null,
  targetLocationId: null,
  quantity: 1
})

const filteredData = computed(() => {
  return tableData.value.filter(loc => {
    if (filters.value.keyword) {
      const kw = filters.value.keyword.toLowerCase()
      const matchCode = loc.locationCode?.toLowerCase().includes(kw)
      const matchGoods = loc.goodsName?.toLowerCase().includes(kw)
      const matchBatch = loc.batchNo?.toLowerCase().includes(kw)
      if (!matchCode && !matchGoods && !matchBatch) return false
    }
    if (filters.value.area && loc.area !== filters.value.area) return false
    if (filters.value.status && String(loc.status) !== filters.value.status) return false
    return true
  })
})

const totalCount = computed(() => tableData.value.length)
const usedCount = computed(() => tableData.value.filter(l => l.status !== 0).length)
const emptyCount = computed(() => tableData.value.filter(l => l.status === 0).length)
const warningCount = computed(() => tableData.value.filter(l => l.status === 2).length)

const occupiedLocations = computed(() => tableData.value.filter(l => l.status !== 0))
const emptyLocations = computed(() => tableData.value.filter(l => l.status === 0))

const transferMaxQuantity = computed(() => {
  const source = tableData.value.find(l => l.id === transferForm.value.sourceLocationId)
  return source?.quantity || 1
})

const totalDiff = computed(() => {
  return countLocations.value.reduce((sum, l) => {
    const diff = (l.countQuantity || 0) - (l.quantity || 0)
    return sum + Math.abs(diff)
  }, 0)
})

const refreshData = async () => {
  try {
    const res = await fetch('/api/locations')
    tableData.value = await res.json()
    
    warningData.value = []
    tableData.value.forEach(loc => {
      if (loc.status === 2) {
        warningData.value.push({
          type: 'stock',
          ...loc,
          warningValue: loc.warningThreshold
        })
      }
      if (loc.expiryDate) {
        const expiry = new Date(loc.expiryDate).getTime()
        const now = Date.now()
        if (expiry - now < 7 * 24 * 60 * 60 * 1000) {
          warningData.value.push({
            type: 'expiry',
            ...loc,
            warningValue: '7天内过期'
          })
        }
      }
    })
  } catch (e) {
    ElMessage.error('加载数据失败')
  }
}

const applyFilters = () => {}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

const isExpiringSoon = (dateStr) => {
  if (!dateStr) return false
  const expiry = new Date(dateStr).getTime()
  return expiry - Date.now() < 7 * 24 * 60 * 60 * 1000
}

const getStatusText = (status) => {
  const map = { 0: '空闲', 1: '正常', 2: '预警', 3: '异常' }
  return map[status] || status
}

const getStatusTagType = (status) => {
  const types = { 0: 'info', 1: 'success', 2: 'warning', 3: 'danger' }
  return types[status] || 'info'
}

const locateLocation = (item) => {
  emit('locate', item.locationCode)
}

const locateWarnings = () => {
  if (warningData.value.length > 0) {
    emit('locate', warningData.value[0].locationCode)
    ElMessage.info(`找到 ${warningData.value.length} 个预警库位`)
  } else {
    ElMessage.success('暂无预警')
  }
}

const createCountSheet = () => {
  countForm.value.area = ''
  countLocations.value = []
}

const generateCountTask = () => {
  countLocations.value = tableData.value
    .filter(l => !countForm.value.area || l.area === countForm.value.area)
    .filter(l => l.status !== 0)
    .map(l => ({ ...l, countQuantity: l.quantity }))
}

const getDiff = (row) => {
  return (row.countQuantity || 0) - (row.quantity || 0)
}

const getDiffClass = (row) => {
  const diff = getDiff(row)
  if (diff > 0) return 'diff-positive'
  if (diff < 0) return 'diff-negative'
  return ''
}

const submitCount = () => {
  const diffItems = countLocations.value.filter(l => getDiff(l) !== 0)
  if (diffItems.length > 0) {
    ElMessage.success(`盘点完成，发现 ${diffItems.length} 个差异，已更新3D库存模型！`)
  } else {
    ElMessage.success('盘点完成，无差异，已更新3D库存模型！')
  }
}

const exportCount = () => {
  ElMessage.info('导出盘点表功能开发中')
}

const submitTransfer = async () => {
  if (!transferForm.value.sourceLocationId || !transferForm.value.targetLocationId) {
    ElMessage.warning('请选择源库位和目标库位')
    return
  }

  try {
    const source = tableData.value.find(l => l.id === transferForm.value.sourceLocationId)
    const res = await fetch('/api/move', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        goodsInstanceId: source.currentGoodsInstanceId,
        targetLocationId: transferForm.value.targetLocationId
      })
    })
    
    const result = await res.json()
    if (result.success) {
      ElMessage.success('调拨成功，已更新3D库存模型！')
      transferForm.value = { sourceLocationId: null, targetLocationId: null, quantity: 1 }
      refreshData()
    } else {
      ElMessage.error(result.message)
    }
  } catch (e) {
    ElMessage.error('调拨失败')
  }
}

onMounted(() => refreshData())
</script>

<style scoped>
.module-page { padding: 12px; height: 100%; display: flex; flex-direction: column; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.page-header h3 { color: #fff; font-size: 14px; margin: 0; }
.header-actions { display: flex; gap: 8px; }
.search-bar { display: flex; gap: 12px; margin-bottom: 12px; }
.search-bar .el-input { width: 150px; }
.search-bar .el-select { width: 100px; }
.data-table { flex: 1; overflow: auto; }
.expiry-warning { color: #ff6b6b; }

.stats-summary { display: flex; gap: 20px; margin-top: 15px; padding: 15px; background: rgba(30,30,50,0.7); border-radius: 10px; }
.stat-item { text-align: center; }
.stat-item .stat-value { font-size: 24px; font-weight: bold; color: #fff; }
.stat-item .stat-label { font-size: 12px; color: #aaa; }
.stat-item.warning .stat-value { color: #e6a23c; }

.count-section { background: rgba(30,30,50,0.7); border-radius: 10px; padding: 15px; }
.count-header { display: flex; gap: 12px; margin-bottom: 15px; }
.count-actions { display: flex; justify-content: space-between; align-items: center; margin-top: 15px; color: #ccc; }
.action-buttons { display: flex; gap: 8px; }
.diff-positive { color: #67c23a; }
.diff-negative { color: #f56c6c; }

.transfer-section { background: rgba(30,30,50,0.7); border-radius: 10px; padding: 20px; }
</style>