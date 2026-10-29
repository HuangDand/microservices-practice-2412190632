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
