$UserUrl = "http://localhost:8081"
$CatalogUrl = "http://localhost:8082"
$EnrollUrl = "http://localhost:8083"

Write-Host "===== 1. 查询预置学生 ====="
$students = Invoke-RestMethod -Uri "$UserUrl/api/students"
$students | ConvertTo-Json -Depth 3
$studentId = $students[0].id
Write-Host "学生 ID: $studentId"

Write-Host ""
Write-Host "===== 2. 查询预置课程 ====="
$courses = Invoke-RestMethod -Uri "$CatalogUrl/api/courses"
$courses | ConvertTo-Json -Depth 3
$courseId = $courses[0].id
Write-Host "课程 ID: $courseId"

Write-Host ""
Write-Host "===== 3. 发起选课 ====="
$body = @{ studentId = $studentId; courseId = $courseId } | ConvertTo-Json
$enrollment = Invoke-RestMethod -Uri "$EnrollUrl/api/enrollments" -Method Post -ContentType "application/json" -Body $body
$enrollment | ConvertTo-Json
$enrollId = $enrollment.id
Write-Host "选课记录 ID: $enrollId"

Write-Host ""
Write-Host "===== 4. 查询学生选课记录 ====="
Invoke-RestMethod -Uri "$EnrollUrl/api/enrollments/student/$studentId" | ConvertTo-Json -Depth 3

Write-Host ""
Write-Host "===== 5. 验证课程名额已 +1 ====="
Invoke-RestMethod -Uri "$CatalogUrl/api/courses/$courseId" | Select-Object code, title, enrolled, capacity | ConvertTo-Json

Write-Host ""
Write-Host "===== 6. 退课 ====="
Invoke-RestMethod -Uri "$EnrollUrl/api/enrollments/$enrollId" -Method Delete
Write-Host "退课成功 (HTTP 204)"

Write-Host ""
Write-Host "===== 7. 验证课程名额已 -1 ====="
Invoke-RestMethod -Uri "$CatalogUrl/api/courses/$courseId" | Select-Object code, title, enrolled, capacity | ConvertTo-Json

Write-Host ""
Write-Host "===== 全部验证完成 ====="
