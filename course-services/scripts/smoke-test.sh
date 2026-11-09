#!/usr/bin/env bash
set -euo pipefail

USER_URL="http://localhost:8081"
CATALOG_URL="http://localhost:8082"
ENROLL_URL="http://localhost:8083"

echo "===== 1. 查询预置学生 ====="
curl -s "$USER_URL/api/students" | head -c 500
echo

STUDENT_ID=$(curl -s "$USER_URL/api/students" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "学生 ID: $STUDENT_ID"

echo
echo "===== 2. 查询预置课程 ====="
curl -s "$CATALOG_URL/api/courses" | head -c 500
echo

COURSE_ID=$(curl -s "$CATALOG_URL/api/courses" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "课程 ID: $COURSE_ID"

echo
echo "===== 3. 发起选课 ====="
ENROLL_RESULT=$(curl -s -X POST "$ENROLL_URL/api/enrollments" \
  -H "Content-Type: application/json" \
  -d "{\"studentId\":\"$STUDENT_ID\",\"courseId\":\"$COURSE_ID\"}")
echo "$ENROLL_RESULT"

ENROLL_ID=$(echo "$ENROLL_RESULT" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "选课记录 ID: $ENROLL_ID"

echo
echo "===== 4. 查询学生选课记录 ====="
curl -s "$ENROLL_URL/api/enrollments/student/$STUDENT_ID"
echo

echo
echo "===== 5. 验证课程名额已 +1 ====="
curl -s "$CATALOG_URL/api/courses/$COURSE_ID" | grep -E 'enrolled|capacity'
echo

echo
echo "===== 6. 退课 ====="
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" -X DELETE "$ENROLL_URL/api/enrollments/$ENROLL_ID"

echo
echo "===== 7. 验证课程名额已 -1 ====="
curl -s "$CATALOG_URL/api/courses/$COURSE_ID" | grep -E 'enrolled|capacity'
echo

echo
echo "===== 全部验证完成 ====="
