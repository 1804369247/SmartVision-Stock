import json, urllib.request, urllib.error

BASE = "http://localhost:8080"

def login():
    req = urllib.request.Request(BASE + "/api/auth/login",
        data=json.dumps({"username":"admin","password":"admin123"}).encode(),
        headers={"Content-Type":"application/json"}, method="POST")
    with urllib.request.urlopen(req) as r:
        d = json.load(r)
    return d["data"]["token"]

def get(token, path):
    url = BASE + path
    req = urllib.request.Request(url, headers={"Authorization":"Bearer "+token})
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            body = r.read().decode()
            code = r.status
    except urllib.error.HTTPError as e:
        body = e.read().decode()
        code = e.code
    try:
        d = json.loads(body)
    except Exception:
        return code, "NON_JSON", body[:80]
    data = d.get("data")
    if isinstance(data, dict) and ("content" in data or "totalElements" in data):
        kind = "PAGE(total=%s,size=%s,items=%s)" % (data.get("totalElements"), data.get("size"), len(data.get("content") or []))
    elif isinstance(data, list):
        kind = "LIST(len=%d)" % len(data)
    elif isinstance(data, dict):
        kind = "OBJ(keys=%s)" % list(data.keys())[:5]
    elif data is None:
        kind = "NULL(api code=%s)" % d.get("code")
    else:
        kind = type(data).__name__
    return code, kind, "code=%s msg=%s" % (d.get("code"), str(d.get("message"))[:60])

token = login()
print("token len:", len(token))
print("%-45s %-4s %s" % ("ENDPOINT", "HTTP", "STRUCTURE"))

endpoints = [
    "/api/basic/suppliers?page=0&size=3",
    "/api/basic/goods?page=0&size=3",
    "/api/locations?page=0&size=3",
    "/api/instances?page=0&size=3",
    "/api/order/inbound/list?page=0&size=3",
    "/api/order/outbound/list?page=0&size=3",
    "/api/return/list?page=0&size=3",
    "/api/warehouses?page=0&size=3",
    "/api/stock-count/list?page=0&size=3",
    "/api/reports/inventory?page=0&size=3",
    "/api/reports/inout-records?page=0&size=3",
    "/api/reports/utilization?page=0&size=3",
    "/api/reports/alerts?page=0&size=3",
    "/api/reports/kpi?page=0&size=3",
    "/api/pick/waves/active?page=0&size=3",
    "/api/notification/list?userId=1&page=0&size=3",
    "/api/system/users?page=0&size=3",
    "/api/system/roles?page=0&size=3",
    "/api/system/logs?page=0&size=3",
    "/api/expiry/alerts/active?page=0&size=3",
    "/api/expiry/statistics?page=0&size=3",
    "/api/expiry/expired?page=0&size=3",
    "/api/batch/expiring?page=0&size=3",
    "/api/batch/statistics?page=0&size=3",
    "/api/dictionary/all",
    "/api/logs/operation?page=0&size=3",
    "/api/logistics/companies",
    "/api/print/printers",
    "/api/inout/records?page=0&size=3",
    "/api/reservation/available?goodsId=1&warehouseId=1&quantity=1",
    "/api/vision",
    "/api/models",
    "/api/location/empty",
    "/api/warehouses/statistics",
    "/api/basic/warehouses?page=0&size=3",
]

for ep in endpoints:
    try:
        code, kind, info = get(token, ep)
    except Exception as e:
        code, kind, info = "ERR", "EXC", str(e)[:60]
    print("%-45s %-4s %s | %s" % (ep.split("?")[0], code, kind, info))
