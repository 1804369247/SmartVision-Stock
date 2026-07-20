<template>
  <div class="report-center">
    <div class="report-layout">
      <!-- 左侧菜单 -->
      <div class="report-menu">
        <div class="menu-title">📊 报表分类</div>
        <el-menu
          :default-active="currentReport"
          class="menu-list"
          @select="selectReport"
        >
          <el-menu-item index="inventory">
            <span class="menu-item">📦 库存台账</span>
          </el-menu-item>
          <el-menu-item index="flow">
            <span class="menu-item">📋 出入库流水</span>
          </el-menu-item>
          <el-menu-item index="utilization">
            <span class="menu-item">📊 库位利用率</span>
          </el-menu-item>
          <el-menu-item index="alert">
            <span class="menu-item">⚠️ 预警报表</span>
          </el-menu-item>
        </el-menu>
      </div>

      <!-- 右侧内容 -->
      <div class="report-content">
        <!-- 筛选区域 -->
        <div class="filter-section">
          <div class="filter-row">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              size="small"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              style="width: 280px;"
              :disabled="currentReport === 'inventory' || currentReport === 'alert'"
            />
            <el-select v-model="selectedArea" placeholder="选择库区" clearable size="small" style="width: 120px;" @change="loadReportData">
              <el-option label="全部" value="" />
              <el-option label="A区" value="A" />
              <el-option label="B区" value="B" />
              <el-option label="C区" value="C" />
            </el-select>
            <el-input
              v-if="currentReport === 'inventory' || currentReport === 'flow'"
              v-model="searchKeyword"
              placeholder="搜索货物名称"
              size="small"
              style="width: 150px;"
              clearable
              @change="loadReportData"
            />
            <el-select v-if="currentReport === 'flow'" v-model="selectedType" placeholder="出入库类型" clearable size="small" style="width: 120px;" @change="loadReportData">
              <el-option label="全部" value="" />
              <el-option label="入库" value="IN" />
              <el-option label="出库" value="OUT" />
              <el-option label="移库" value="MOVE" />
            </el-select>
            <el-button type="primary" size="small" @click="loadReportData">
              <el-icon class="el-icon--left"><Search /></el-icon>
              查询
            </el-button>
            <el-button size="small" @click="resetFilter">重置</el-button>
          </div>
        </div>

        <!-- 报表标题 + KPI -->
        <div class="report-header">
          <div class="header-left">
            <h4>{{ reportTitle }}</h4>
            <div class="kpi-tags" v-if="kpiData">
              <el-tag v-if="currentReport === 'inventory' || currentReport === 'utilization'" type="success" size="small">库位 {{ kpiData.totalLocations }} 个</el-tag>
              <el-tag v-if="currentReport === 'inventory' || currentReport === 'utilization'" type="warning" size="small">已用 {{ kpiData.usedLocations }} 个</el-tag>
              <el-tag v-if="currentReport === 'inventory'" type="info" size="small">在库 {{ kpiData.totalQuantity }} 件</el-tag>
              <el-tag v-if="currentReport === 'flow'" type="success" size="small">今日入库 {{ kpiData.todayInbound || 0 }} 次</el-tag>
              <el-tag v-if="currentReport === 'flow'" type="danger" size="small">今日出库 {{ kpiData.todayOutbound || 0 }} 次</el-tag>
              <el-tag v-if="currentReport === 'flow'" type="info" size="small">移库 {{ kpiData.todayMove || 0 }} 次</el-tag>
              <el-tag v-if="currentReport === 'alert'" type="danger" size="small">预警 {{ kpiData.totalAlerts || 0 }} 条</el-tag>
              <el-tag v-if="currentReport === 'alert'" type="warning" size="small">库存预警 {{ kpiData.stockWarningCount || 0 }}</el-tag>
              <el-tag v-if="currentReport === 'alert'" type="danger" size="small">过期预警 {{ kpiData.expiryWarningCount || 0 }}</el-tag>
            </div>
          </div>
          <div class="report-actions">
            <el-button type="primary" size="small" @click="exportReport">
              <el-icon class="el-icon--left"><Download /></el-icon>
              导出 Excel
            </el-button>
            <el-button size="small" @click="printReport">
              <el-icon class="el-icon--left"><Printer /></el-icon>
              打印
            </el-button>
          </div>
        </div>

        <!-- 加载状态 -->
        <div v-if="loading" class="loading-mask">
          <el-icon class="is-loading" style="font-size: 32px; color: #409eff;"><Loading /></el-icon>
          <span style="margin-top: 8px; color: #aaa;">加载中...</span>
        </div>

        <!-- 报表数据 -->
        <div class="report-table" v-else>
          <el-table
            :data="reportData"
            border
            size="small"
            max-height="420"
            :empty-text="emptyText"
          >
            <!-- 库存台账 -->
            <template v-if="currentReport === 'inventory'">
              <el-table-column prop="locationCode" label="库位编码" width="100" fixed />
              <el-table-column prop="area" label="分区" width="60" />
              <el-table-column prop="attribute" label="属性" width="80">
                <template #default="scope">{{ getAttributeText(scope.row.attribute) }}</template>
              </el-table-column>
              <el-table-column prop="goodsName" label="货物名称" min-width="120" />
              <el-table-column prop="goodsCode" label="货物编码" width="100" />
              <el-table-column prop="batchNo" label="批次号" width="130" />
              <el-table-column prop="quantity" label="数量" width="70" />
              <el-table-column prop="inTime" label="入库时间" width="150">
                <template #default="scope">{{ formatDate(scope.row.inTime) }}</template>
              </el-table-column>
              <el-table-column prop="expiryDate" label="过期时间" width="120">
                <template #default="scope">
                  <span :class="{ 'expiry-warning': isExpiringSoon(scope.row.expiryDate) }">
                    {{ formatDate(scope.row.expiryDate) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="80">
                <template #default="scope">
                  <el-tag :type="getStatusTag(scope.row.status)" size="small">
                    {{ getStatusText(scope.row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
            </template>

            <!-- 出入库流水 -->
            <template v-else-if="currentReport === 'flow'">
              <el-table-column prop="orderNo" label="单号" width="150" />
              <el-table-column prop="type" label="类型" width="80">
                <template #default="scope">
                  <el-tag :type="getTypeTag(scope.row.type)" size="small">
                    {{ getTypeText(scope.row.type) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="goodsName" label="货物名称" min-width="120" />
              <el-table-column prop="goodsCode" label="货物编码" width="100" />
              <el-table-column prop="batchNo" label="批次号" width="130" />
              <el-table-column prop="quantity" label="数量" width="70" />
              <el-table-column prop="fromLocationCode" label="源库位" width="100">
                <template #default="scope">{{ scope.row.fromLocationCode || '-' }}</template>
              </el-table-column>
              <el-table-column prop="toLocationCode" label="目标库位" width="100">
                <template #default="scope">{{ scope.row.toLocationCode || '-' }}</template>
              </el-table-column>
              <el-table-column prop="operator" label="操作人" width="100" />
              <el-table-column prop="operateTime" label="时间" width="150">
                <template #default="scope">{{ formatDate(scope.row.operateTime) }}</template>
              </el-table-column>
            </template>

            <!-- 库位利用率 -->
            <template v-else-if="currentReport === 'utilization'">
              <el-table-column prop="area" label="库区" width="80" />
              <el-table-column prop="totalLocations" label="总库位" width="90" />
              <el-table-column prop="usedLocations" label="已使用" width="90" />
              <el-table-column prop="emptyLocations" label="空闲" width="80" />
              <el-table-column prop="frozenLocations" label="已冻结" width="80" />
              <el-table-column prop="utilizationRate" label="利用率" width="90">
                <template #default="scope">
                  <el-progress
                    :percentage="parseFloat(scope.row.utilizationRate)"
                    :color="getProgressColor(scope.row.utilizationRate)"
                    :stroke-width="8"
                    :show-text="true"
                  />
                </template>
              </el-table-column>
              <el-table-column prop="totalQuantity" label="库存量" width="90" />
            </template>

            <!-- 预警报表 -->
            <template v-else-if="currentReport === 'alert'">
              <el-table-column prop="type" label="预警类型" width="100">
                <template #default="scope">
                  <el-tag :type="getAlertTagType(scope.row.level)" size="small">
                    {{ scope.row.type }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="locationCode" label="库位" width="100" />
              <el-table-column prop="goodsName" label="货物名称" min-width="120" />
              <el-table-column prop="goodsCode" label="货物编码" width="100" />
              <el-table-column prop="quantity" label="当前库存" width="90" />
              <el-table-column prop="warningThreshold" label="预警阈值" width="100" />
              <el-table-column prop="expiryDate" label="过期时间" width="120">
                <template #default="scope">
                  <span :class="{ 'expiry-warning': scope.row.type === '保质期预警' || scope.row.type === '已过期' }">
                    {{ scope.row.expiryDate || '-' }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="frozenReason" label="备注" min-width="120">
                <template #default="scope">{{ scope.row.frozenReason || '-' }}</template>
              </el-table-column>
            </template>
          </el-table>

          <!-- 分页 -->
          <div class="pagination" v-if="currentReport === 'inventory' || currentReport === 'flow'">
            <el-pagination
              v-model:current-page="pagination.page"
              v-model:page-size="pagination.size"
              :total="pagination.total"
              :page-sizes="[20, 50, 100, 200]"
              layout="total, sizes, prev, pager, next"
              @size-change="loadReportData"
              @current-change="loadReportData"
            />
            <span class="pagination-info">共 {{ pagination.total }} 条</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Download, Printer, Loading } from '@element-plus/icons-vue'
import { reportApi } from '@/api/report'
import { exportApi } from '@/api/export'

const currentReport = ref('inventory')
const dateRange = ref([])
const selectedArea = ref('')
const selectedType = ref('')
const searchKeyword = ref('')
const reportData = ref([])
const loading = ref(false)
const emptyText = ref('暂无数据')

const pagination = ref({
  page: 0,
  size: 50,
  total: 0
})

const kpiData = ref(null)

const reportTitle = computed(() => {
  const titles = {
    inventory: '📦 库存台账报表',
    flow: '📋 出入库流水报表',
    utilization: '📊 库位利用率报表',
    alert: '⚠️ 预警报表'
  }
  return titles[currentReport.value]
})

const selectReport = (index) => {
  currentReport.value = index
  pagination.value = { page: 0, size: 50, total: 0 }
  loadReportData()
  loadKpi()
}

const loadReportData = async () => {
  loading.value = true
  emptyText.value = '加载中...'
  reportData.value = []

  try {
    if (currentReport.value === 'inventory') {
      const res = await reportApi.getInventoryReport({
        area: selectedArea.value || undefined,
        status: '',
        goodsName: searchKeyword.value || undefined,
        page: pagination.value.page,
        size: pagination.value.size
      })
      reportData.value = res.data?.data || res.data?.content || res.data || []
      pagination.value.total = res.data?.total || res.data?.totalElements || res.total || 0
      emptyText.value = '暂无数据'
    } else if (currentReport.value === 'flow') {
      const res = await reportApi.getInoutFlowReport({
        type: selectedType.value || undefined,
        goodsName: searchKeyword.value || undefined,
        page: pagination.value.page,
        size: pagination.value.size
      })
      reportData.value = res.data?.data || res.data?.content || res.data || []
      pagination.value.total = res.data?.total || res.data?.totalElements || res.total || 0
      emptyText.value = '暂无数据'
    } else if (currentReport.value === 'utilization') {
      const res = await reportApi.getUtilizationReport({
        area: selectedArea.value || undefined
      })
      reportData.value = res.data?.data || res.data?.content || res.data || []
      pagination.value.total = reportData.value.length
      const _sum = res.data?.summary || res.summary
      if (_sum) {
        kpiData.value = { ...kpiData.value, ..._sum }
      }
      emptyText.value = '暂无数据'
    } else if (currentReport.value === 'alert') {
      const res = await reportApi.getAlertsReport()
      reportData.value = res.data?.data || res.data?.content || res.data || []
      pagination.value.total = reportData.value.length
      const _sum = res.data?.summary || res.summary
      if (_sum) {
        kpiData.value = { ...kpiData.value, ..._sum }
      }
      emptyText.value = '暂无数据'
    }
  } catch (e) {
    emptyText.value = '加载失败: ' + (e.message || '未知错误')
  } finally {
    loading.value = false
  }
}

const loadKpi = async () => {
  try {
    const res = await reportApi.getKpi()
    kpiData.value = res.data
  } catch (e) {
    // ignore
  }
}

const resetFilter = () => {
  dateRange.value = []
  selectedArea.value = ''
  selectedType.value = ''
  searchKeyword.value = ''
  pagination.value = { page: 0, size: 50, total: 0 }
  loadReportData()
}

const exportReport = async () => {
  try {
    let blob
    if (currentReport.value === 'inventory') {
      blob = await exportApi.exportInventory({ area: selectedArea.value || undefined })
    } else if (currentReport.value === 'flow') {
      blob = await exportApi.exportInoutRecords({
        type: selectedType.value || undefined,
        goodsName: searchKeyword.value || undefined,
        startTime: dateRange.value?.[0] || undefined,
        endTime: dateRange.value?.[1] || undefined
      })
    } else {
      blob = await exportApi.exportStatistics()
    }
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `报表_${currentReport.value}_${new Date().toISOString().slice(0, 10)}.xlsx`
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

const printReport = () => {
  window.print()
}

// 辅助方法
const getAttributeText = (attr) => {
  const map = { NORMAL: '普通', COLD: '冷藏', DANGEROUS: '危险品', VALUABLE: '高价值' }
  return map[attr] || attr || '普通'
}

const getStatusText = (status) => {
  const texts = { 0: '空闲', 1: '正常', 2: '预警', 3: '异常' }
  return texts[status] || '未知'
}

const getStatusTag = (status) => {
  const tags = { 0: 'info', 1: 'success', 2: 'warning', 3: 'danger' }
  return tags[status] || 'info'
}

const getTypeText = (type) => {
  const map = { IN: '入库', OUT: '出库', MOVE: '移库' }
  return map[type] || type
}

const getTypeTag = (type) => {
  const map = { IN: 'success', OUT: 'danger', MOVE: 'primary' }
  return map[type] || 'info'
}

const getAlertTagType = (level) => {
  const map = { warning: 'warning', danger: 'danger', info: 'info' }
  return map[level] || 'info'
}

const getProgressColor = (rate) => {
  const r = parseFloat(rate)
  if (r >= 80) return '#67c23a'
  if (r >= 50) return '#e6a23c'
  return '#909399'
}

const formatDate = (date) => {
  if (!date) return '-'
  try {
    const d = new Date(date)
    return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
  } catch {
    return '-'
  }
}

const isExpiringSoon = (date) => {
  if (!date) return false
  try {
    const expiry = new Date(date).getTime()
    return expiry - Date.now() < 7 * 24 * 60 * 60 * 1000
  } catch {
    return false
  }
}

onMounted(() => {
  loadReportData()
  loadKpi()
})
</script>

<style scoped>
.report-center {
  padding: 12px;
}

.report-layout {
  display: flex;
  gap: 12px;
}

.report-menu {
  width: 180px;
  background: rgba(25, 25, 35, 0.6);
  border-radius: 8px;
  padding: 12px;
  flex-shrink: 0;
}

.menu-title {
  color: #fff;
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(100, 100, 150, 0.2);
}

.menu-list {
  border: none;
  background: transparent;
}

.menu-list :deep(.el-menu-item) {
  color: #bbb;
  border-radius: 6px;
  margin-bottom: 4px;
}

.menu-list :deep(.el-menu-item:hover) {
  background: rgba(100, 100, 150, 0.1);
  color: #fff;
}

.menu-list :deep(.el-menu-item.is-active) {
  background: rgba(64, 158, 255, 0.2);
  color: #409eff;
}

.menu-item {
  font-size: 13px;
}

.report-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.filter-section {
  background: rgba(25, 25, 35, 0.6);
  border-radius: 8px;
  padding: 12px;
  flex-shrink: 0;
}

.filter-row {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.report-header h4 {
  color: #fff;
  margin: 0;
  font-size: 14px;
}

.kpi-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.report-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.report-table {
  background: rgba(25, 25, 35, 0.6);
  border-radius: 8px;
  padding: 12px;
  flex: 1;
  overflow: hidden;
  position: relative;
  min-height: 200px;
}

.report-table :deep(.el-table) {
  background: transparent;
}

.loading-mask {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(25, 25, 35, 0.7);
  border-radius: 8px;
  z-index: 10;
}

.expiry-warning {
  color: #ff6b6b;
  font-weight: bold;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
}

.pagination-info {
  color: #aaa;
  font-size: 12px;
}
</style>