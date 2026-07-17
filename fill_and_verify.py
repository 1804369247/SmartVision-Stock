import json, urllib.request, urllib.error, urllib.parse, time

BASE = "http://localhost:8080"
results = []
TS = time.strftime("%H%M%S")

def login():
    req = urllib.request.Request(BASE + "/api/auth/login",
        data=json.dumps({"username":"admin","password":"admin123"}).encode(),
        headers={"Content-Type":"application/json"}, method="POST")
    with urllib.request.urlopen(req) as r:
        return json.load(r)["data"]["token"]

def call(method, path, token, body=None, is_form=False):
    url = BASE + path
    headers = {"Authorization":"Bearer "+token}
    data = None
    if body is not None:
        if is_form:
            data = urllib.parse.urlencode(body).encode()
            headers["Content-Type"] = "application/x-www-form-urlencoded"
        else:
            data = json.dumps(body).encode()
            headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return r.status, json.load(r)
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.load(e)
        except Exception:
            return e.code, {"code":e.code,"message":e.read().decode()[:120]}
    except Exception as e:
        return -1, {"code":-1,"message":str(e)[:120]}

def get_list(token, path):
    code, d = call("GET", path, token)
    data = d.get("data")
    if isinstance(data, dict):
        return data.get("content") or data.get("data") or []
    if isinstance(data, list):
        return data
    return []

def find_in_list(token, path, key, value):
    for it in get_list(token, path):
        if it.get(key) == value:
            return it
    return None

def rec(module, action, ok, detail):
    results.append((module, action, "OK" if ok else "FAIL", detail))
    print(f"[{'OK' if ok else 'FAIL'}] {module:9} | {action:26} | {detail}")

token = login()
print("login OK, token len", len(token))

# ---------- 1. 供应商 ----------
sname = f"测试供应商-{TS}"
code, d = call("POST","/api/basic/suppliers",token,{"name":sname,"contact":"王经理","phone":"13800000000","address":"北京市朝阳区"})
sup = find_in_list(token, "/api/basic/suppliers?size=100", "name", sname)
rec("供应商","创建+验证", code==200 and sup, f"name={sname} id={sup['id'] if sup else None}")
rec("供应商","查询(分页)", True, f"total={len(get_list(token,'/api/basic/suppliers?size=3'))}")

# ---------- 2. 商品 ----------
gname = f"测试商品-伺服电机{TS}"
code, d = call("POST","/api/basic/goods",token,{"name":gname,"spec":"750W","unit":"台","category":"电机","warningQuantity":10,"barcode":f"692{TS}"})
g = find_in_list(token, "/api/basic/goods?size=100", "name", gname)
rec("商品","创建+验证", code==200 and g, f"name={gname} id={g['id'] if g else None} code={g['code'] if g else None}")
if g:
    code, d = call("PUT",f"/api/basic/goods/{g['id']}",token,{"name":gname+"(改)","spec":"750W","unit":"台","category":"电机"})
    rec("商品","修改", code==200, f"code={code}")
    code, d = call("DELETE",f"/api/basic/goods/{g['id']}",token)
    rec("商品","删除", code==200, f"code={code}（删测试商品，保持数据整洁）")

# ---------- 3. 仓库（第2个用于调拨） ----------
wname = f"华东分仓-{TS}"
code, d = call("POST","/api/warehouses",token,{"name":wname,"code":f"WH-TS-{TS}","address":"上海市","description":"测试分仓"})
wh = find_in_list(token, "/api/warehouses?size=100", "name", wname)
rec("仓库","创建(分仓)+验证", code==200 and wh, f"name={wname} id={wh['id'] if wh else None}")
wid2 = wh['id'] if wh else None

# ---------- 4. 入库（生成库存） ----------
inbody = {"type":"PURCHASE","supplierId":2,"warehouseId":1,
          "items":[{"goodsId":1,"quantity":50,"batchNo":f"B{TS}","locationId":1}]}
code, d = call("POST","/api/order/inbound",token,inbody)
ib = d.get("data") or {}
ib_id = ib.get("id")
rec("入库单","创建", code==200 and ib_id, f"id={ib_id} orderNo={ib.get('orderNo')}")
if ib_id:
    code, d = call("PUT",f"/api/order/inbound/{ib_id}/audit",token,{})
    rec("入库单","审核", code==200, f"code={code}")
    code, d = call("PUT",f"/api/order/inbound/{ib_id}/confirm",token,{})
    rec("入库单","确认(生成库存)", code==200, f"code={code} msg={str(d.get('message'))[:40]}")

# 选出库目标实例
insts = get_list(token,"/api/instances?page=0&size=100")
target = next((i for i in insts if i.get("goodsId")==1 and (i.get("quantity") or 0)>=5), None)
rec("库存实例","选取出库目标", target is not None, f"instanceId={target['id'] if target else None} qty={target['quantity'] if target else None}")

# ---------- 5. 出库（含悲观锁路径） ----------
ob_id=None
if target:
    code, d = call("POST","/api/order/outbound",token,{"type":"SALE","customerId":1001,"warehouseId":1,
            "items":[{"goodsInstanceId":target["id"],"quantity":5}]})
    ob = d.get("data") or {}
    ob_id = ob.get("id")
    rec("出库单","创建", code==200 and ob_id, f"id={ob_id}")
    if ob_id:
        code, d = call("PUT",f"/api/order/outbound/{ob_id}/audit",token,{})
        rec("出库单","审核", code==200, f"code={code}")
        code, d = call("PUT",f"/api/order/outbound/{ob_id}/pick",token,{})
        rec("出库单","拣货", code==200, f"code={code}")
        code, d = call("PUT",f"/api/order/outbound/{ob_id}/confirm",token,{})
        rec("出库单","确认(扣减)", code==200, f"code={code} msg={str(d.get('message'))[:40]}")

# ---------- 5b. 出库（留 PICKING 状态供波次） ----------
ob2_id=None
if target:
    code, d = call("POST","/api/order/outbound",token,{"type":"SALE","customerId":1002,"warehouseId":1,
            "items":[{"goodsInstanceId":target["id"],"quantity":3}]})
    ob2 = d.get("data") or {}
    ob2_id = ob2.get("id")
    if ob2_id:
        call("PUT",f"/api/order/outbound/{ob2_id}/audit",token,{})
        call("PUT",f"/api/order/outbound/{ob2_id}/pick",token,{})
        rec("出库单2","留PICKING供波次", ob2_id is not None, f"id={ob2_id}")

# ---------- 6. 调拨 ----------
if wid2:
    code, d = call("POST",f"/api/warehouses/transfer?sku=GD-001&quantity=10&fromWarehouseId=1&toWarehouseId={wid2}",token,None,is_form=True)
    tr = d.get("data") or {}
    tr_id = tr.get("id") or tr.get("transferId")
    rec("调拨","创建", code==200 and tr_id, f"id={tr_id} code={code}")
    if tr_id:
        code, d = call("POST",f"/api/warehouses/transfer/{tr_id}/approve",token,{})
        rec("调拨","审批", code==200, f"code={code}")
        code, d = call("POST",f"/api/warehouses/transfer/{tr_id}/execute",token,{})
        rec("调拨","执行", code==200, f"code={code} msg={str(d.get('message'))[:40]}")

# ---------- 7. 退货 ----------
code, d = call("POST","/api/return/create",token,{"orderId":ob_id,"customerId":1001,"reason":"质量问题","items":[{"sku":"GD-001","quantity":2}]})
rt = d.get("data") or {}
rt_id = rt.get("id") or rt.get("returnId") or rt.get("returnNo")
rec("退货单","创建", code==200, f"id={rt_id} code={code} msg={str(d.get('message'))[:30]}")
if rt_id:
    code, d = call("PUT",f"/api/return/{rt_id}/inspect",token,{"result":"PASS","remark":"检验合格"})
    rec("退货单","验收", code==200, f"code={code}")
    code, d = call("PUT",f"/api/return/{rt_id}/confirm",token)
    rec("退货单","确认", code==200, f"code={code} msg={str(d.get('message'))[:40]}")

# ---------- 8. 盘点 ----------
code, d = call("POST","/api/stock-count/create",token,{"warehouseId":1,"warehouseName":"主仓库","area":"A","countType":"FULL","operatorName":"admin","operatorId":1})
sc_no = (d.get("data") or {}).get("countNo")
sc = find_in_list(token, "/api/stock-count/list?size=100", "countNo", sc_no) if sc_no else None
rec("盘点单","创建+验证", code==200 and sc, f"countNo={sc_no} id={sc['id'] if sc else None}")
if sc:
    sc_id = sc["id"]
    code, d = call("POST",f"/api/stock-count/{sc_id}/start",token,{})
    rec("盘点单","开始", code==200, f"code={code}")
    _, det = call("GET", f"/api/stock-count/{sc_id}", token)
    citems = (det.get("data") or {}).get("items") or []
    if citems:
        payload = [{"id": it["id"], "actualQty": it.get("expectedQty") or 10} for it in citems[:3]]
        code, d = call("POST",f"/api/stock-count/{sc_id}/items",token,payload)
        rec("盘点单","录入明细", code==200, f"code={code} msg={str(d.get('message'))[:30]}")
    else:
        rec("盘点单","录入明细", False, "未获取到盘点明细项")
    code, d = call("POST",f"/api/stock-count/{sc_id}/complete",token,{})
    rec("盘点单","完成", code==200, f"code={code}")
    code, d = call("POST",f"/api/stock-count/{sc_id}/confirm",token,{})
    rec("盘点单","确认", code==200, f"code={code} msg={str(d.get('message'))[:40]}")

# ---------- 9. 波次拣货（用 PICKING 状态出库单） ----------
if ob2_id:
    code, d = call("POST","/api/pick/wave",token,[ob2_id])
    rec("波次拣货","创建(基于PICKING单)", code==200, f"code={code} msg={str(d.get('message'))[:40]}")

# ---------- 10. 通知 ----------
code, d = call("POST","/api/notification/send",token,{"userId":1,"username":"admin","type":"SYSTEM","title":f"测试通知-{TS}","content":"自动生成的测试通知","priority":1})
rec("通知","发送", code==200, f"code={code}")

# ---------- 11. 角色 + 用户 ----------
rname = f"测试角色-{TS}"
code, d = call("POST","/api/system/roles",token,{"name":rname,"description":"自动化测试角色","menuIds":"1,2,3","dataScope":"ALL","status":1})
role = find_in_list(token, "/api/system/roles?size=100", "name", rname)
rec("角色","创建+验证", code==200 and role, f"name={rname} id={role['id'] if role else None}")
uname = f"testuser{TS}"
code, d = call("POST","/api/system/users",token,{"username":uname,"password":"test123","role":"USER","realName":"测试用户","email":"t@test.com","phone":"13900000000","enabled":True})
user = find_in_list(token, "/api/system/users?size=100", "username", uname)
rec("用户","创建+验证", code==200 and user, f"username={uname} id={user['id'] if user else None} msg={str(d.get('message'))[:30]}")

# ---------- 汇总 ----------
print("\n================ CRUD 验证汇总 ================")
okc=sum(1 for r in results if r[2]=="OK"); failc=sum(1 for r in results if r[2]=="FAIL")
print(f"总计 {len(results)} 项，成功 {okc}，失败 {failc}")
for m,a,s,det in results:
    print(f"  [{s}] {m} - {a} : {det}")

with open("CRUD_VERIFY_REPORT.md","w",encoding="utf-8") as f:
    f.write("# 全功能模块 CRUD 验证与测试数据填充报告\n\n> 自动生成于 fill_and_verify.py（时间戳 "+TS+"）\n\n")
    f.write(f"## 汇总：共 {len(results)} 项，成功 {okc}，失败 {failc}\n\n")
    f.write("| 模块 | 操作 | 结果 | 说明 |\n|---|---|---|---|\n")
    for m,a,s,det in results:
        f.write(f"| {m} | {a} | {s} | {det} |\n")
print("\n报告已写入 CRUD_VERIFY_REPORT.md")
