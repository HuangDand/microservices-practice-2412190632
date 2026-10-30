$ResidentUrl = "http://localhost:8091"
$RepairUrl = "http://localhost:8092"

Write-Host "===== 1. 准备业主（没有则登记） ====="
$residents = Invoke-RestMethod -Uri "$ResidentUrl/api/residents"
if ($residents.Count -eq 0) {
    $body = @{ fullName = "张三"; phone = "13800000001"; building = "1栋"; unitNumber = "101" } | ConvertTo-Json
    $resident = Invoke-RestMethod -Uri "$ResidentUrl/api/residents" -Method Post -ContentType "application/json; charset=utf-8" -Body $body
} else {
    $resident = $residents[0]
}
$resident | ConvertTo-Json
$residentId = $resident.id
Write-Host "业主 ID: $residentId"

Write-Host ""
Write-Host "===== 2. 提交报修工单（repair-service 内部调用 resident-service 校验业主） ====="
$ticketBody = @{ title = "楼道灯不亮"; description = "三楼楼道灯损坏，需要更换"; location = "1栋3单元3楼"; residentId = $residentId } | ConvertTo-Json
$ticket = Invoke-RestMethod -Uri "$RepairUrl/api/repair-tickets" -Method Post -ContentType "application/json; charset=utf-8" -Body $ticketBody
$ticket | ConvertTo-Json
$ticketId = $ticket.id
Write-Host "工单 ID: $ticketId"

Write-Host ""
Write-Host "===== 3. 按业主查询工单 ====="
Invoke-RestMethod -Uri "$RepairUrl/api/repair-tickets?residentId=$residentId" | ConvertTo-Json -Depth 3

Write-Host ""
Write-Host "===== 4. 工单状态流转 OPEN -> IN_PROGRESS ====="
Invoke-RestMethod -Uri "$RepairUrl/api/repair-tickets/$ticketId/status" -Method Patch -ContentType "application/json; charset=utf-8" -Body (@{ status = "IN_PROGRESS" } | ConvertTo-Json) | ConvertTo-Json

Write-Host ""
Write-Host "===== 5. 工单状态流转 IN_PROGRESS -> RESOLVED ====="
Invoke-RestMethod -Uri "$RepairUrl/api/repair-tickets/$ticketId/status" -Method Patch -ContentType "application/json; charset=utf-8" -Body (@{ status = "RESOLVED" } | ConvertTo-Json) | ConvertTo-Json

Write-Host ""
Write-Host "===== 6. 验证非法流转被拒绝（RESOLVED -> OPEN 应报 400） ====="
try {
    Invoke-RestMethod -Uri "$RepairUrl/api/repair-tickets/$ticketId/status" -Method Patch -ContentType "application/json; charset=utf-8" -Body (@{ status = "OPEN" } | ConvertTo-Json)
    Write-Host "ERROR: 非法流转未被拦截"
} catch {
    Write-Host "已拦截非法流转: $($_.ErrorDetails.Message)"
}

Write-Host ""
Write-Host "===== 7. 验证不存在的业主被拒绝（应报 404） ====="
try {
    Invoke-RestMethod -Uri "$RepairUrl/api/repair-tickets" -Method Post -ContentType "application/json; charset=utf-8" -Body (@{ title = "测试"; description = "测试"; location = "测试"; residentId = 999999 } | ConvertTo-Json)
    Write-Host "ERROR: 不存在的业主未被拦截"
} catch {
    Write-Host "已拦截不存在的业主: $($_.ErrorDetails.Message)"
}

Write-Host ""
Write-Host "===== 全部验证完成 ====="
#!/bin/bash
set -euo pipefail

# 服务地址
RESIDENT_URL="http://localhost:8091"
REPAIR_URL="http://localhost:8092"

echo "===== 1. 健康检查：住户服务 health ====="
curl -s -w "\nHTTP_STATUS:%{http_code}\n" "${RESIDENT_URL}/actuator/health"
echo -e "\n===== 2. 健康检查：报修服务 health ====="
curl -s -w "\nHTTP_STATUS:%{http_code}\n" "${REPAIR_URL}/actuator/health"

echo -e "\n===== 3. 新增住户（业务前置数据） ====="
RESIDENT_RESP=$(curl -s -w "\nHTTP_STATUS:%{http_code}\n" -X POST -H "Content-Type:application/json" -d '{
"studentNumber":"2026001",
"name":"小明",
"fullName":"小明",
"major":"计算机科学",
"phone":"13800138000",
"building":"1号楼",
"unitNumber":"301"
}' "${RESIDENT_URL}/api/residents")
echo "$RESIDENT_RESP"

echo -e "\n===== 4. 查询住户列表，确认住户创建成功 ====="
curl -s -w "\nHTTP_STATUS:%{http_code}\n" "${RESIDENT_URL}/api/residents"

echo -e "\n===== 5. 跨服务业务流程：合法住户创建报修（成功场景） ====="
curl -s -w "\nHTTP_STATUS:%{http_code}\n" -X POST -H "Content-Type:application/json" -d '{
"residentId":1,
"title":"墙面渗水",
"description":"卫生间墙面渗水，需要检修",
"location":"1号楼301"
}' "${REPAIR_URL}/api/repair-tickets"

echo -e "\n===== 6. 失败场景：依赖资源不存在（residentId=999，住户不存在） ====="
curl -s -w "\nHTTP_STATUS:%{http_code}\n" -X POST -H "Content-Type:application/json" -d '{
"residentId":999,
"title":"测试报修",
"description":"非法住户测试",
"location":"测试地址"
}' "${REPAIR_URL}/api/repair-tickets"

echo -e "\n===== 7. 失败场景：依赖服务不可用模拟（访问错误端口，模拟住户服务宕机） ====="
curl -s -w "\nHTTP_STATUS:%{http_code}\n" --max-time 3 "http://localhost:9999/actuator/health" || echo "【预期失败】住户服务不可访问，超时/连接拒绝"

echo -e "\n✅ 冒烟测试脚本执行完毕"
