#!/bin/bash
# 根目录执行：bash scripts/smoke-test.sh
# resident-service 不映射宿主机端口（多实例不抢端口），因此其健康检查走容器健康状态，
# 前置住户数据直接写入 resident_db；跨服务调用仍经 repair-service 的宿主机端口验证。
set -euo pipefail

NACOS_CONSOLE_URL="${NACOS_CONSOLE_URL:-http://localhost:18081}"
REPAIR_URL="${REPAIR_URL:-http://localhost:8092}"
NOTICE_URL="${NOTICE_URL:-http://localhost:8093}"
RESIDENT_PREFIX="${RESIDENT_PREFIX:-resident-services-resident-service}"

fail() {
  echo "[FAIL] $*" >&2
  exit 1
}

http_status() {
  curl -sS -o /dev/null -w '%{http_code}' --max-time 5 "$1" || fail "无法访问 $1"
}

expect_status() {
  local desc="$1" expected="$2" actual="$3"
  [ "$actual" = "$expected" ] || fail "$desc：期望 HTTP $expected，实际 $actual"
  echo "[PASS] $desc -> HTTP $actual"
}

echo "===== 1. Nacos 控制台可访问（宿主机 18081，不使用旧版 8848/nacos） ====="
console_code="$(http_status "${NACOS_CONSOLE_URL}/")"
case "$console_code" in
  2*|3*) echo "[PASS] Nacos 控制台 ${NACOS_CONSOLE_URL} -> HTTP ${console_code}" ;;
  *) fail "Nacos 控制台 ${NACOS_CONSOLE_URL} 返回 HTTP ${console_code}" ;;
esac

echo
echo "===== 2. 三个业务服务健康检查 ====="
# repair-service / notice-service 通过宿主机端口检查 Actuator
for entry in "repair-service:${REPAIR_URL}" "notice-service:${NOTICE_URL}"; do
  name="${entry%%:*}"
  url="${entry#*:}"
  body="$(curl -sS --max-time 5 "${url}/actuator/health")" || fail "${name} health 无法访问"
  echo "${body}" | grep -q '"status":"UP"' || fail "${name} health 非 UP：${body}"
  echo "[PASS] ${name} health -> ${body}"
done

# resident-service 不暴露宿主机端口，改为检查容器健康状态（覆盖全部副本）
resident_healthy=0
while IFS= read -r cname; do
  [ -n "${cname}" ] || continue
  status="$(docker inspect --format '{{.State.Health.Status}}' "${cname}" 2>/dev/null || echo unknown)"
  [ "${status}" = "healthy" ] || fail "${cname} 容器健康状态为 ${status}"
  echo "[PASS] ${cname} 容器健康 -> ${status}"
  resident_healthy=$((resident_healthy + 1))
done < <(docker ps --filter "name=${RESIDENT_PREFIX}" --format '{{.Names}}')
[ "${resident_healthy}" -ge 1 ] || fail "未找到运行中的 resident-service 容器"

echo
echo "===== 3. 准备住户数据（写入 resident_db，作为跨服务流程前置数据） ====="
resident_id="$(docker compose exec -T resident-db sh -c \
  'mysql -N -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" 2>/dev/null -e "
     INSERT INTO residents (full_name, phone, building, unit_number, created_at)
     VALUES (\"冒烟住户\", CONCAT(\"139\", LPAD(UNIX_TIMESTAMP(), 8, \"0\")), \"1号楼\", \"301\", NOW(3));
     SELECT LAST_INSERT_ID();"' | tail -n 1 | tr -d '\r')" || fail "写入住户数据失败"
[ -n "${resident_id}" ] || fail "未能获取住户 id"
echo "[PASS] 住户数据已就绪 id=${resident_id}"

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
