<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>出库单</title>
    <style>
        body { font-family: Arial, sans-serif; font-size: 12px; }
        .header { text-align: center; margin-bottom: 20px; }
        .title { font-size: 18px; font-weight: bold; }
        .info { margin: 10px 0; }
        .table { width: 100%; border-collapse: collapse; margin-top: 10px; }
        .table th, .table td { border: 1px solid #000; padding: 5px; text-align: center; }
        .table th { background-color: #f0f0f0; }
        .footer { margin-top: 20px; text-align: right; }
        .barcode { margin-top: 10px; }
    </style>
</head>
<body>
    <div class="header">
        <div class="title">出库单</div>
        <div class="info">订单编号: ${orderNo}</div>
        <div class="info">出库时间: ${createTime}</div>
        <div class="info">操作员: ${operatorName}</div>
    </div>
    
    <table class="table">
        <thead>
            <tr>
                <th>商品编号</th>
                <th>商品名称</th>
                <th>规格</th>
                <th>数量</th>
                <th>单位</th>
                <th>批次号</th>
            </tr>
        </thead>
        <tbody>
            <#list items as item>
            <tr>
                <td>${item.sku}</td>
                <td>${item.name}</td>
                <td>${item.spec}</td>
                <td>${item.quantity}</td>
                <td>${item.unit}</td>
                <td>${item.batchNo}</td>
            </tr>
            </#list>
        </tbody>
    </table>
    
    <div class="footer">
        <div>合计: ${totalCount} 件</div>
        <div class="barcode">条形码: ${barcode}</div>
    </div>
</body>
</html>