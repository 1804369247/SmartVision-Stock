<template>
  <div class="dashboard">
    <div class="filter-bar">
      <el-input v-model="filters.keyword" placeholder="搜索库位/货物/批次" size="small" clearable @change="applyFilters">
        <template #prefix>🔍</template>
      </el-input>
      <el-select v-model="filters.status" placeholder="状态" size="small" clearable @change="applyFilters">
        <el-option label="全部" value="" />
        <el-option label="空库位" value="0" />
        <el-option label="正常库存" value="1" />
        <el-option label="库存预警" value="2" />
        <el-option label="已冻结" value="frozen" />
      </el-select>
      <el-select v-model="filters.attribute" placeholder="库位属性" size="small" clearable @change="applyFilters">
        <el-option label="全部" value="" />
        <el-option label="普通库位" value="NORMAL" />
        <el-option label="冷藏库位" value="COLD" />
        <el-option label="危险品库位" value="DANGEROUS" />
        <el-option label="高价值库位" value="VALUABLE" />
      </el-select>
      <el-select v-model="filters.area" placeholder="分区" size="small" clearable @change="applyFilters">
        <el-option label="全部" value="" />
        <el-option label="A区" value="A" />
        <el-option label="B区" value="B" />
        <el-option label="C区" value="C" />
      </el-select>
      <el-button v-if="hasActiveFilters" size="small" type="danger" link @click="clearFilters">清除筛选</el-button>
    </div>
    
    <div class="filter-results" v-if="hasActiveFilters">
      <span>筛选结果: {{ filteredLocations.length }} 个库位</span>
      <el-button size="small" type="primary" @click="showFiltered">🔍 只显示结果</el-button>
      <el-button size="small" @click="locateFirst">📍 定位第一个</el-button>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon">📦</div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.totalGoods }}</div>
          <div class="stat-label">库存总量</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">🏭</div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.totalLocations }}</div>
          <div class="stat-label">库位总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">✅</div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.usedLocations }}</div>
          <div class="stat-label">已用库位</div>
        </div>
      </div>
      <div class="stat-card warning" v-if="stats.warningCount > 0">
        <div class="stat-icon">⚠️</div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.warningCount }}</div>
          <div class="stat-label">预警数量</div>
        </div>
      </div>
    </div>

    <div class="charts-row">
      <div class="chart-card">
        <h4>库位利用率</h4>
        <div class="pie-chart">
          <svg viewBox="0 0 100 100">
            <circle cx="50" cy="50" r="40" fill="none" stroke="#3a3a5c" stroke-width="20" />
            <circle 
              cx="50" cy="50" r="40" fill="none" stroke="#2d8c4e" stroke-width="20"
              :stroke-dasharray="`${usedPercent * 2.51} 251`"
              stroke-linecap="round" transform="rotate(-90 50 50)"
            />
          </svg>
          <div class="pie-center">
            <div class="pie-value">{{ usedPercent }}%</div>
            <div class="pie-label">已使用</div>
          </div>
        </div>
        <div class="pie-legend">
          <div><span class="legend-color" style="background: #2d8c4e"></span> 已用 {{ stats.usedLocations }}</div>
          <div><span class="legend-color" style="background: #3a3a5c"></span> 空闲 {{ stats.emptyLocations }}</div>
        </div>
      </div>

      <div class="chart-card">
        <h4>库存状态分布</h4>
        <div class="bar-chart">
          <div class="bar-item">
            <span class="bar-label">正常</span>
            <div class="bar-track">
              <div class="bar-fill normal" :style="{ width: normalPercent + '%' }"></div>
            </div>
            <span class="bar-value">{{ stats.normalCount }}</span>
          </div>
          <div class="bar-item">
            <span class="bar-label">预警</span>
            <div class="bar-track">
              <div class="bar-fill warning" :style="{ width: warningPercent + '%' }"></div>
            </div>
            <span class="bar-value">{{ stats.warningCount }}</span>
          </div>
          <div class="bar-item">
            <span class="bar-label">空库</span>
            <div class="bar-track">
              <div class="bar-fill empty" :style="{ width: emptyPercent + '%' }"></div>
            </div>
            <span class="bar-value">{{ stats.emptyLocations }}</span>
          </div>
          <div class="bar-item">
            <span class="bar-label">异常</span>
            <div class="bar-track">
              <div class="bar-fill error" :style="{ width: errorPercent + '%' }"></div>
            </div>
            <span class="bar-value">{{ stats.errorCount }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="alert-section">
      <h4>⚠️ 异常预警</h4>
      <div v-if="alerts.length === 0" class="no-alerts">
        <div class="no-alerts-icon">✓</div>
        <span>暂无异常预警</span>
      </div>
      <div v-else class="alert-list">
        <div 
          v-for="alert in alerts" 
          :key="alert.id" 
          class="alert-item"
          :class="alert.level"
          @click="handleAlertClick(alert)"
        >
          <span class="alert-icon">{{ alert.icon }}</span>
          <div class="alert-content">
            <div class="alert-title">{{ alert.title }}</div>
            <div class="alert-desc">{{ alert.description }}</div>
          </div>
          <span class="alert-location">{{ alert.location }}</span>
        </div>
      </div>
    </div>

    <div class="quick-actions">
      <h4>快捷操作</h4>
      <div class="action-buttons">
        <el-button size="small" @click="$emit('refresh')">🔄 刷新数据</el-button>
        <el-button size="small" type="primary" @click="$emit('export')">📊 导出报表</el-button>
        <el-button size="small" type="warning" @click="$emit('locateWarnings')">📍 定位预警</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'

const props = defineProps({
  locations: { type: Array, default: () => [] },
  goods: { type: Array, default: () => [] }
})

const emit = defineEmits(['refresh', 'export', 'locateWarnings', 'locateLocation', 'filterChange', 'showFilteredLocations'])

const filters = ref({
  keyword: '',
  status: '',
  attribute: '',
  area: ''
})

const filteredLocations = ref([])

const hasActiveFilters = computed(() => {
  return filters.value.keyword || filters.value.status || filters.value.attribute || filters.value.area
})

const applyFilters = () => {
  filteredLocations.value = props.locations.filter(loc => {
    if (filters.value.keyword) {
      const kw = filters.value.keyword.toLowerCase()
      const matchCode = loc.locationCode?.toLowerCase().includes(kw)
      const matchGoods = loc.goodsName?.toLowerCase().includes(kw)
      const matchBatch = loc.batchNo?.toLowerCase().includes(kw)
      if (!matchCode && !matchGoods && !matchBatch) return false
    }
    if (filters.value.status) {
      if (filters.value.status === 'frozen') {
        if (!loc.frozen) return false
      } else {
        const statusNum = parseInt(filters.value.status)
        if (loc.status !== statusNum) return false
      }
    }
    if (filters.value.attribute && loc.attribute !== filters.value.attribute) {
      return false
    }
    if (filters.value.area && loc.area !== filters.value.area) {
      return false
    }
    return true
  })
  emit('filterChange', filteredLocations.value)
}

const clearFilters = () => {
  filters.value = { keyword: '', status: '', attribute: '', area: '' }
  filteredLocations.value = []
  emit('filterChange', [])
}

const showFiltered = () => {
  emit('showFilteredLocations', filteredLocations.value)
}

const locateFirst = () => {
  if (filteredLocations.value.length > 0) {
    emit('locateLocation', filteredLocations.value[0].locationCode)
  }
}

const stats = ref({
  totalGoods: 0,
  totalLocations: 144,
  usedLocations: 0,
  emptyLocations: 144,
  normalCount: 0,
  warningCount: 0,
  errorCount: 0
})

const alerts = ref([])

const usedPercent = computed(() => Math.round((stats.value.usedLocations / stats.value.totalLocations) * 100))
const normalPercent = computed(() => stats.value.totalLocations > 0 ? Math.round((stats.value.normalCount / stats.value.totalLocations) * 100) : 0)
const warningPercent = computed(() => stats.value.totalLocations > 0 ? Math.round((stats.value.warningCount / stats.value.totalLocations) * 100) : 0)
const emptyPercent = computed(() => stats.value.totalLocations > 0 ? Math.round((stats.value.emptyLocations / stats.value.totalLocations) * 100) : 0)
const errorPercent = computed(() => stats.value.totalLocations > 0 ? Math.round((stats.value.errorCount / stats.value.totalLocations) * 100) : 0)

const updateStats = () => {
  const locs = props.locations || []
  stats.value.totalLocations = locs.length || 144
  stats.value.usedLocations = locs.filter(l => l.status !== 0).length
  stats.value.emptyLocations = locs.filter(l => l.status === 0).length
  stats.value.normalCount = locs.filter(l => l.status === 1).length
  stats.value.warningCount = locs.filter(l => l.status === 2).length
  stats.value.errorCount = locs.filter(l => l.status === 3).length
  stats.value.totalGoods = locs.reduce((sum, l) => sum + (l.quantity || 0), 0)

  alerts.value = []
  locs.filter(l => l.status === 2 || l.status === 3 || l.frozen).forEach((l, index) => {
    let alertData = {
      id: index,
      icon: '❄️',
      level: 'info',
      title: '已冻结',
      description: l.frozenReason || '货物已冻结',
      location: l.locationCode
    }
    if (l.status === 2) {
      alertData = { id: index, icon: '⚠️', level: 'warning', title: '库存预警', description: `库存低于预警阈值 (${l.quantity}件)`, location: l.locationCode }
    } else if (l.status === 3) {
      alertData = { id: index, icon: '🔴', level: 'error', title: '库位异常', description: '库位状态异常', location: l.locationCode }
    }
    alerts.value.push(alertData)
  })
}

const handleAlertClick = (alert) => {
  emit('locateLocation', alert.location)
}

onMounted(() => updateStats())

defineExpose({ updateStats })
</script>

<style scoped>
.dashboard { padding: 10px; }
.filter-bar { display: flex; gap: 8px; margin-bottom: 12px; flex-wrap: wrap; }
.filter-bar .el-input { width: 140px; }
.filter-bar .el-select { width: 120px; }
.filter-results { display: flex; gap: 10px; align-items: center; padding: 8px 12px; background: rgba(45, 140, 78, 0.1); border-radius: 6px; margin-bottom: 12px; font-size: 13px; }
.filter-results span { color: #2d8c4e; flex: 1; }
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin-bottom: 15px; }
.stat-card {
  background: rgba(30, 30, 50, 0.7);
  border-radius: 10px;
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid rgba(100, 100, 150, 0.2);
}
.stat-card.warning { border-color: rgba(230, 162, 60, 0.5); background: rgba(230, 162, 60, 0.1); }
.stat-icon { font-size: 24px; }
.stat-content { flex: 1; }
.stat-value { font-size: 20px; font-weight: bold; color: #fff; }
.stat-label { font-size: 11px; color: #aaa; }

.charts-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 15px; }
.chart-card {
  background: rgba(30, 30, 50, 0.7);
  border-radius: 10px;
  padding: 12px;
  border: 1px solid rgba(100, 100, 150, 0.2);
}
.chart-card h4 { color: #ccc; font-size: 13px; margin-bottom: 12px; }

.pie-chart { position: relative; width: 100%; padding-bottom: 100%; }
.pie-chart svg { position: absolute; width: 100%; height: 100%; }
.pie-center {
  position: absolute;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}
.pie-value { font-size: 20px; font-weight: bold; color: #fff; }
.pie-label { font-size: 11px; color: #aaa; }
.pie-legend { margin-top: 10px; }
.pie-legend div { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #aaa; margin-bottom: 4px; }
.legend-color { width: 12px; height: 12px; border-radius: 3px; }

.bar-chart { display: flex; flex-direction: column; gap: 10px; }
.bar-item { display: flex; align-items: center; gap: 10px; }
.bar-label { width: 35px; font-size: 12px; color: #aaa; }
.bar-track { flex: 1; height: 12px; background: rgba(60, 60, 80, 0.5); border-radius: 6px; overflow: hidden; }
.bar-fill { height: 100%; border-radius: 6px; transition: width 0.5s ease; }
.bar-fill.normal { background: #2d8c4e; }
.bar-fill.warning { background: #e6a23c; }
.bar-fill.empty { background: #3a3a5c; }
.bar-fill.error { background: #f56c6c; }
.bar-value { width: 35px; text-align: right; font-size: 12px; color: #ccc; }

.alert-section {
  background: rgba(30, 30, 50, 0.7);
  border-radius: 10px;
  padding: 12px;
  border: 1px solid rgba(100, 100, 150, 0.2);
  margin-bottom: 15px;
}
.alert-section h4 { color: #ccc; font-size: 13px; margin-bottom: 12px; }
.no-alerts { text-align: center; padding: 20px; color: #666; }
.no-alerts-icon { font-size: 32px; color: #67c23a; margin-bottom: 8px; }
.alert-list { display: flex; flex-direction: column; gap: 8px; }
.alert-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: rgba(60, 60, 80, 0.5);
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}
.alert-item:hover { background: rgba(80, 80, 100, 0.5); }
.alert-item.warning { border-left: 3px solid #e6a23c; }
.alert-item.error { border-left: 3px solid #f56c6c; }
.alert-icon { font-size: 18px; }
.alert-content { flex: 1; }
.alert-title { font-size: 13px; color: #fff; font-weight: bold; }
.alert-desc { font-size: 11px; color: #aaa; }
.alert-location { font-size: 12px; color: #88ccff; font-family: monospace; }

.quick-actions {
  background: rgba(30, 30, 50, 0.7);
  border-radius: 10px;
  padding: 12px;
  border: 1px solid rgba(100, 100, 150, 0.2);
}
.quick-actions h4 { color: #ccc; font-size: 13px; margin-bottom: 12px; }
.action-buttons { display: flex; gap: 8px; }
</style>
