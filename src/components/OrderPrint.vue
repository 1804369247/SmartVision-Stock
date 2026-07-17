<template>
  <div class="order-print-modal">
    <el-dialog
      :title="printTitle"
      :visible="visible"
      width="800px"
      :close-on-click-modal="false"
    >
      <div class="print-container" ref="printRef">
        <!-- 入库单模板 -->
        <div v-if="orderType === 'inbound'" class="order-template">
          <div class="template-header">
            <div class="company-info">
              <h1>入库单</h1>
              <div class="company-name">智视仓储管理系统</div>
              <div class="company-address">地址：XX市XX区XX路XX号</div>
            </div>
            <div class="order-info">
              <div class="info-row">
                <span class="label">单据编号：</span>
                <span class="value">{{ orderData.orderNo }}</span>
              </div>
              <div class="info-row">
                <span class="label">入库类型：</span>
                <span class="value">{{ getInboundTypeText(orderData.type) }}</span>
              </div>
              <div class="info-row">
                <span class="label">入库日期：</span>
                <span class="value">{{ formatDate(orderData.createTime) }}</span>
              </div>
            </div>
          </div>

          <div class="supplier-section">
            <div class="section-title">供应商信息</div>
            <div class="info-grid">
              <div class="info-item">
                <span class="label">供应商名称：</span>
                <span class="value">{{ orderData.supplierName }}</span>
              </div>
              <div class="info-item">
                <span class="label">联系人：</span>
                <span class="value">{{ orderData.supplierContact || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">联系电话：</span>
                <span class="value">{{ orderData.supplierPhone || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">备注：</span>
                <span class="value">{{ orderData.remark || '-' }}</span>
              </div>
            </div>
          </div>

          <div class="items-section">
            <div class="section-title">入库明细</div>
            <table class="items-table">
              <thead>
                <tr>
                  <th>序号</th>
                  <th>商品编码</th>
                  <th>商品名称</th>
                  <th>规格型号</th>
                  <th>批次号</th>
                  <th>单位</th>
                  <th>数量</th>
                  <th>目标库位</th>
                  <th>保质期</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, index) in orderData.items" :key="index">
                  <td>{{ index + 1 }}</td>
                  <td>{{ item.goodsCode }}</td>
                  <td>{{ item.goodsName }}</td>
                  <td>{{ item.spec || '-' }}</td>
                  <td>{{ item.batchNo }}</td>
                  <td>{{ item.unit || '件' }}</td>
                  <td>{{ item.quantity }}</td>
                  <td>{{ item.locationCode }}</td>
                  <td>{{ item.expiryDate || '-' }}</td>
                </tr>
              </tbody>
              <tfoot>
                <tr>
                  <td colspan="6" class="total-label">合计</td>
                  <td class="total-value">{{ totalQuantity }}</td>
                  <td colspan="2"></td>
                </tr>
              </tfoot>
            </table>
          </div>

          <div class="footer-section">
            <div class="sign-row">
              <div class="sign-item">
                <span class="label">制单人：</span>
                <span class="value">{{ orderData.creator || '-' }}</span>
              </div>
              <div class="sign-item">
                <span class="label">审核人：</span>
                <span class="value">{{ orderData.auditor || '-' }}</span>
              </div>
              <div class="sign-item">
                <span class="label">入库人：</span>
                <span class="value">{{ orderData.operator || '-' }}</span>
              </div>
            </div>
            <div class="sign-row">
              <div class="sign-item">
                <span class="label">制单时间：</span>
                <span class="value">{{ formatDate(orderData.createTime) }}</span>
              </div>
              <div class="sign-item">
                <span class="label">审核时间：</span>
                <span class="value">{{ formatDate(orderData.auditTime) || '-' }}</span>
              </div>
              <div class="sign-item">
                <span class="label">入库时间：</span>
                <span class="value">{{ formatDate(orderData.confirmTime) || '-' }}</span>
              </div>
            </div>
          </div>

          <div class="status-section">
            <div class="status-badge" :class="getStatusClass(orderData.status)">
              {{ getStatusText(orderData.status) }}
            </div>
          </div>
        </div>

        <!-- 出库单模板 -->
        <div v-else class="order-template">
          <div class="template-header">
            <div class="company-info">
              <h1>出库单</h1>
              <div class="company-name">智视仓储管理系统</div>
              <div class="company-address">地址：XX市XX区XX路XX号</div>
            </div>
            <div class="order-info">
              <div class="info-row">
                <span class="label">单据编号：</span>
                <span class="value">{{ orderData.orderNo }}</span>
              </div>
              <div class="info-row">
                <span class="label">出库类型：</span>
                <span class="value">{{ getOutboundTypeText(orderData.type) }}</span>
              </div>
              <div class="info-row">
                <span class="label">出库日期：</span>
                <span class="value">{{ formatDate(orderData.createTime) }}</span>
              </div>
            </div>
          </div>

          <div class="supplier-section">
            <div class="section-title">客户信息</div>
            <div class="info-grid">
              <div class="info-item">
                <span class="label">客户名称：</span>
                <span class="value">{{ orderData.customerName || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">联系人：</span>
                <span class="value">{{ orderData.customerContact || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">联系电话：</span>
                <span class="value">{{ orderData.customerPhone || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">收货地址：</span>
                <span class="value">{{ orderData.deliveryAddress || '-' }}</span>
              </div>
            </div>
          </div>

          <div class="items-section">
            <div class="section-title">出库明细</div>
            <table class="items-table">
              <thead>
                <tr>
                  <th>序号</th>
                  <th>商品编码</th>
                  <th>商品名称</th>
                  <th>规格型号</th>
                  <th>批次号</th>
                  <th>单位</th>
                  <th>数量</th>
                  <th>库位</th>
                  <th>备注</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, index) in orderData.items" :key="index">
                  <td>{{ index + 1 }}</td>
                  <td>{{ item.goodsCode }}</td>
                  <td>{{ item.goodsName }}</td>
                  <td>{{ item.spec || '-' }}</td>
                  <td>{{ item.batchNo }}</td>
                  <td>{{ item.unit || '件' }}</td>
                  <td>{{ item.quantity }}</td>
                  <td>{{ item.locationCode }}</td>
                  <td>{{ item.itemRemark || '-' }}</td>
                </tr>
              </tbody>
              <tfoot>
                <tr>
                  <td colspan="6" class="total-label">合计</td>
                  <td class="total-value">{{ totalQuantity }}</td>
                  <td colspan="2"></td>
                </tr>
              </tfoot>
            </table>
          </div>

          <div class="footer-section">
            <div class="sign-row">
              <div class="sign-item">
                <span class="label">制单人：</span>
                <span class="value">{{ orderData.creator || '-' }}</span>
              </div>
              <div class="sign-item">
                <span class="label">审核人：</span>
                <span class="value">{{ orderData.auditor || '-' }}</span>
              </div>
              <div class="sign-item">
                <span class="label">出库人：</span>
                <span class="value">{{ orderData.operator || '-' }}</span>
              </div>
            </div>
            <div class="sign-row">
              <div class="sign-item">
                <span class="label">制单时间：</span>
                <span class="value">{{ formatDate(orderData.createTime) }}</span>
              </div>
              <div class="sign-item">
                <span class="label">审核时间：</span>
                <span class="value">{{ formatDate(orderData.auditTime) || '-' }}</span>
              </div>
              <div class="sign-item">
                <span class="label">出库时间：</span>
                <span class="value">{{ formatDate(orderData.confirmTime) || '-' }}</span>
              </div>
            </div>
          </div>

          <div class="status-section">
            <div class="status-badge" :class="getStatusClass(orderData.status)">
              {{ getStatusText(orderData.status) }}
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button size="small" @click="visible = false">关闭</el-button>
        <el-button size="small" type="primary" @click="handlePrint">打印单据</el-button>
        <el-button size="small" type="success" @click="handleExport">📄 导出PDF</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  orderType: { type: String, default: 'inbound' },
  orderData: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['close'])

const printRef = ref(null)

const printTitle = computed(() => {
  return props.orderType === 'inbound' ? '打印入库单' : '打印出库单'
})

const totalQuantity = computed(() => {
  if (!props.orderData.items || !Array.isArray(props.orderData.items)) return 0
  return props.orderData.items.reduce((sum, item) => sum + (item.quantity || 0), 0)
})

const getInboundTypeText = (type) => {
  const types = {
    'PURCHASE': '采购入库',
    'RETURN': '退货入库',
    'TRANSFER_IN': '调拨入库',
    'OTHER_IN': '其他入库'
  }
  return types[type] || type || '采购入库'
}

const getOutboundTypeText = (type) => {
  const types = {
    'SALE': '销售出库',
    'RETURN_OUT': '退货出库',
    'TRANSFER_OUT': '调拨出库',
    'OTHER_OUT': '其他出库'
  }
  return types[type] || type || '销售出库'
}

const getStatusText = (status) => {
  const statusMap = {
    'DRAFT': '草稿',
    'AUDITING': '审核中',
    'AUDITED': '已审核',
    'CONFIRMED': '已完成',
    'CANCELLED': '已取消'
  }
  return statusMap[status] || status || '未知'
}

const getStatusClass = (status) => {
  const classMap = {
    'DRAFT': 'status-draft',
    'AUDITING': 'status-auditing',
    'AUDITED': 'status-audited',
    'CONFIRMED': 'status-confirmed',
    'CANCELLED': 'status-cancelled'
  }
  return classMap[status] || 'status-default'
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const handlePrint = () => {
  if (!printRef.value) return
  const printContent = printRef.value.innerHTML
  const iframe = document.createElement('iframe')
  iframe.style.position = 'fixed'
  iframe.style.right = '0'
  iframe.style.bottom = '0'
  iframe.style.width = '0'
  iframe.style.height = '0'
  iframe.style.border = 'none'
  document.body.appendChild(iframe)

  const iframeDoc = iframe.contentWindow.document
  iframeDoc.open()
  iframeDoc.write(`
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="UTF-8">
      <title>打印单据</title>
      <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Microsoft YaHei', sans-serif; padding: 20px; }
        @media print { body { padding: 0; } }
      </style>
    </head>
    <body>${printContent}</body>
    </html>
  `)
  iframeDoc.close()

  iframe.contentWindow.focus()
  iframe.contentWindow.print()

  // 打印完成后清理 iframe
  setTimeout(() => {
    document.body.removeChild(iframe)
  }, 1000)
}

const handleExport = () => {
  handlePrint()
}

watch(() => props.visible, (val) => {
  if (val && printRef.value) {
    printRef.value.classList.add('print-mode')
  } else if (printRef.value) {
    printRef.value.classList.remove('print-mode')
  }
})
</script>

<style scoped>
.order-print-modal :deep(.el-dialog) {
  max-width: 900px;
}

.print-container {
  padding: 20px;
  background: #fff;
}

.print-mode {
  background: #fff !important;
}

.order-template {
  font-family: 'SimSun', '宋体', serif;
  color: #333;
}

.template-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  border-bottom: 2px solid #333;
  padding-bottom: 15px;
  margin-bottom: 20px;
}

.company-info h1 {
  font-size: 28px;
  margin: 0 0 10px 0;
  color: #333;
}

.company-name {
  font-size: 14px;
  margin-bottom: 4px;
}

.company-address {
  font-size: 12px;
  color: #666;
}

.order-info {
  text-align: right;
}

.info-row {
  margin-bottom: 6px;
  font-size: 13px;
}

.info-row .label {
  color: #666;
}

.info-row .value {
  font-weight: bold;
}

.section-title {
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 12px;
  padding-bottom: 6px;
  border-bottom: 1px solid #ddd;
  color: #333;
}

.supplier-section, .items-section {
  margin-bottom: 20px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.info-item {
  font-size: 13px;
}

.info-item .label {
  color: #666;
  margin-right: 8px;
}

.info-item .value {
  color: #333;
}

.items-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.items-table th,
.items-table td {
  border: 1px solid #ddd;
  padding: 8px;
  text-align: center;
}

.items-table th {
  background: #f5f5f5;
  font-weight: bold;
}

.items-table tfoot .total-label {
  text-align: right;
  font-weight: bold;
}

.items-table tfoot .total-value {
  font-weight: bold;
  font-size: 14px;
  color: #333;
}

.footer-section {
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px dashed #ddd;
}

.sign-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 13px;
}

.sign-item {
  flex: 1;
  text-align: center;
}

.sign-item .label {
  color: #666;
}

.sign-item .value {
  display: inline-block;
  min-width: 80px;
  border-bottom: 1px solid #ddd;
}

.status-section {
  margin-top: 20px;
  text-align: right;
}

.status-badge {
  display: inline-block;
  padding: 6px 16px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: bold;
}

.status-draft { background: #f0f0f0; color: #666; }
.status-auditing { background: #fff3cd; color: #856404; }
.status-audited { background: #cce5ff; color: #004085; }
.status-confirmed { background: #d4edda; color: #155724; }
.status-cancelled { background: #f8d7da; color: #721c24; }

@media print {
  body * {
    visibility: hidden;
  }
  .print-container,
  .print-container * {
    visibility: visible;
  }
  .print-container {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
  }
}
</style>
