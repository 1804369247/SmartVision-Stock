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
            />
            <el-select v-model="selectedArea" placeholder="选择库区" clearable size="small" style="width: 120px;">
              <el-option label="全部" value="" />
              <el-option label="A区" value="A" />
              <el-option label="B区" value="B" />
              <el-option label="C区" value="C" />
            </el-select>
            <el-select v-model="selectedGoodsType" placeholder="货物类型" clearable size="small" style="width: 150px;">
              <el-option label="全部类型" value="" />
              <el-option label="电子设备" value="电子设备" />
              <el-option label="机械零件" value="机械零件" />
              <el-option label="原材料" value="原材料" />
              <el-option label="其他" value="其他" />
            </el-select>
            <el-button type="primary" size="small" @click="queryReport">
              <el-icon class="el-icon--left"><Search /></el-icon>
              查询
            </el-button>
            <el-button size="small" @click="resetFilter">重置</el-button>
          </div>
        </div>
        
        <!-- 报表标题 -->
        <div class="report-header">
          <h4>{{ reportTitle }}</h4>
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
        
        <!-- 报表数据 -->
        <div class="report-table">
          <el-table :data="reportData" border size="small" max-height="400">
            <template v-if="currentReport === 'inventory'">
              <el-table-column prop="locationCode" label="库位编码" width="100" />
              <el-table-column prop="goodsName" label="货物名称" />
              <el-table-column prop="batchNo" label="批次号" width="100" />
              <el-table-column prop="quantity" label="数量" width="80" />
              <el-table-column prop="inTime" label="入库时间" width="160" />
              <el-table-column prop="status" label="状态" width="80">
                <template #default="scope">
                  <el-tag :type="getStatusTag(scope.row.status)" size="small">
                    {{ getStatusText(scope.row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
            </template>
            
            <template v-else-if="currentReport === 'flow'">
              <el-table-column prop="orderNo" label="单据号" width="120" />
              <el-table-column prop="type" label="类型" width="80">
                <template #default="scope">
                  <el-tag :type="scope.row.type === '入库' ? 'success' : 'danger'" size="small">
                    {{ scope.row.type }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="goodsName" label="货物名称" />
              <el-table-column prop="quantity" label="数量" width="80" />
              <el-table-column prop="operator" label="操作人" width="100" />
              <el-table-column prop="time" label="时间" width="160" />
            </template>
            
            <template v-else-if="currentReport === 'utilization'">
              <el-table-column prop="area" label="库区" width="80" />
              <el-table-column prop="totalLocations" label="总库位数" width="100" />
              <el-table-column prop="usedLocations" label="已使用" width="100" />
              <el-table-column prop="emptyLocations" label="空闲" width="80" />
              <el-table-column prop="utilizationRate" label="利用率" width="100">
                <template #default="scope">
                  <span :class="scope.row.utilizationRate > 80 ? 'high-rate' : ''">
                    {{ scope.row.utilizationRate }}%
                  </span>
                </template>
              </el-table-column>
            </template>
            
            <template v-else-if="currentReport === 'alert'">
              <el-table-column prop="type" label="预警类型" width="100">
                <template #default="scope">
                  <el-tag :type="scope.row.type === '库存预警' ? 'warning' : 'danger'" size="small">
                    {{ scope.row.type }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="locationCode" label="库位" width="100" />
              <el-table-column prop="goodsName" label="货物名称" />
              <el-table-column prop="quantity" label="当前库存" width="100" />
              <el-table-column prop="warningThreshold" label="预警阈值" width="100" />
              <el-table-column prop="expiryDate" label="过期时间" width="160" />
            </template>
          </el-table>
        </div>
        
        <!-- 统计摘要 -->
        <div class="summary-section" v-if="summaryData">
          <div class="summary-item">
            <span class="label">{{ summaryData.label1 }}</span>
            <span class="value">{{ summaryData.value1 }}</span>
          </div>
          <div class="summary-item">
            <span class="label">{{ summaryData.label2 }}</span>
            <span class="value">{{ summaryData.value2 }}</span>
          </div>
          <div class="summary-item">
            <span class="label">{{ summaryData.label3 }}</span>
            <span class="value">{{ summaryData.value3 }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Download, Printer } from '@element-plus/icons-vue'

const props = defineProps({
  locations: { type: Array, default: () => [] }
})

const currentReport = ref('inventory')
const dateRange = ref([])
const selectedArea = ref('')
const selectedGoodsType = ref('')
const reportData = ref([])
const summaryData = ref(null)

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
  loadReportData()
}

const loadReportData = () => {
  const mockData = {
    inventory: [
      { locationCode: 'A-01-01', goodsName: '电机轴承', batchNo: 'B20240501', quantity: 20, inTime: '2024-05-15 09:30:00', status: 1 },
      { locationCode: 'A-01-02', goodsName: '控制器', batchNo: 'B20240502', quantity: 15, inTime: '2024-05-16 10:20:00', status: 1 },
      { locationCode: 'B-02-01', goodsName: '电子元件', batchNo: 'B20240503', quantity: 5, inTime: '2024-05-10 14:00:00', status: 2 }
    ],
    flow: [
      { orderNo: 'IN20240515001', type: '入库', goodsName: '电机轴承', quantity: 20, operator: '张三', time: '2024-05-15 09:30:00' },
      { orderNo: 'OUT20240516001', type: '出库', goodsName: '控制器', quantity: 5, operator: '李四', time: '2024-05-16 14:20:00' },
      { orderNo: 'IN20240517001', type: '入库', goodsName: '电子元件', quantity: 30, operator: '王五', time: '2024-05-17 11:00:00' }
    ],
    utilization: [
      { area: 'A区', totalLocations: 50, usedLocations: 35, emptyLocations: 15, utilizationRate: 70 },
      { area: 'B区', totalLocations: 50, usedLocations: 45, emptyLocations: 5, utilizationRate: 90 },
      { area: 'C区', totalLocations: 50, usedLocations: 20, emptyLocations: 30, utilizationRate: 40 }
    ],
    alert: [
      { type: '库存预警', locationCode: 'B-02-01', goodsName: '电子元件', quantity: 5, warningThreshold: 10, expiryDate: '-' },
      { type: '保质期预警', locationCode: 'A-03-01', goodsName: '化工原料', quantity: 25, warningThreshold: '-', expiryDate: '2024-06-20' }
    ]
  }
  
  reportData.value = mockData[currentReport.value] || []
  
  const summaryMocks = {
    inventory: { label1: '总库位数', value1: '150', label2: '在库货物种类', value2: '45', label3: '总库存量', value3: '2,580' },
    flow: { label1: '入库次数', value1: '28', label2: '出库次数', value2: '15', label3: '移库次数', value3: '7' },
    utilization: { label1: '总库位数', value1: '150', label2: '利用率', value2: '66.7%', label3: '空闲库位', value3: '50' },
    alert: { label1: '库存预警', value1: '8', label2: '保质期预警', value2: '3', label3: '总预警数', value3: '11' }
  }
  summaryData.value = summaryMocks[currentReport.value]
}

const queryReport = () => {
  ElMessage.success('报表查询成功')
  loadReportData()
}

const resetFilter = () => {
  dateRange.value = []
  selectedArea.value = ''
  selectedGoodsType.value = ''
  loadReportData()
}

const exportReport = () => {
  ElMessage.success('报表导出中，请稍候...')
  setTimeout(() => {
    ElMessage.success('报表导出成功！')
  }, 800)
}

const printReport = () => {
  ElMessage.info('正在准备打印...')
  setTimeout(() => {
    ElMessage.success('打印任务已发送！')
  }, 500)
}

const getStatusTag = (status) => {
  const tags = { 0: 'info', 1: 'success', 2: 'warning', 3: 'danger' }
  return tags[status] || 'info'
}

const getStatusText = (status) => {
  const texts = { 0: '空闲', 1: '正常', 2: '预警', 3: '异常' }
  return texts[status] || status
}

onMounted(() => {
  loadReportData()
})
</script>

<style scoped>
.report-center {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.report-layout {
  display: flex;
  height: 100%;
  gap: 12px;
}

.report-menu {
  width: 180px;
  background: rgba(25, 25, 35, 0.6);
  border-radius: 8px;
  padding: 12px;
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
}

.filter-section {
  background: rgba(25, 25, 35, 0.6);
  border-radius: 8px;
  padding: 12px;
}

.filter-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.report-header h4 {
  color: #fff;
  margin: 0;
  font-size: 14px;
}

.report-actions {
  display: flex;
  gap: 8px;
}

.report-table {
  background: rgba(25, 25, 35, 0.6);
  border-radius: 8px;
  padding: 12px;
  flex: 1;
  overflow: hidden;
}

.report-table :deep(.el-table) {
  background: transparent;
}

.high-rate {
  color: #e6a23c;
  font-weight: bold;
}

.summary-section {
  display: flex;
  gap: 20px;
  background: rgba(25, 25, 35, 0.6);
  border-radius: 8px;
  padding: 12px;
}

.summary-item {
  flex: 1;
  text-align: center;
}

.summary-item .label {
  display: block;
  font-size: 12px;
  color: #888;
  margin-bottom: 4px;
}

.summary-item .value {
  display: block;
  font-size: 18px;
  font-weight: bold;
  color: #409eff;
}
</style>