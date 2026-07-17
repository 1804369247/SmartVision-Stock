<template>
  <div class="wms-module">
    <!-- 页面标题栏 -->
    <div class="module-header">
      <div class="header-left">
        <h3 class="page-title">
          <span class="title-icon">{{ pageIcon }}</span>
          {{ pageTitle }}
        </h3>
        <el-breadcrumb separator="/" v-if="currentSubPage !== 'inbound' && currentSubPage !== 'outbound'">
          <el-breadcrumb-item>{{ breadcrumbParent }}</el-breadcrumb-item>
          <el-breadcrumb-item>{{ pageTitle }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      <div class="header-right">
        <el-button v-if="currentSubPage === 'inbound' || currentSubPage === 'outbound'" size="small" type="primary" @click="resetForm">
          <span class="btn-icon">+</span> 新增单据
        </el-button>
        <el-button size="small" plain @click="refreshData">
          🔄 刷新
        </el-button>
      </div>
    </div>

    <!-- ========== 入库/出库表单页（带统计卡片）========== -->
    <template v-if="currentSubPage === 'inbound' || currentSubPage === 'outbound'">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-primary">
          <div class="stat-icon-wrap" :style="{ background: currentSubPage === 'inbound' ? '#e6f7ff' : '#fff7e6' }">
            <span class="stat-emoji">{{ currentSubPage === 'inbound' ? '📥' : '📤' }}</span>
          </div>
          <div class="stat-info">
            <div class="stat-label">今日{{ currentSubPage === 'inbound' ? '入库' : '出库' }}</div>
            <div class="stat-value">{{ todayCount }}</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon-wrap" style="background:#f6ffed">
            <span class="stat-emoji">📦</span>
          </div>
          <div class="stat-info">
            <div class="stat-label">待处理</div>
            <div class="stat-value pending-count">{{ pendingCount }}</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon-wrap" style="background:#fff0f6">
            <span class="stat-emoji">✅</span>
          </div>
          <div class="stat-info">
            <div class="stat-label">已完成</div>
            <div class="stat-value success-text">{{ completedCount }}</div>
          </div>
        </div>
      </div>

      <!-- 表单区域 -->
      <div class="form-card">
        <div class="card-title">
          <span class="card-title-icon">📝</span>
          {{ currentSubPage === 'inbound' ? '入库信息填写' : '出库信息填写' }}
        </div>
        <div class="card-body">
          <el-form :model="orderForm" label-width="90px" size="default" label-position="top">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item :label="currentSubPage === 'inbound' ? '供应商' : '客户'">
                  <el-select v-model="orderForm.supplierId" placeholder="请选择" clearable filterable style="width:100%">
                    <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="备注说明">
                  <el-input v-model="orderForm.remark" placeholder="请输入备注信息" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>

          <!-- 商品明细 -->
          <div class="items-card">
            <div class="items-header-bar">
              <span class="items-title">
                <span class="items-icon">🧾</span> 商品明细
              </span>
              <el-button size="small" type="primary" plain @click="addOrderItem">
                + 添加商品
              </el-button>
            </div>

            <el-table :data="orderForm.items" border stripe class="items-table" empty-text="暂无商品，请添加">
              <el-table-column prop="goodsId" label="商品名称" min-width="180">
                <template #default="scope">
                  <el-select v-model="scope.row.goodsId" placeholder="选择商品" filterable size="default" style="width:100%">
                    <el-option v-for="g in goodsList" :key="g.id" :label="g.name" :value="g.id">
                      <span>{{ g.name }}</span>
                      <span style="color:#999;font-size:11px;float:right">{{ g.code || '' }}</span>
                    </el-option>
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column prop="batchNo" label="批次号" width="130">
                <template #default="scope">
                  <el-input v-model="scope.row.batchNo" placeholder="批次号" size="default" />
                </template>
              </el-table-column>
              <el-table-column prop="quantity" label="数量" width="100">
                <template #default="scope">
                  <el-input-number v-model="scope.row.quantity" :min="1" :max="9999" size="default" controls-position="right" style="width:100%" />
                </template>
              </el-table-column>
              <el-table-column v-if="currentSubPage === 'inbound'" prop="locationId" label="目标库位" width="140">
                <template #default="scope">
                  <el-select v-model="scope.row.locationId" placeholder="选择库位" filterable size="default" style="width:100%">
                    <el-option v-for="l in emptyLocations" :key="l.id" :label="l.locationCode" :value="l.id">
                      <span>{{ l.locationCode }}</span>
                      <el-tag size="small" type="info" style="margin-left:4px">{{ l.area || '' }}区</el-tag>
                    </el-option>
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column v-if="currentSubPage === 'outbound'" prop="goodsInstanceId" label="库存批次" width="150">
                <template #default="scope">
                  <el-select v-model="scope.row.goodsInstanceId" placeholder="选择批次" size="default" style="width:100%">
                    <el-option v-for="i in goodsInstances" :key="i.id ?? i.batchNo" :label="(i.batchNo || '') + '(' + (i.quantity || 0) + '件)'" :value="i.id ?? ''" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="" width="55" align="center">
                <template #default="scope">
                  <el-button size="small" text type="danger" @click="removeOrderItem(scope.$index)" title="删除">
                    ✕
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 操作按钮 -->
          <div class="form-actions-bar">
            <div class="action-summary" v-if="orderForm.items.length > 0">
              共 <strong class="summary-num">{{ orderForm.items.length }}</strong> 项商品，
              合计数量：<strong class="summary-num summary-total">{{ totalQuantity }}</strong>
            </div>
            <div class="action-buttons">
              <el-button @click="resetForm" plain>重置</el-button>
              <el-button type="primary" @click="submitOrder" :loading="submitting">
                {{ currentSubPage === 'inbound' ? '提交入库单' : '提交出库单' }}
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- ========== 查询列表页 ========== -->
    <template v-else>
      <!-- 搜索区域 -->
      <div class="search-card">
        <el-input v-model="searchKeyword" placeholder="搜索单号..." size="default" clearable @keyup.enter="search" class="search-input">
          <template #prefix><span style="font-size:14px">🔍</span></template>
        </el-input>
        <el-select v-model="searchStatus" placeholder="状态筛选" size="default" clearable class="status-filter">
          <el-option label="全部" value="" />
          <el-option label="📝 草稿" value="DRAFT" />
          <el-option label="⏳ 审核中" value="AUDITING" />
          <el-option label="✅ 已完成" value="COMPLETED" />
          <el-option v-if="currentSubPage === 'outboundList'" label="📋 拣货中" value="PICKING" />
        </el-select>
        <el-button type="primary" @click="search" plain>查询</el-button>
      </div>

      <!-- 数据表格 -->
      <div class="table-card">
        <el-table :data="tableData" border stripe class="data-table" empty-text="暂无数据" v-loading="tableLoading">
          <el-table-column prop="orderNo" label="单号" min-width="160">
            <template #default="scope">
              <span class="order-no-link" @click="viewOrder(scope.row)">{{ scope.row.orderNo }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="typeText" label="类型" width="95">
            <template #default="scope">
              <el-tag :type="scope.row.type === 'purchase' ? '' : 'success'" size="small">
                {{ scope.row.typeText }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="supplierName" v-if="currentSubPage === 'inboundList'" label="供应商" min-width="120" />
          <el-table-column prop="warehouseName" label="仓库" width="90" />
          <el-table-column prop="totalQuantity" label="数量" width="75" align="right">
            <template #default="scope">
              <span class="qty-cell">{{ scope.row.totalQuantity }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="statusText" label="状态" width="90" align="center">
            <template #default="scope">
              <el-tag :type="getStatusTagType(scope.row.status)" size="small" effect="light">
                {{ scope.row.statusText }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="155" />
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="scope">
              <div class="action-group">
                <el-button size="small" link type="primary" @click="viewOrder(scope.row)">查看</el-button>
                <el-button size="small" link type="primary" @click="printOrder(scope.row)">打印</el-button>
                <el-button v-if="scope.row.status === 'DRAFT'" size="small" link type="warning" @click="auditOrder(scope.row)">审核</el-button>
                <el-button v-if="scope.row.status === 'AUDITING'" size="small" link :type="currentSubPage === 'inboundList' ? 'success' : 'warning'" @click="confirmOrder(scope.row)">
                  {{ currentSubPage === 'inboundList' ? '确认入库' : '确认出库' }}
                </el-button>
                <el-button v-if="scope.row.status === 'AUDITING' && currentSubPage === 'outboundList'" size="small" link type="info" @click="pickOrder(scope.row)">拣货</el-button>
                <el-button v-if="scope.row.status === 'DRAFT'" size="small" link type="danger" @click="deleteOrder(scope.row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-bar" v-if="tableData.length > 0">
          <span class="total-info">共 {{ tableData.length }} 条记录</span>
        </div>
      </div>
    </template>

    <!-- 查看详情弹窗 -->
    <el-dialog v-model="showOrderModal" :title="'查看单据 — ' + (viewingOrder?.orderNo || '')" width="520px" destroy-on-close>
      <template v-if="viewingOrder">
        <div class="detail-view">
          <div class="detail-meta-grid">
            <div class="dm-item">
              <span class="dm-label">单号</span>
              <span class="dm-value mono">{{ viewingOrder.orderNo }}</span>
            </div>
            <div class="dm-item">
              <span class="dm-label">类型</span>
              <el-tag :type="viewingOrder.type === 'purchase' ? '' : 'success'" size="small">{{ viewingOrder.typeText }}</el-tag>
            </div>
            <div class="dm-item">
              <span class="dm-label">状态</span>
              <el-tag :type="getStatusTagType(viewingOrder.status)" size="small">{{ viewingOrder.statusText }}</el-tag>
            </div>
            <div class="dm-item">
              <span class="dm-label">{{ currentSubPage === 'inboundList' ? '供应商' : '客户' }}</span>
              <span class="dm-value">{{ viewingOrder.supplierName || '-' }}</span>
            </div>
            <div class="dm-item">
              <span class="dm-label">总数量</span>
              <span class="dm-value strong">{{ viewingOrder.totalQuantity }}</span>
            </div>
            <div class="dm-item">
              <span class="dm-label">创建时间</span>
              <span class="dm-value">{{ viewingOrder.createTime }}</span>
            </div>
          </div>
          <div class="detail-items-section">
            <div class="detail-items-title">商品明细</div>
            <el-table :data="viewingOrder.items" border size="small" max-height="240">
              <el-table-column prop="goodsName" label="商品名称" />
              <el-table-column prop="batchNo" label="批次号" width="110" />
              <el-table-column prop="quantity" label="数量" width="65" align="center" />
              <el-table-column prop="locationCode" label="目标库位" width="100" />
            </el-table>
          </div>
        </div>
      </template>
    </el-dialog>

    <OrderPrint 
      :visible="showPrintModal"
      :order-type="currentSubPage === 'inboundList' ? 'inbound' : 'outbound'"
      :order-data="printOrderData"
      @close="showPrintModal = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import OrderPrint from './OrderPrint.vue'
import { basicApi, goodsApi, locationApi, stockApi, orderApi } from '../api'

const props = defineProps({
  subPage: { type: String, default: 'inboundList' }
})

const currentSubPage = computed(() => props.subPage)

const pageTitleMap = {
  inbound: '采购入库',
  outbound: '销售出库',
  inboundList: '入库单查询',
  outboundList: '出库单查询'
}
const pageTitle = computed(() => pageTitleMap[currentSubPage.value] || '管理')
const pageIcon = computed(() => {
  const icons = { inbound: '📥', outbound: '📤', inboundList: '📋', outboundList: '📋' }
  return icons[currentSubPage.value] || '📄'
})
const breadcrumbParent = computed(() => {
  return currentSubPage.value && currentSubPage.value.includes('inbound') ? '入库管理' : '出库管理'
})

// 状态
const searchKeyword = ref('')
const searchStatus = ref('')
const tableData = ref([])
const tableLoading = ref(false)
const showOrderModal = ref(false)
const viewingOrder = ref(null)
const showPrintModal = ref(false)
const printOrderData = ref({})
const submitting = ref(false)

// 表单数据
const suppliers = ref([])
const goodsList = ref([])
const emptyLocations = ref([])
const goodsInstances = ref([])

const orderForm = ref({
  supplierId: null,
  remark: '',
  items: []
})

// 统计数据
const todayCount = ref(0)
const pendingCount = ref(0)
const completedCount = ref(0)

const totalQuantity = computed(() => orderForm.value.items.reduce((sum, item) => sum + (item.quantity || 0), 0))

// ==================== 数据加载 ====================

const refreshData = async () => {
  tableLoading.value = true
  try {
    if (currentSubPage.value === 'inbound' || currentSubPage.value === 'outbound') {
      const [supResult, goodsResult, locResult, instResult] = await Promise.all([
        basicApi.getSuppliers(),
        goodsApi.getAll(),
        locationApi.getAll(),
        stockApi.getInstances({ size: 1000 })
      ])
      suppliers.value = supResult.data?.content || supResult.data || supResult || []
      goodsList.value = Array.isArray(goodsResult) ? goodsResult : (goodsResult.data?.content || goodsResult.data || [])
      emptyLocations.value = (Array.isArray(locResult) ? locResult : (locResult.data || [])).filter(l => l.status === 0)
      goodsInstances.value = Array.isArray(instResult) ? instResult : (instResult.data?.content || instResult.data || [])

      // 获取订单列表计算统计数据
      const listApi = currentSubPage.value === 'inbound'
        ? orderApi.getInboundList({ page: 0, size: 50 })
        : orderApi.getOutboundList({ page: 0, size: 50 })
      const listResult = await listApi
      const orderList = listResult.content || listResult.data?.content || []
      const today = new Date().toDateString()
      todayCount.value = orderList.filter(o => new Date(o.createTime).toDateString() === today).length
      pendingCount.value = orderList.filter(o => o.status === 'DRAFT' || o.status === 'AUDITING').length
      completedCount.value = orderList.filter(o => o.status === 'COMPLETED').length
    } else {
      const api = currentSubPage.value === 'inboundList'
        ? orderApi.getInboundList({ page: 0, size: 50 })
        : orderApi.getOutboundList({ page: 0, size: 50 })
      const result = await api
      let data = result.content || result.data?.content || []
      // 前端过滤：关键词 + 状态
      if (searchKeyword.value?.trim()) {
        const k = searchKeyword.value.trim().toLowerCase()
        data = data.filter(item =>
          (item.orderNo && item.orderNo.toLowerCase().includes(k))
        )
      }
      if (searchStatus.value) {
        data = data.filter(item => item.status === searchStatus.value)
      }
      tableData.value = data
      // 从实际订单数据更新统计
      const fullList = result.content || result.data?.content || []
      todayCount.value = fullList.length
      pendingCount.value = fullList.filter(o => o.status === 'DRAFT' || o.status === 'AUDITING').length
      completedCount.value = fullList.filter(o => o.status === 'COMPLETED').length
    }
  } catch (e) {
    ElMessage.error('加载数据失败')
  } finally {
    tableLoading.value = false
  }
}

const search = () => refreshData()

// ==================== 表单操作 ====================

const addOrderItem = () => {
  orderForm.value.items.push({
    goodsId: null,
    batchNo: '',
    quantity: 1,
    locationId: null,
    goodsInstanceId: null
  })
}

const removeOrderItem = (index) => {
  orderForm.value.items.splice(index, 1)
}

const resetForm = () => {
  orderForm.value = { supplierId: null, remark: '', items: [] }
  // 滚动到表单区域
  const el = document.querySelector('.stat-cards')
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const submitOrder = async () => {
  if (!orderForm.value.items.length) {
    ElMessage.warning('请添加至少一件商品')
    return
  }
  if (!orderForm.value.supplierId && (currentSubPage.value === 'inbound')) {
    ElMessage.warning('请选择供应商')
    return
  }

  const type = currentSubPage.value === 'inbound' ? 'purchase' : 'sale'
  submitting.value = true
  try {
    const data = {
      type,
      supplierId: orderForm.value.supplierId,
      remark: orderForm.value.remark,
      items: orderForm.value.items
    }
    const api = currentSubPage.value === 'inbound' ? orderApi.createInbound : orderApi.createOutbound
    const result = await api(data)

    if (result.code === 200) {
      ElMessage.success(currentSubPage.value === 'inbound' ? '入库单创建成功！' : '出库单创建成功！')
      resetForm()
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error('提交失败，请检查网络连接')
  } finally {
    submitting.value = false
  }
}

// ==================== 单据操作 ====================

const viewOrder = async (order) => {
  try {
    const api = currentSubPage.value === 'inboundList' 
      ? orderApi.getInboundById : orderApi.getOutboundById
    const result = await api(order.id)
    if (result.code === 200) {
      viewingOrder.value = result.data || result
      showOrderModal.value = true
    }
  } catch (e) {
    // 如果API失败，用行数据直接显示
    viewingOrder.value = order
    showOrderModal.value = true
  }
}

const printOrder = async (order) => {
  try {
    const api = currentSubPage.value === 'inboundList' 
      ? orderApi.getInboundById : orderApi.getOutboundById
    const result = await api(order.id)
    if (result.code === 200) {
      printOrderData.value = result.data || result
      showPrintModal.value = true
    }
  } catch (e) {
    printOrderData.value = order
    showPrintModal.value = true
  }
}

const auditOrder = async (order) => {
  try {
    const api = currentSubPage.value === 'inboundList' 
      ? orderApi.auditInbound : orderApi.auditOutbound
    const result = await api(order.id)
    if (result.code === 200) {
      ElMessage.success('审核通过')
      refreshData()
    } else {
      ElMessage.warning(result.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error('网络错误')
  }
}

const pickOrder = async (order) => {
  try {
    const result = await orderApi.pickOutbound(order.id)
    if (result.code === 200) {
      ElMessage.success('拣货成功')
      refreshData()
    } else {
      ElMessage.error(result.message || '拣货失败')
    }
  } catch (e) {
    ElMessage.error('拣货失败')
  }
}

const confirmOrder = async (order) => {
  try {
    const api = currentSubPage.value === 'inboundList' 
      ? orderApi.confirmInbound : orderApi.confirmOutbound
    const result = await api(order.id)
    if (result.code === 200) {
      const action = currentSubPage.value === 'inboundList' ? '入库' : '出库'
      ElMessage.success(`${action}成功，已更新3D库存模型！`)
      refreshData()
    } else {
      ElMessage.error(result.message || '确认失败')
    }
  } catch (e) {
    ElMessage.error(currentSubPage.value === 'inboundList' ? '入库失败' : '出库失败')
  }
}

const deleteOrder = async (order) => {
  try {
    await ElMessageBox.confirm(`确定删除单据 ${order.orderNo} 吗？删除后不可恢复。`, '确认删除', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'error',
    })
  } catch { return }

  try {
    const api = currentSubPage.value === 'inboundList'
      ? orderApi.deleteInbound : orderApi.deleteOutbound
    const result = await api(order.id)
    if (result.code === 200) {
      ElMessage.success('删除成功')
      refreshData()
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch (e) {
    ElMessage.error('删除失败，请检查网络连接')
  }
}

const getStatusTagType = (status) => {
  const types = { DRAFT: 'info', AUDITING: 'warning', PICKING: '', COMPLETED: 'success' }
  return types[status] || 'info'
}

onMounted(() => refreshData())

// 切换子 Tab 时自动重新加载数据
watch(currentSubPage, () => {
  tableData.value = []
  refreshData()
})
</script>

<style scoped>
/* ========== 整体容器 ========== */
.wms-module {
  padding: 16px 20px;
  background: #f5f7fa;
}

/* ========== 页面头部 ========== */
.module-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-shrink: 0;
}
.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.page-title {
  color: #1a1a2e;
  font-size: 18px;
  margin: 0;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}
.title-icon { font-size: 20px; }
.header-left .el-breadcrumb { font-size: 12px; }
.header-right {
  display: flex;
  gap: 8px;
}
.btn-icon {
  font-size: 15px;
  font-weight: 700;
  line-height: 1;
}

/* ========== 统计卡片 ========== */
.stat-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
  flex-shrink: 0;
}
.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 14px 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid #ebeef5;
  transition: transform 0.2s, box-shadow 0.2s;
}
.stat-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
.stat-card.stat-primary {
  background: linear-gradient(135deg, #fff, #f0f5ff);
  border-color: #d6e4ff;
}
.stat-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-emoji { font-size: 22px; }
.stat-info { display: flex; flex-direction: column; gap: 2px; }
.stat-label {
  font-size: 12px;
  color: #8c8c8c;
  font-weight: 500;
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  line-height: 1.2;
}
.pending-count { color: #faad14; }
.success-text { color: #52c41a; }

/* ========== 表单卡片 ========== */
.form-card {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid #ebeef5;
  overflow: hidden;
  flex-shrink: 0;
}
.card-title {
  padding: 14px 18px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  gap: 6px;
  background: #fafbfc;
}
.card-title-icon { font-size: 16px; }
.card-body {
  padding: 18px;
}

/* 商品明细子卡片 */
.items-card {
  margin-top: 16px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
}
.items-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: #fafafa;
  border-bottom: 1px solid #eee;
}
.items-title {
  font-size: 13px;
  font-weight: 600;
  color: #444;
  display: flex;
  align-items: center;
  gap: 5px;
}
.items-icon { font-size: 14px; }

.items-table {
  width: 100%;
}
.items-table :deep(.el-table__header th) {
  background: #f8f9fb !important;
  color: #555;
  font-weight: 600;
  font-size: 12px;
}

/* 操作按钮栏 */
.form-actions-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #f0f0f0;
}
.action-summary {
  font-size: 13px;
  color: #666;
}
.summary-num {
  color: #1890ff;
  font-size: 15px;
}
.summary-total {
  color: #faad14;
  font-size: 16px;
}
.action-buttons {
  display: flex;
  gap: 10px;
}

/* ========== 搜索区域 ========== */
.search-card {
  background: #fff;
  border-radius: 10px;
  padding: 14px 18px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid #ebeef5;
  margin-bottom: 14px;
  flex-shrink: 0;
}
.search-input {
  width: 220px;
}
.status-filter {
  width: 140px;
}

/* ========== 数据表格 ========== */
.table-card {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid #ebeef5;
  overflow: hidden;
  flex: 1;
  display: flex;
  flex-direction: column;
}
.data-table {
  flex: 1;
}
.data-table :deep(.el-table__header th) {
  background: #f8f9fb !important;
  color: #555;
  font-weight: 600;
  font-size: 12px;
}
.order-no-link {
  color: #1890ff;
  cursor: pointer;
  font-family: 'Consolas', monospace;
  font-size: 12.5px;
}
.order-no-link:hover {
  text-decoration: underline;
  color: #096dd9;
}
.qty-cell {
  font-weight: 600;
  color: #333;
}
.action-group {
  display: flex;
  flex-wrap: wrap;
  gap: 2px;
}

.pagination-bar {
  padding: 12px 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #f0f0f0;
  background: #fafbfc;
}
.total-info {
  font-size: 13px;
  color: #999;
}

/* ========== 详情弹窗 ========== */
.detail-view { padding: 4px 0; }
.detail-meta-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 12px;
}
.dm-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.dm-label {
  font-size: 11px;
  color: #999;
  text-transform: uppercase;
}
.dm-value {
  font-size: 14px;
  color: #333;
}
.dm-value.mono {
  font-family: 'Consolas', monospace;
  font-size: 13px;
  word-break: break-all;
}
.dm-value.strong {
  font-weight: 700;
  color: #1890ff;
  font-size: 16px;
}

.detail-items-section { padding: 4px 0; }
.detail-items-title {
  font-size: 13px;
  font-weight: 600;
  color: #555;
  margin-bottom: 8px;
}
</style>
