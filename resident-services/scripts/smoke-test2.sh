#!/bin/bash
# 根目录执行：bash scripts/smoke-test.sh
# 只使用宿主机映射端口，不打印任何密码/Token
set -euo pipefail

NACOS_CONSOLE_URL="${NACOS_CONSOLE_URL:-http://localhost:18081}"
RESIDENT_URL="${RESIDENT_URL:-http://localhost:8091}"
REPAIR_URL="${REPAIR_URL:-http://localhost:8092}"
NOTICE_URL="${NOTICE_URL:-http://localhost:8093}"

fail() {
  echo "[FAIL] $*" >&2
  exit 1
}

http_status() {
  curl -sS -o /dev/null -w '%{http_code}' --max-time 5 "$1" || fail "无法访问 $1"
}

expect_status() {
  local desc="$1"
  local expected="$2"
  local actual="$3"
  [ "$actual" = "$expected" ] || fail "$desc：期望 HTTP $expected，实际 $actual"
  echo "[PASS] $desc -> HTTP $actual"
}

echo "===== 1. Nacos 控制台可访问（宿主机 18081） ====="
console_code="$(http_status "${NACOS_CONSOLE_URL}/nacos")"
case "$console_code" in
  2*|3*) echo "[PASS] Nacos 控制台 ${NACOS_CONSOLE_URL}/nacos -> HTTP ${console_code}" ;;
  *) fail "Nacos 控制台 ${NACOS_CONSOLE_URL}/nacos 返回 HTTP ${console_code}" ;;
esac

echo
echo "===== 2. 三个业务服务 Actuator 健康检查 ====="
for entry in "resident-service:${RESIDENT_URL}" "repair-service:${REPAIR_URL}" "notice-service:${NOTICE_URL}"; do
  name="${entry%%:*}"
  url="${entry#*:}"
  body="$(curl -sS --max-time 5 "${url}/actuator/health")" || fail "${name} health 无法访问"
  echo "${body}" | grep -q '"status":"UP"' || fail "${name} health 非 UP：${body}"
  echo "[PASS] ${name} health -> ${body}"
done

echo
echo "===== 3. 新增住户（跨服务流程的前置数据） ====="
phone="139$(date +%s | tail -c 9)"
resident_json="$(curl -sS --max-time 5 -X POST -H 'Content-Type: application/json' -d "{
  \"fullName\":\"冒烟住户\",
  \"phone\":\"${phone}\",
  \"building\":\"1号楼\",
  \"unitNumber\":\"301\"
}" "${RESIDENT_URL}/api/residents")" || fail "新增住户请求失败"
resident_id="$(printf '%s' "${resident_json}" | grep -o '"id":[0-9]*' | head -n 1 | cut -d: -f2)"
[ -n "${resident_id}" ] || fail "未能从响应解析住户 id：${resident_json}"
echo "[PASS] 新增住户 id=${resident_id}"

echo
echo "===== 4. 跨服务调用：报修服务经 Nacos 发现业主服务并写入工单 ====="
code="$(curl -sS -o /tmp/smoke-repair.json -w '%{http_code}' --max-time 8 -X POST -H 'Content-Type: application/json' -d "{
  \"residentId\":${resident_id},
  \"title\":\"墙面渗水\",
  \"description\":\"卫生间墙面渗水，需要检修\",
  \"location\":\"1号楼301\"
}" "${REPAIR_URL}/api/repair-tickets")" || fail "跨服务报修请求失败"
expect_status "合法住户创建报修" "201" "${code}"

echo
echo "===== 5. 依赖资源不存在：保留 404（不被服务发现改写成 503） ====="
code="$(curl -sS -o /tmp/smoke-repair-404.json -w '%{http_code}' --max-time 8 -X POST -H 'Content-Type: application/json' -d '{
  "residentId":999,
  "title":"测试报修",
  "description":"非法住户测试",
  "location":"测试地址"
}' "${REPAIR_URL}/api/repair-tickets")" || fail "不存在的住户请求失败"
expect_status "业主不存在" "404" "${code}"

echo
echo "===== 6. 公告服务业务接口 ====="
code="$(curl -sS -o /tmp/smoke-notice.json -w '%{http_code}' --max-time 5 -X POST -H 'Content-Type: application/json' -d '{
  "title":"社区停水通知",
  "content":"本周六上午 9 点至 12 点停水检修",
  "publisher":"物业服务中心"
}' "${NOTICE_URL}/api/notices")" || fail "新增公告请求失败"
expect_status "新增公告" "201" "${code}"
code="$(http_status "${NOTICE_URL}/api/notices")"
expect_status "公告列表查询" "200" "${code}"

echo
echo "[PASS] 冒烟测试全部通过"
