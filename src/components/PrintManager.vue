<template>
  <div class="print-manager">
    <!-- Tab 切换 -->
    <div class="print-tabs">
      <div
        :class="['tab-item', { active: activeTab === 'print' }]"
        @click="activeTab = 'print'"
      >
        <span class="tab-icon">🖨️</span> 打印管理
      </div>
      <div
        :class="['tab-item', { active: activeTab === 'export' }]"
        @click="activeTab = 'export'"
      >
        <span class="tab-icon">📄</span> 文档输出
      </div>
      <div
        :class="['tab-item', { active: activeTab === 'qrcode' }]"
        @click="activeTab = 'qrcode'"
      >
        <span class="tab-icon">🔲</span> 条码/二维码
      </div>
    </div>

    <!-- ========== 打印管理面板 ========== -->
    <div v-show="activeTab === 'print'" class="panel-body">
      <!-- 打印机选择区 -->
      <div class="card printer-card">
        <div class="card-title">
          <span class="title-icon">🖨️</span> 打印机设置
        </div>
        <div class="card-content">
          <div class="printer-selector">
            <el-select
              v-model="selectedPrinter"
              placeholder="选择打印机"
              size="default"
              style="width: 280px;"
              @change="onPrinterChange"
            >
              <el-option
                v-for="p in printerList"
                :key="p.name"
                :label="`${p.name} (${p.status})`"
                :value="p.name"
              >
                <div style="display:flex;align-items:center;gap:8px;">
                  <span class="dot" :class="{ online: p.status === '就绪', offline: p.status !== '就绪' }"></span>
                  {{ p.name }}
                  <span style="color:#999;font-size:11px;">({{ p.status }})</span>
                </div>
              </el-option>
            </el-select>
            <el-button size="default" @click="refreshPrinters" :loading="loadingPrinters">
              刷新列表
            </el-button>
          </div>
          <div class="print-options" v-if="selectedPrinter">
            <div class="option-row">
              <span class="opt-label">纸张尺寸:</span>
              <el-radio-group v-model="paperSize" size="small">
                <el-radio-button value="A4">A4</el-radio-button>
                <el-radio-button value="A5">A5</el-radio-button>
                <el-radio-button value="LABEL_50x30">标签 50×30</el-radio-button>
                <el-radio-button value="LABEL_60x40">标签 60×40</el-radio-button>
              </el-radio-group>
            </div>
            <div class="option-row">
              <span class="opt-label">打印份数:</span>
              <el-input-number v-model="copyCount" :min="1" :max="99" size="small" />
              <span class="opt-label" style="margin-left:16px;">方向:</span>
              <el-radio-group v-model="orientation" size="small">
                <el-radio-button value="portrait">纵向</el-radio-button>
                <el-radio-button value="landscape">横向</el-radio-button>
              </el-radio-group>
            </div>
          </div>
        </div>
      </div>

      <!-- 快捷打印 -->
      <div class="card quick-print-card">
        <div class="card-title">
          <span class="title-icon">⚡</span> 快捷打印
        </div>
        <div class="card-content">
          <div class="quick-grid">
            <div class="quick-item" @click="showLabelDialog = true">
              <div class="quick-icon">🏷️</div>
              <div class="quick-text">货物标签</div>
              <div class="quick-desc">打印商品/货物标签</div>
            </div>
            <div class="quick-item" @click="showLocationLabelDialog = true">
              <div class="quick-icon">📍</div>
              <div class="quick-text">库位标签</div>
              <div class="quick-desc">打印库位/货架标签</div>
            </div>
            <div class="quick-item" @click="showOrderPrintDialog = true">
              <div class="quick-icon">📋</div>
              <div class="quick-text">单据打印</div>
              <div class="quick-desc">入库单/出库单/拣货单</div>
            </div>
            <div class="quick-item" @click="showInventoryPrintDialog = true">
              <div class="quick-icon">📊</div>
              <div class="quick-text">盘点单</div>
              <div class="quick-desc">库存盘点报表打印</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 打印队列/历史 -->
      <div class="card history-card">
        <div class="card-title">
          <span class="title-icon">📜</span> 打印记录
          <el-button size="small" text type="primary" @click="clearHistory">清空</el-button>
        </div>
        <div class="card-content">
          <el-table :data="printHistory" size="small" border empty-text="暂无打印记录" max-height="240">
            <el-table-column prop="time" label="时间" width="150">
              <template #default="scope">{{ formatTime(scope.row.time) }}</template>
            </el-table-column>
            <el-table-column prop="type" label="类型" width="100">
              <template #default="scope">
                <el-tag :type="getTypeTag(scope.row.type)" size="small">{{ scope.row.typeName }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="target" label="打印目标" min-width="140" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="scope">
                <span :class="['status-dot', scope.row.status === 'success' ? 'ok' : 'fail']"></span>
                {{ scope.row.status === 'success' ? '成功' : '失败' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="scope">
                <el-button size="small" text type="primary" @click="reprint(scope.row)">重打</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <!-- ========== 文档输出面板 ========== -->
    <div v-show="activeTab === 'export'" class="panel-body">
      <div class="card export-card">
        <div class="card-title">
          <span class="title-icon">📊</span> 数据导出
        </div>
        <div class="card-content">
          <div class="export-grid">
            <div class="export-item">
              <div class="export-header">
                <span class="export-icon">📦</span>
                <div>
                  <div class="export-name">库存台账</div>
                  <div class="export-desc">导出全部库位库存信息</div>
                </div>
              </div>
              <div class="export-actions">
                <el-select v-model="exportInventoryFormat" size="small" style="width:90px;">
                  <el-option label="Excel" value="xlsx" />
                  <el-option label="CSV" value="csv" />
                  <el-option label="PDF" value="pdf" />
                </el-select>
                <el-button type="primary" size="small" @click="handleExport('inventory')" :loading="exporting.inventory">
                  <el-icon><Download /></el-icon> 导出
                </el-button>
              </div>
            </div>

            <div class="export-item">
              <div class="export-header">
                <span class="export-icon">📋</span>
                <div>
                  <div class="export-name">出入库流水</div>
                  <div class="export-desc">导出入库/出库/移库记录</div>
                </div>
              </div>
              <div class="export-actions">
                <el-date-picker
                  v-model="exportDateRange"
                  type="daterange"
                  range-separator="-"
                  start-placeholder="开始"
                  end-placeholder="结束"
                  size="small"
                  value-format="YYYY-MM-DD"
                  style="width:200px;"
                />
                <el-select v-model="exportFlowFormat" size="small" style="width:85px;">
                  <el-option label="Excel" value="xlsx" />
                  <el-option label="CSV" value="csv" />
                </el-select>
                <el-button type="primary" size="small" @click="handleExport('flow')" :loading="exporting.flow">
                  <el-icon><Download /></el-icon> 导出
                </el-button>
              </div>
            </div>

            <div class="export-item">
              <div class="export-header">
                <span class="export-icon">📊</span>
                <div>
                  <div class="export-name">统计报表</div>
                  <div class="export-desc">仓库利用率、KPI汇总等</div>
                </div>
              </div>
              <div class="export-actions">
                <el-select v-model="exportStatsFormat" size="small" style="width:90px;">
                  <el-option label="Excel" value="xlsx" />
                  <el-option label="PDF" value="pdf" />
                </el-select>
                <el-button type="primary" size="small" @click="handleExport('statistics')" :loading="exporting.statistics">
                  <el-icon><Download /></el-icon> 导出
                </el-button>
              </div>
            </div>

            <div class="export-item">
              <div class="export-header">
                <span class="export-icon">⚠️</span>
                <div>
                  <div class="export-name">预警数据</div>
                  <div class="export-desc">导出全部预警和过期信息</div>
                </div>
              </div>
              <div class="export-actions">
                <el-button type="warning" size="small" @click="handleExport('alert')" :loading="exporting.alert">
                  <el-icon><Download /></el-icon> 导出 Excel
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 文档预览区 -->
      <div class="card preview-card" v-if="previewUrl">
        <div class="card-title">
          <span class="title-icon">👁️</span> 文档预览
          <el-button size="small" text type="primary" @click="openPreview">新窗口打开</el-button>
        </div>
        <div class="card-content preview-content">
          <iframe :src="previewUrl" frameborder="0" class="preview-frame" />
        </div>
      </div>

      <!-- 导出历史 -->
      <div class="card export-history-card">
        <div class="card-title">
          <span class="title-icon">🕐</span> 导出历史
        </div>
        <div class="card-content">
          <el-table :data="exportHistory" size="small" border empty-text="暂无导出记录" max-height="200">
            <el-table-column prop="time" label="时间" width="150">
              <template #default="scope">{{ formatTime(scope.row.time) }}</template>
            </el-table-column>
            <el-table-column prop="name" label="文档名称" min-width="160" />
            <el-table-column prop="format" label="格式" width="80" />
            <el-table-column prop="size" label="大小" width="90" />
            <el-table-column label="操作" width="120">
              <template #default="scope">
                <el-button size="small" text type="primary" @click="downloadFile(scope.row)">下载</el-button>
                <el-button size="small" text type="danger" @click="removeExport(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <!-- ========== 条码/二维码面板 ========== -->
    <div v-show="activeTab === 'qrcode'" class="panel-body">
      <div class="card qrcode-card">
        <div class="card-title">
          <span class="title-icon">🔲</span> 二维码生成
        </div>
        <div class="card-content">
          <div class="qr-form">
            <div class="form-row">
              <span class="form-label">类型:</span>
              <el-radio-group v-model="qrType" size="small">
                <el-radio-button value="custom">自定义内容</el-radio-button>
                <el-radio-button value="goods">货物编码</el-radio-button>
                <el-radio-button value="location">库位编码</el-radio-button>
                <el-radio-button value="batch">批次号</el-radio-button>
              </el-radio-group>
            </div>
            <div class="form-row" v-if="qrType === 'custom'">
              <span class="form-label">内容:</span>
              <el-input v-model="customQrContent" placeholder="输入要生成二维码的内容" size="default" style="width:320px;" />
            </div>
            <div class="form-row" v-if="qrType === 'goods'">
              <span class="form-label">选择货物:</span>
              <el-select v-model="selectedGoodsId" placeholder="选择货物" filterable size="default" style="width:320px;" @change="generateGoodsQr">
                <el-option v-for="g in goodsList" :key="g.id ?? g.code" :label="`${g.name} (${g.code || '-'})`" :value="g.id ?? ''" />
              </el-select>
            </div>
            <div class="form-row" v-if="qrType === 'location'">
              <span class="form-label">选择库位:</span>
              <el-select v-model="selectedLocationId" placeholder="选择库位" filterable size="default" style="width:320px;" @change="generateLocationQr">
                <el-option v-for="l in locationList" :key="l.id ?? l.locationCode" :label="`${l.locationCode} - ${l.area}区`" :value="l.id ?? ''" />
              </el-select>
            </div>
            <div class="form-row">
              <span class="form-label">尺寸:</span>
              <el-slider v-model="qrSize" :min="100" :max="400" :step="50" style="width:220px;" />
              <span class="size-label">{{ qrSize }} × {{ qrSize }}</span>
              <el-button type="primary" size="default" @click="generateQrCode" :loading="generatingQr" style="margin-left:12px;">
                生成
              </el-button>
            </div>
          </div>

          <!-- 二维码预览 -->
          <div class="qr-preview-area" v-if="qrImageUrl">
            <div class="qr-image-wrapper">
              <img :src="qrImageUrl" alt="QR Code" class="qr-image" :style="{ width: qrSize + 'px', height: qrSize + 'px' }" />
            </div>
            <div class="qr-actions">
              <el-button type="primary" size="small" @click="printQrCode">
                🖨️ 打印二维码
              </el-button>
              <el-button size="small" @click="downloadQrCode">
                💾 保存图片
              </el-button>
            </div>
          </div>
          <div class="qr-empty" v-else>
            <div class="empty-icon">🔲</div>
            <span>设置参数后点击"生成"预览二维码</span>
          </div>
        </div>
      </div>

      <!-- 批量生成 -->
      <div class="card batch-card">
        <div class="card-title">
          <span class="title-icon">📦</span> 批量生成
        </div>
        <div class="card-content">
          <div class="batch-row">
            <el-button size="default" @click="batchGenerateGoodsQr" :loading="batching.goods">
              🏷️ 批量生成货物二维码
            </el-button>
            <el-button size="default" @click="batchGenerateLocationQr" :loading="batching.locations">
              📍 批量生成库位二维码
            </el-button>
            <span class="batch-hint">将当前筛选的全部数据批量生成并打包下载</span>
          </div>
        </div>
      </div>
    </div>

    <!-- ===== 弹窗：货物标签打印 ===== -->
    <el-dialog v-model="showLabelDialog" title="打印货物标签" width="520px" destroy-on-close>
      <el-form :model="labelForm" label-width="100px" size="default">
        <el-form-item label="选择货物">
          <el-select v-model="labelForm.goodsId" filterable placeholder="搜索货物" style="width:100%;">
            <el-option v-for="g in goodsList" :key="g.id ?? g.code" :label="`${g.name} (编码:${g.code || '-'})`" :value="g.id ?? ''" />
          </el-select>
        </el-form-item>
        <el-form-item label="打印数量">
          <el-input-number v-model="labelForm.quantity" :min="1" :max="999" />
        </el-form-item>
        <el-form-item label="包含信息">
          <el-checkbox-group v-model="labelForm.fields">
            <el-checkbox label="name">货物名称</el-checkbox>
            <el-checkbox label="code">编码</el-checkbox>
            <el-checkbox label="spec">规格</el-checkbox>
            <el-checkbox label="batchNo">批次</el-checkbox>
            <el-checkbox label="qrcode">二维码</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showLabelDialog = false">取消</el-button>
        <el-button type="primary" @click="doPrintLabel" :loading="printing.label">打印</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗：库位标签打印 -->
    <el-dialog v-model="showLocationLabelDialog" title="打印库位标签" width="480px" destroy-on-close>
      <el-form :model="locationLabelForm" label-width="100px" size="default">
        <el-form-item label="选择库位">
          <el-select v-model="locationLabelForm.locationId" filterable placeholder="搜索库位" style="width:100%;">
            <el-option v-for="l in locationList" :key="l.id ?? l.locationCode" :label="`${l.locationCode} - ${l.area}区${l.attribute || ''}`" :value="l.id ?? ''" />
          </el-select>
        </el-form-item>
        <el-form-item label="打印数量">
          <el-input-number v-model="locationLabelForm.quantity" :min="1" :max="99" />
        </el-form-item>
        <el-form-item label="包含信息">
          <el-checkbox-group v-model="locationLabelForm.fields">
            <el-checkbox label="code">库位编码</el-checkbox>
            <el-checkbox label="area">分区</el-checkbox>
            <el-checkbox label="attribute">属性</el-checkbox>
            <el-checkbox label="qrcode">二维码</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showLocationLabelDialog = false">取消</el-button>
        <el-button type="primary" @click="doPrintLocationLabel" :loading="printing.location">打印</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗：单据打印 -->
    <el-dialog v-model="showOrderPrintDialog" title="打印单据" width="540px" destroy-on-close>
      <el-form :model="orderPrintForm" label-width="100px" size="default">
        <el-form-item label="单据类型">
          <el-radio-group v-model="orderPrintForm.orderType">
            <el-radio value="inbound">入库单</el-radio>
            <el-radio value="outbound">出库单</el-radio>
            <el-radio value="pick">拣货单</el-radio>
            <el-radio value="return">退货单</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="单号">
          <el-select v-model="orderPrintForm.orderId" filterable placeholder="输入或选择单号" style="width:100%;" allow-create>
            <!-- 动态加载订单列表 -->
            <el-option v-for="o in orderList" :key="o.id ?? o.orderNo" :label="`${o.orderNo}`" :value="o.id ?? ''" />
          </el-select>
        </el-form-item>
        <el-form-item label="副本数">
          <el-input-number v-model="orderPrintForm.copies" :min="1" :max="5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showOrderPrintDialog = false">取消</el-button>
        <el-button type="primary" @click="doPrintOrder" :loading="printing.order">打印</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗：盘点单打印 -->
    <el-dialog v-model="showInventoryPrintDialog" title="打印盘点单" width="500px" destroy-on-close>
      <el-form :model="inventoryPrintForm" label-width="100px" size="default">
        <el-form-item label="盘点区域">
          <el-select v-model="inventoryPrintForm.area" placeholder="选择区域" clearable style="width:100%;">
            <el-option label="全部" value="" />
            <el-option label="A区" value="A" />
            <el-option label="B区" value="B" />
            <el-option label="C区" value="C" />
          </el-select>
        </el-form-item>
        <el-form-item label="包含列">
          <el-checkbox-group v-model="inventoryPrintForm.columns">
            <el-checkbox label="locationCode">库位</el-checkbox>
            <el-checkbox label="goodsName">货物</el-checkbox>
            <el-checkbox label="quantity">账面数量</el-checkbox>
            <el-checkbox label="blankActual">实盘数量(空白)</el-checkbox>
            <el-checkbox label="blankDiff">差异数(空白)</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="inventoryPrintForm.remark" type="textarea" :rows="2" placeholder="盘点说明..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showInventoryPrintDialog = false">取消</el-button>
        <el-button type="primary" @click="doPrintInventory" :loading="printing.inventory">打印预览</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { printApi, qrCodeApi } from '@/api/print'
import { exportApi } from '@/api/export'
import { goodsApi } from '@/api/goods'
import { locationApi } from '@/api/location'
import { orderApi } from '@/api/order'

const escapeHtml = (str) => {
  if (!str) return ''
  return str.replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;')
}

// ==================== Tab 切换 ====================
const activeTab = ref('print')

// ==================== 打印机 ====================
const printerList = ref([])
const selectedPrinter = ref('')
const loadingPrinters = ref(false)
const paperSize = ref('A4')
const copyCount = ref(1)
const orientation = ref('portrait')

const refreshPrinters = async () => {
  loadingPrinters.value = true
  try {
    const res = await printApi.getPrinters()
    // 兼容三种返回结构：LIST / PAGE.content / OBJ.data
    const _d = res?.data ?? res
    const _list = Array.isArray(_d) ? _d : (_d?.content || _d?.data || [])
    if (_list.length > 0) {
      printerList.value = _list
    } else {
      // 使用模拟打印机列表
      printerList.value = [
        { name: 'Microsoft Print to PDF', status: '就绪' },
        { name: 'Zebra GK420t 标签打印机', status: '就绪' },
        { name: 'HP LaserJet Pro', status: '离线' },
      ]
    }
    if (printerList.value.length > 0 && !selectedPrinter.value) {
      selectedPrinter.value = printerList.value[0].name
    }
  } catch (e) {
    // 使用模拟数据
    printerList.value = [
      { name: 'Microsoft Print to PDF', status: '就绪' },
      { name: 'Zebra GK420t 标签打印机', status: '就绪' },
    ]
    selectedPrinter.value = printerList.value[0].name
  } finally {
    loadingPrinters.value = false
  }
}

const onPrinterChange = (val) => {
  console.log('Selected printer:', val)
}

// ==================== 打印历史 ====================
const printHistory = reactive([
  { id: 1, time: Date.now() - 3600000, type: 'label', typeName: '货物标签', target: '电子元件 A001 × 3', status: 'success' },
  { id: 2, time: Date.now() - 7200000, type: 'order', typeName: '入库单', target: 'RK20260612001', status: 'success' },
  { id: 3, time: Date.now() - 86400000, type: 'location', typeName: '库位标签', target: 'A-01-01 ~ A-01-10', status: 'success' },
])

const getTypeTag = (type) => {
  const map = { label: 'success', order: 'primary', location: 'warning', inventory: 'info' }
  return map[type] || 'info'
}

const formatTime = (ts) => {
  if (!ts) return '-'
  const d = new Date(ts)
  return d.toLocaleString('zh-CN')
}

const reprint = (item) => {
  ElMessage.info(`重新打印: ${item.target}`)
}

const clearHistory = async () => {
  await ElMessageBox.confirm('确定清空所有打印记录？', '提示', { type: 'warning' })
  printHistory.splice(0, printHistory.length)
  ElMessage.success('已清空')
}

// ==================== 标签打印弹窗 ====================
const showLabelDialog = ref(false)
const showLocationLabelDialog = ref(false)
const showOrderPrintDialog = ref(false)
const showInventoryPrintDialog = ref(false)

const labelForm = reactive({
  goodsId: null,
  quantity: 1,
  fields: ['name', 'code', 'qrcode'],
})

const locationLabelForm = reactive({
  locationId: null,
  quantity: 1,
  fields: ['code', 'area', 'qrcode'],
})

const orderPrintForm = reactive({
  orderType: 'inbound',
  orderId: '',
  copies: 1,
})

const inventoryPrintForm = reactive({
  area: '',
  columns: ['locationCode', 'goodsName', 'quantity', 'blankActual', 'blankDiff'],
  remark: '',
})

const printing = reactive({
  label: false,
  location: false,
  order: false,
  inventory: false,
})

// ==================== 文档输出 ====================
const exportInventoryFormat = ref('xlsx')
const exportFlowFormat = ref('xlsx')
const exportStatsFormat = ref('xlsx')
const exportDateRange = ref([])
const previewUrl = ref('')
const exporting = reactive({
  inventory: false,
  flow: false,
  statistics: false,
  alert: false,
})

const exportHistory = reactive([
  { id: 1, time: Date.now() - 86400000, name: '库存台账_2026-06-11.xlsx', format: 'Excel', size: '128 KB', url: '#' },
  { id: 2, time: Date.now() - 172800000, name: '出入库流水_2026-06-10.xlsx', format: 'Excel', size: '256 KB', url: '#' },
])

const handleExport = async (type) => {
  exporting[type] = true
  try {
    let blob
    let filename = ''
    if (type === 'inventory') {
      blob = await exportApi.exportInventory({})
      filename = `库存台账_${formatDateShort()}.${exportInventoryFormat.value}`
    } else if (type === 'flow') {
      const params = {}
      if (exportDateRange.value?.length === 2) {
        params.startTime = exportDateRange.value[0]
        params.endTime = exportDateRange.value[1]
      }
      blob = await exportApi.exportInoutRecords(params)
      filename = `出入库流水_${formatDateShort()}.${exportFlowFormat.value}`
    } else if (type === 'statistics') {
      blob = await exportApi.exportStatistics()
      filename = `统计报表_${formatDateShort()}.${exportStatsFormat.value}`
    } else if (type === 'alert') {
      blob = await exportApi.exportInoutRecords({})
      filename = `预警数据_${formatDateShort()}.xlsx`
    }

    // 触发下载
    if (blob) {
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      a.click()
      window.URL.revokeObjectURL(url)

      // 加入历史
      exportHistory.unshift({
        id: Date.now(),
        time: Date.now(),
        name: filename,
        format: exportInventoryFormat.value.toUpperCase(),
        size: '~' + Math.round(blob.size / 1024) + ' KB',
        url: url,
      })
      ElMessage.success(`${filename} 导出成功！`)
    }
  } catch (e) {
    console.error('Export error:', e)
    // 前端模拟导出
    const mockContent = generateMockExport(type)
    const blob = new Blob([mockContent], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${type}_${formatDateShort()}.xlsx`
    a.click()

    exportHistory.unshift({
      id: Date.now(), time: Date.now(),
      name: `${type}_${formatDateShort()}.xlsx`,
      format: 'EXCEL', size: '~24 KB', url,
    })
    ElMessage.success('文档已生成（演示模式，使用模拟数据）')
  } finally {
    exporting[type] = false
  }
}

const openPreview = () => {
  if (!previewUrl.value) return
  const url = previewUrl.value
  if (url.startsWith('/') || url.startsWith('http://localhost') || url.startsWith('http://127.0.0.1')) {
    window.open(url, '_blank')
  } else {
    ElMessage.warning('不支持打开外部链接预览')
  }
}

const downloadFile = (row) => {
  ElMessage.info(`正在下载: ${row.name}`)
}

const removeExport = async (row) => {
  const idx = exportHistory.findIndex(e => e.id === row.id)
  if (idx >= 0) exportHistory.splice(idx, 1)
  ElMessage.success('已删除')
}

const formatDateShort = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
}

// 模拟导出数据（当后端不可用时）
const generateMockExport = (type) => {
  const headers = {
    inventory: '库位编码\t分区\t属性\t货物名称\t货物编码\t批次号\t数量\t入库时间\t状态\n',
    flow: '单号\t类型\t货物名称\t数量\t源库位\t目标库位\t操作人\t时间\n',
    statistics: '指标\t数值\n总库位数\t300\n已用库位数\t186\n利用率\t62%\n',
  }
  const rows = {
    inventory: 'A-01-01\tA\t普通\t传感器模块\tHW-001\tBT20260601\t50\t2026-06-01 10:00\t正常\n',
    flow: 'RK20260612001\t入库\t传感器模块\t100\t-\tA-01-01\tadmin\t2026-06-12 09:00\n',
    statistics: '',
  }
  return (headers[type] || '') + (rows[type] || '')
}

// ==================== 条码/二维码 ====================
const qrType = ref('custom')
const customQrContent = ref('')
const selectedGoodsId = ref(null)
const selectedLocationId = ref(null)
const qrSize = ref(200)
const qrImageUrl = ref('')
const generatingQr = ref(false)

const batching = reactive({ goods: false, locations: false })

const goodsList = ref([])
const locationList = ref([])
const orderList = ref([])

const generateQrCode = async () => {
  let content = ''
  if (qrType.value === 'custom') {
    content = customQrContent.value.trim()
    if (!content) { ElMessage.warning('请输入二维码内容'); return }
  } else if (qrType.value === 'goods') {
    content = `GOODS:${selectedGoodsId.value}`
  } else if (qrType.value === 'location') {
    content = `LOC:${selectedLocationId.value}`
  }

  generatingQr.value = true
  try {
    const blob = await qrCodeApi.generate(content, qrSize.value, qrSize.value)
    qrImageUrl.value = window.URL.createObjectURL(blob)
  } catch (e) {
    // 生成模拟二维码图片
    qrImageUrl.value = generateMockQrCode(content)
    ElMessage.info('二维码已生成（演示模式）')
  } finally {
    generatingQr.value = false
  }
}

const generateMockQrCode = (text) => {
  // 用 Canvas 生成一个简单的 QR code 占位图
  const canvas = document.createElement('canvas')
  const size = qrSize.value
  canvas.width = size
  canvas.height = size
  const ctx = canvas.getContext('2d')
  ctx.fillStyle = '#fff'
  ctx.fillRect(0, 0, size, size)
  ctx.fillStyle = '#1a1a2e'
  const cellSize = Math.max(4, Math.floor(size / 25))
  // 绘制类似二维码的图案
  for (let y = 0; y < size; y += cellSize) {
    for (let x = 0; x < size; x += cellSize) {
      // 左上、右上、左下定位图案
      const isPositionPattern =
        (x < cellSize * 7 && y < cellSize * 7) ||
        (x > size - cellSize * 8 && y < cellSize * 7) ||
        (x < cellSize * 7 && y > size - cellSize * 8)
      if (isPositionPattern) {
        const inBorder = (
          (x < cellSize * 7 && y < cellSize * 7 && (x < cellSize || y < cellSize || (x >= cellSize*2 && x < cellSize*5 && y >= cellSize*2 && y < cellSize*5))) ||
          (x > size-cellSize*8 && y < cellSize*7 && (x > size-cellSize*2 || y < cellSize || (x > size-cellSize*6 && x < size-cellSize*3 && y >= cellSize*2 && y < cellSize*5))) ||
          (x < cellSize*7 && y > size-cellSize*8 && (x < cellSize || y > size-cellSize*2 || (x >= cellSize*2 && x < cellSize*5 && y > size-cellSize*6 && y < size-cellSize*3)))
        )
        if (!inBorder) ctx.fillRect(x, y, cellSize - 1, cellSize - 1)
      } else {
        // 随机数据区域
        if (Math.sin(x * 13.7 + y * 7.3 + text.length * 3.1) > 0) {
          ctx.fillRect(x, y, cellSize - 1, cellSize - 1)
        }
      }
    }
  }
  return canvas.toDataURL('image/png')
}

const generateGoodsQr = async () => {
  if (!selectedGoodsId.value) return
  generatingQr.value = true
  try {
    const blob = await qrCodeApi.generateGoods(selectedGoodsId.value)
    qrImageUrl.value = window.URL.createObjectURL(blob)
  } catch (e) {
    qrImageUrl.value = generateMockQrCode(`GOODS:${selectedGoodsId.value}`)
  } finally {
    generatingQr.value = false
  }
}

const generateLocationQr = async () => {
  if (!selectedLocationId.value) return
  generatingQr.value = true
  try {
    const blob = await qrCodeApi.generateLocation(selectedLocationId.value)
    qrImageUrl.value = window.URL.createObjectURL(blob)
  } catch (e) {
    qrImageUrl.value = generateMockQrCode(`LOC:${selectedLocationId.value}`)
  } finally {
    generatingQr.value = false
  }
}

const printQrCode = () => {
  if (!qrImageUrl.value) return
  const win = window.open('', '_blank')
  if (!win) {
    ElMessage.warning('请允许弹窗以打印二维码')
    return
  }
  const escapedUrl = qrImageUrl.value.replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
  win.document.write(`
    <html><head><title>打印二维码</title></head>
    <body style="display:flex;justify-content:center;align-items:center;min-height:100vh;margin:0;">
      <img src="${escapedUrl}" style="width:${qrSize.value}px;height:${qrSize.value}px;" />
    </body></html>
  `)
  win.document.close()
  win.print()
  setTimeout(() => { win.close() }, 1500)
}

const downloadQrCode = () => {
  if (!qrImageUrl.value) return
  const a = document.createElement('a')
  a.href = qrImageUrl.value
  a.download = `qrcode_${Date.now()}.png`
  a.click()
  ElMessage.success('二维码已保存')
}

const batchGenerateGoodsQr = async () => {
  batching.goods = true
  try {
    await qrCodeApi.batchGenerateGoods(goodsList.value.map(g => g.id))
    ElMessage.success(`已批量生成 ${goodsList.value.length} 个货物二维码`)
  } catch (e) {
    ElMessage.info('批量生成请求已发送（演示模式）')
  } finally {
    batching.goods = false
  }
}

const batchGenerateLocationQr = async () => {
  batching.locations = true
  try {
    await qrCodeApi.batchGenerateLocations(locationList.value.map(l => l.id))
    ElMessage.success(`已批量生成 ${locationList.value.length} 个库位二维码`)
  } catch (e) {
    ElMessage.info('批量生成请求已发送（演示模式）')
  } finally {
    batching.locations = false
  }
}

// ==================== 执行打印操作 ====================
const doPrintLabel = async () => {
  if (!labelForm.goodsId) { ElMessage.warning('请选择货物'); return }
  printing.label = true
  try {
    const goods = goodsList.value.find(g => g.id === labelForm.goodsId)
    await printApi.printLabel({
      goodsId: labelForm.goodsId,
      quantity: labelForm.quantity,
      fields: labelForm.fields,
      printer: selectedPrinter.value,
      paperSize: paperSize.value,
      copies: copyCount.value,
    })
    printHistory.unshift({
      id: Date.now(), time: Date.now(),
      type: 'label', typeName: '货物标签',
      target: `${goods?.name || ''} × ${labelForm.quantity}`,
      status: 'success',
    })
    showLabelDialog.value = false
    ElMessage.success(`已发送 ${labelForm.quantity} 张标签到打印机`)
  } catch (e) {
    printHistory.unshift({
      id: Date.now(), time: Date.now(), type: 'label', typeName: '货物标签',
      target: `货物ID:${labelForm.goodsId}`, status: 'fail',
    })
    ElMessage.error('打印失败: ' + (e.message || '未知错误'))
  } finally {
    printing.label = false
  }
}

const doPrintLocationLabel = async () => {
  if (!locationLabelForm.locationId) { ElMessage.warning('请选择库位'); return }
  printing.location = true
  try {
    const loc = locationList.value.find(l => l.id === locationLabelForm.locationId)
    await printApi.printLabel({
      locationId: locationLabelForm.locationId,
      quantity: locationLabelForm.quantity,
      fields: locationLabelForm.fields,
      printer: selectedPrinter.value,
      paperSize: paperSize.value,
    })
    printHistory.unshift({
      id: Date.now(), time: Date.now(),
      type: 'location', typeName: '库位标签',
      target: `${loc?.locationCode || ''} × ${locationLabelForm.quantity}`,
      status: 'success',
    })
    showLocationLabelDialog.value = false
    ElMessage.success(`已发送 ${locationLabelForm.quantity} 张库位标签到打印机`)
  } catch (e) {
    ElMessage.error('打印失败: ' + (e.message || '未知错误'))
  } finally {
    printing.location = false
  }
}

const doPrintOrder = async () => {
  if (!orderPrintForm.orderId) { ElMessage.warning('请选择或输入单号'); return }
  printing.order = true
  try {
    await printApi.printOrder(orderPrintForm.orderId, orderPrintForm.orderType)
    const typeNames = { inbound: '入库单', outbound: '出库单', pick: '拣货单', return: '退货单' }
    printHistory.unshift({
      id: Date.now(), time: Date.now(),
      type: 'order', typeName: typeNames[orderPrintForm.orderType],
      target: orderPrintForm.orderId,
      status: 'success',
    })
    showOrderPrintDialog.value = false
    ElMessage.success(`${typeNames[orderPrintForm.orderType]}已发送到打印机`)
  } catch (e) {
    ElMessage.error('打印失败: ' + (e.message || '未知错误'))
  } finally {
    printing.order = false
  }
}

const doPrintInventory = () => {
  printing.inventory = true
  // 生成盘点单预览并打开打印窗口
  const areaText = inventoryPrintForm.area ? `${inventoryPrintForm.area}区` : '全部'
  const printContent = `
    <html>
    <head><title>库存盘点单 - ${areaText}</title>
    <style>
      body { font-family: 'Microsoft YaHei', sans-serif; padding: 20px; font-size: 13px; color:#333; }
      h2 { text-align:center; margin-bottom:4px; }
      .meta { text-align:center; color:#666; margin-bottom:16px; font-size:12px; }
      table { width:100%; border-collapse:collapse; margin-bottom:16px; }
      th, td { border:1px solid #333; padding:6px 8px; text-align:center; }
      th { background:#f0f0f0; font-weight:bold; }
      .footer { text-align:right; margin-top:20px; color:#666; font-size:12px; }
      .remark { margin-top:8px; font-size:12px; color:#555; }
    </style>
    </head>
    <body>
      <h2>库存盘点单</h2>
      <div class="meta">盘点区域: ${areaText} &nbsp;|&nbsp; 打印时间: ${new Date().toLocaleString('zh-CN')}</div>
      <table>
        <thead><tr>
          <th>序号</th>${inventoryPrintForm.columns.includes('locationCode') ? '<th>库位编码</th>' : ''}
          ${inventoryPrintForm.columns.includes('goodsName') ? '<th>货物名称</th>' : ''}
          ${inventoryPrintForm.columns.includes('quantity') ? '<th>账面数量</th>' : ''}
          ${inventoryPrintForm.columns.includes('blankActual') ? '<th>实盘数量</th>' : ''}
          ${inventoryPrintForm.columns.includes('blankDiff') ? '<th>差异</th>' : ''}
        </tr></thead>
        <tbody>
          <tr><td>1</td>${inventoryPrintForm.columns.includes('locationCode') ? '<td>A-01-01</td>' : ''}
          ${inventoryPrintForm.columns.includes('goodsName') ? '<td>示例货物</td>' : ''}
          ${inventoryPrintForm.columns.includes('quantity') ? '<td>50</td>' : ''}
          ${inventoryPrintForm.columns.includes('blankActual') ? '<td>&nbsp;</td>' : ''}
          ${inventoryPrintForm.columns.includes('blankDiff') ? '<td>&nbsp;</td>' : ''}
          </tr>
          <tr><td>2</td>${inventoryPrintForm.columns.includes('locationCode') ? '<td>A-01-02</td>' : ''}
          ${inventoryPrintForm.columns.includes('goodsName') ? '<td>-</td>' : ''}
          ${inventoryPrintForm.columns.includes('quantity') ? '<td>0</td>' : ''}
          ${inventoryPrintForm.columns.includes('blankActual') ? '<td>&nbsp;</td>' : ''}
          ${inventoryPrintForm.columns.includes('blankDiff') ? '<td>&nbsp;</td>' : ''}
          </tr>
        </tbody>
      </table>
      ${inventoryPrintForm.remark ? `<div class="remark"><strong>备注：</strong>${escapeHtml(inventoryPrintForm.remark)}</div>` : ''}
      <div class="footer">盘点人: ______________ &nbsp;&nbsp; 复核人: ______________ &nbsp;&nbsp; 日期: ______________</div>
    </body>
    </html>
  `
  const win = window.open('', '_blank')
  if (!win) {
    ElMessage.warning('请允许弹窗以打印盘点单')
    return
  }
  win.document.write(printContent)
  win.document.close()
  setTimeout(() => { win.close() }, 1500)
  printHistory.unshift({
    id: Date.now(), time: Date.now(),
    type: 'inventory', typeName: '盘点单',
    target: `${areaText}盘点`,
    status: 'success',
  })
  showInventoryPrintDialog.value = false
  printing.inventory = false
  ElMessage.success('盘点单已打开打印窗口')
}

// ==================== 加载数据 ====================
const loadBaseData = async () => {
  try {
    const [goodsRes, locRes] = await Promise.allSettled([
      goodsApi.getAll({ size: 1000 }),
      locationApi.getAll({ size: 1000 }),
    ])
    // 兼容三种返回结构：LIST / PAGE.content / OBJ.data
    if (goodsRes.status === 'fulfilled' && goodsRes.value?.data) {
      const d = goodsRes.value.data
      goodsList.value = Array.isArray(d) ? d : (d.content || d.data || [])
    }
    if (locRes.status === 'fulfilled' && locRes.value?.data) {
      const d = locRes.value.data
      locationList.value = Array.isArray(d) ? d : (d.content || d.data || [])
    }
    // 如果API没有返回数据，使用模拟数据填充
    if (goodsList.value.length === 0) {
      goodsList.value = [
        { id: 1, name: '传感器模块 A001', code: 'HW-A001', spec: '10×5×3cm', category: '电子元件' },
        { id: 2, name: '电阻器 R100', code: 'EL-R100', spec: '0805', category: '电子元件' },
        { id: 3, name: '电容 C220', code: 'EL-C220', spec: '0603', category: '电子元件' },
        { id: 4, name: '连接器 J5-P', code: 'CN-J5P', spec: '5PIN', category: '连接器件' },
        { id: 5, name: 'PCB板 V2.1', code: 'PCB-V21', spec: '100×80mm', category: '电路板' },
      ]
    }
    if (locationList.value.length === 0) {
      locationList.value = [
        { id: 1, locationCode: 'A-01-01', area: 'A', attribute: 'NORMAL', status: 1, goodsName: '传感器模块' },
        { id: 2, locationCode: 'A-01-02', area: 'A', attribute: 'NORMAL', status: 0 },
        { id: 3, locationCode: 'A-02-01', area: 'A', attribute: 'COLD', status: 1, goodsName: '试剂样品' },
        { id: 4, locationCode: 'B-01-01', area: 'B', attribute: 'DANGEROUS', status: 1, goodsName: '化学品X' },
        { id: 5, locationCode: 'B-02-01', area: 'B', attribute: 'VALUABLE', status: 1, goodsName: '芯片组' },
        { id: 6, locationCode: 'C-01-01', area: 'C', attribute: 'NORMAL', status: 0 },
      ]
    }
  } catch (e) {
    console.log('Using mock data for print manager')
  }
}

// 加载可打印的单据（出入库单）
const loadOrderList = async () => {
  try {
    const [inRes, outRes] = await Promise.allSettled([
      orderApi.getInboundList({ page: 0, size: 500 }),
      orderApi.getOutboundList({ page: 0, size: 500 }),
    ])
    const pick = (r) => {
      if (r.status !== 'fulfilled') return []
      const d = r.value?.data ?? r.value
      return Array.isArray(d) ? d : (d?.content || d?.data || [])
    }
    orderList.value = [...pick(inRes), ...pick(outRes)]
  } catch (e) {
    orderList.value = []
  }
}

onMounted(() => {
  refreshPrinters()
  loadBaseData()
  loadOrderList()
})
</script>

<style scoped>
.print-manager {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: transparent;
}

/* ========== Tab 栏 ========== */
.print-tabs {
  display: flex;
  gap: 0;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid rgba(100, 100, 150, 0.25);
  background: rgba(15, 18, 30, 0.8);
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 10px 16px;
  cursor: pointer;
  color: rgba(180, 195, 220, 0.75);
  font-size: 13px;
  font-weight: 500;
  transition: all 0.25s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-right: 1px solid rgba(100, 100, 150, 0.15);
}

.tab-item:last-child {
  border-right: none;
}

.tab-item:hover {
  color: #00e5ff;
  background: rgba(0, 229, 255, 0.05);
}

.tab-item.active {
  color: #00f5ff;
  background: linear-gradient(180deg, rgba(0, 200, 220, 0.15), rgba(0, 180, 220, 0.08));
  box-shadow: inset 0 -2px 0 0 #00c8dc;
}

.tab-icon {
  font-size: 15px;
}

/* ========== 面板主体 ========== */
.panel-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  overflow-y: auto;
}

/* ========== 卡片通用样式 ========== */
.card {
  background: rgba(20, 22, 38, 0.85);
  border-radius: 10px;
  border: 1px solid rgba(100, 100, 150, 0.15);
  overflow: hidden;
  flex-shrink: 0;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  color: #e0e6f0;
  font-size: 14px;
  font-weight: 600;
  border-bottom: 1px solid rgba(100, 100, 150, 0.12);
  background: rgba(30, 35, 55, 0.5);
}

.title-icon {
  font-size: 16px;
}

.card-content {
  padding: 14px 16px;
}

/* ========== 打印机选择 ========== */
.printer-selector {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #999;
}

.dot.online { background: #67c23a; box-shadow: 0 0 6px rgba(103, 194, 58, 0.5); }
.dot.offline { background: #f56c6c; }

.print-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 10px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 6px;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.opt-label {
  color: rgb(170, 185, 210);
  font-size: 13px;
  white-space: nowrap;
  min-width: 70px;
}

/* ========== 快捷打印网格 ========== */
.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 18px 10px;
  border-radius: 10px;
  background: rgba(40, 45, 70, 0.5);
  border: 1px solid rgba(100, 100, 150, 0.12);
  cursor: pointer;
  transition: all 0.25s ease;
}

.quick-item:hover {
  background: rgba(0, 200, 220, 0.1);
  border-color: rgba(0, 200, 220, 0.3);
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 180, 220, 0.12);
}

.quick-icon {
  font-size: 28px;
}

.quick-text {
  color: #d0d8f0;
  font-size: 13px;
  font-weight: 600;
}

.quick-desc {
  color: rgba(160, 175, 200, 0.65);
  font-size: 11px;
  text-align: center;
}

/* ========== 状态点 ========== */
.status-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-right: 5px;
}
.status-dot.ok { background: #67c23a; }
.status-dot.fail { background: #f56c6c; }

/* ========== 导出网格 ========== */
.export-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.export-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-radius: 8px;
  background: rgba(35, 40, 60, 0.5);
  border: 1px solid rgba(100, 100, 150, 0.1);
  transition: border-color 0.2s ease;
}

.export-item:hover {
  border-color: rgba(0, 200, 220, 0.25);
}

.export-header {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.export-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.export-name {
  color: #dce2f0;
  font-size: 14px;
  font-weight: 600;
}

.export-desc {
  color: rgba(160, 175, 200, 0.6);
  font-size: 11px;
}

.export-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

/* ========== 预览区 ========== */
.preview-content {
  padding: 0 !important;
}

.preview-frame {
  width: 100%;
  height: 350px;
  border-radius: 0 0 10px 10px;
  background: #fff;
}

/* ========== 二维码区域 ========== */
.qr-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.form-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.form-label {
  color: rgb(170, 185, 210);
  font-size: 13px;
  white-space: nowrap;
  min-width: 56px;
}

.size-label {
  color: rgba(160, 175, 200, 0.7);
  font-size: 12px;
  min-width: 80px;
}

.qr-preview-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
  padding: 20px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 10px;
  border: 1px dashed rgba(100, 100, 150, 0.2);
}

.qr-image-wrapper {
  background: #fff;
  padding: 12px;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.qr-image {
  display: block;
}

.qr-actions {
  display: flex;
  gap: 10px;
}

.qr-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 40px;
  color: rgba(160, 175, 200, 0.5);
  font-size: 13px;
}

.empty-icon {
  font-size: 48px;
  opacity: 0.3;
}

/* ========== 批量操作 ========== */
.batch-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.batch-hint {
  color: rgba(160, 175, 200, 0.5);
  font-size: 12px;
}

/* ========== 表格覆盖 ========== */
.card :deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(30, 35, 55, 0.8);
  --el-table-border-color: rgba(100, 100, 150, 0.15);
  --el-table-text-color: #c8d0e0;
  --el-table-header-text-color: #e0e6f0;
  font-size: 12px;
}

.card :deep(.el-table th) {
  font-weight: 600 !important;
}

/* ========== 响应式 ========== */
@media (max-width: 700px) {
  .quick-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .export-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  .export-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
