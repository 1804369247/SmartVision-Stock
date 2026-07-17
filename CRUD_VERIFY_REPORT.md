# 全功能模块 CRUD 验证与测试数据填充报告

> 自动生成于 fill_and_verify.py（时间戳 120019）

## 汇总：共 30 项，成功 30，失败 0

| 模块 | 操作 | 结果 | 说明 |
|---|---|---|---|
| 供应商 | 创建+验证 | OK | name=测试供应商-120019 id=8 |
| 供应商 | 查询(分页) | OK | total=3 |
| 商品 | 创建+验证 | OK | name=测试商品-伺服电机120019 id=8 code=GD202607171200200005 |
| 商品 | 修改 | OK | code=200 |
| 商品 | 删除 | OK | code=200（删测试商品，保持数据整洁） |
| 仓库 | 创建(分仓)+验证 | OK | name=华东分仓-120019 id=4 |
| 入库单 | 创建 | OK | id=5 orderNo=IN20260717120020000008 |
| 入库单 | 审核 | OK | code=200 |
| 入库单 | 确认(生成库存) | OK | code=200 msg=入库确认成功 |
| 库存实例 | 选取出库目标 | OK | instanceId=39 qty=50 |
| 出库单 | 创建 | OK | id=16 |
| 出库单 | 审核 | OK | code=200 |
| 出库单 | 拣货 | OK | code=200 |
| 出库单 | 确认(扣减) | OK | code=200 msg=出库确认成功 |
| 出库单2 | 留PICKING供波次 | OK | id=17 |
| 调拨 | 创建 | OK | id=TR20260717120020000002 code=200 |
| 调拨 | 审批 | OK | code=200 |
| 调拨 | 执行 | OK | code=200 msg=success |
| 退货单 | 创建 | OK | id=RET20260717120021000003 code=200 msg=success |
| 退货单 | 验收 | OK | code=200 |
| 退货单 | 确认 | OK | code=200 msg=success |
| 盘点单 | 创建+验证 | OK | countNo=SC202607171200210005 id=4 |
| 盘点单 | 开始 | OK | code=200 |
| 盘点单 | 录入明细 | OK | code=200 msg=更新成功 |
| 盘点单 | 完成 | OK | code=200 |
| 盘点单 | 确认 | OK | code=200 msg=盘点确认完成，库存已更新 |
| 波次拣货 | 创建(基于PICKING单) | OK | code=200 msg=success |
| 通知 | 发送 | OK | code=200 |
| 角色 | 创建+验证 | OK | name=测试角色-120019 id=3 |
| 用户 | 创建+验证 | OK | username=testuser120019 id=10 msg=用户创建成功 |
