#!/usr/bin/env bash
# week-08 端到端验证：OpenFeign + LoadBalancer 多实例 + 跨服务调用
# 必须从仓库根目录执行：bash scripts/e2e-test.sh
# resident-service 不映射宿主机端口，只通过 Nacos 服务名被 repair-service 调用。
# 任一检查失败退出非零。

set -uo pipefail

PASS_COUNT=0
FAIL_COUNT=0

NACOS_CONSOLE_URL="${NACOS_CONSOLE_URL:-http://localhost:18081}"
REPAIR_URL="${REPAIR_URL:-http://localhost:8092}"
NOTICE_URL="${NOTICE_URL:-http://localhost:8093}"
RESIDENT_PREFIX="${RESIDENT_PREFIX:-resident-services-resident-service}"
REPAIR_CONTAINER="${REPAIR_CONTAINER:-resident-services-repair-service-1}"

pass() { echo "[PASS] $1"; PASS_COUNT=$((PASS_COUNT + 1)); }
fail() { echo "[FAIL] $1"; FAIL_COUNT=$((FAIL_COUNT + 1)); }

# 等待某个宿主机端口的 Actuator 健康
wait_http_healthy() {
  local name="$1" url="$2" deadline=$((SECONDS + 90))
  while [ ${SECONDS} -lt ${deadline} ]; do
    if curl -sf --max-time 5 "${url}/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
      pass "${name} 健康检查 UP"
      return 0
    fi
    sleep 3
  done
  fail "${name} 健康检查超时"
  return 1
}

# 等待 resident-service 所有容器副本 healthy，并返回副本数
wait_resident_healthy() {
  local expected="$1" deadline=$((SECONDS + 120)) count
  while [ ${SECONDS} -lt ${deadline} ]; do
    count=0
    local ok=1
    while IFS= read -r cname; do
      [ -n "${cname}" ] || continue
      count=$((count + 1))
      status="$(docker inspect --format '{{.State.Health.Status}}' "${cname}" 2>/dev/null || echo unknown)"
      [ "${status}" = "healthy" ] || ok=0
    done < <(docker ps --filter "name=${RESIDENT_PREFIX}" --format '{{.Names}}')
    if [ "${count}" -ge "${expected}" ] && [ "${ok}" -eq 1 ]; then
      echo "${count}"
      return 0
    fi
    sleep 3
  done
  echo "${count:-0}"
  return 1
}

echo "============================================="
echo "  Week-08 E2E：OpenFeign + LoadBalancer"
echo "============================================="

# ---------- 1) 基础可用性 ----------
echo
echo "--- 1/6 基础健康 ---"
code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 "${NACOS_CONSOLE_URL}/" || echo 000)"
case "${code}" in
  2*|3*) pass "Nacos 控制台可访问 (HTTP ${code})" ;;
  *) fail "Nacos 控制台不可访问 (HTTP ${code})" ;;
esac
wait_http_healthy "repair-service" "${REPAIR_URL}"
wait_http_healthy "notice-service" "${NOTICE_URL}"
resident_count="$(wait_resident_healthy 1)" && pass "resident-service 容器健康（副本 ${resident_count}）" \
                                            || fail "resident-service 容器未达健康状态"

# ---------- 2) Nacos 注册 ----------
echo
echo "--- 2/6 Nacos 服务注册 ---"
if [ -z "${NACOS_PASSWORD:-}" ]; then
  fail "未设置 NACOS_PASSWORD（请先 set 或 source .env）"
  NACOS_TOKEN=""
else
  NACOS_TOKEN="$(curl -sf --max-time 10 -X POST "http://localhost:18848/nacos/v1/auth/login" \
      -d "username=${NACOS_USERNAME:-nacos}&password=${NACOS_PASSWORD}" \
      | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')"
  [ -n "${NACOS_TOKEN}" ] && pass "Nacos 登录成功" || fail "Nacos 登录失败"
fi

if [ -n "${NACOS_TOKEN:-}" ]; then
  LIST_JSON="$(curl -sf --max-time 10 "http://localhost:18848/nacos/v3/admin/ns/service/list?accessToken=${NACOS_TOKEN}&pageNo=1&pageSize=20&namespaceId=public&groupName=PERSONAL_GROUP" || echo '{}')"
  for svc in resident-service repair-service notice-service; do
    echo "${LIST_JSON}" | grep -q "\"name\":\"${svc}\"" \
      && pass "Nacos 已注册 ${svc}" || fail "Nacos 未注册 ${svc}"
  done
fi

# ---------- 3) 扩到 2 实例 ----------
echo
echo "--- 3/6 resident-service 扩到 2 实例 ---"
docker compose up -d --no-recreate --scale resident-service=2 resident-service >/dev/null 2>&1 || true
sleep 5

instances=0
for _ in $(seq 1 30); do
  if [ -n "${NACOS_TOKEN:-}" ]; then
    instances="$(curl -sf --max-time 10 "http://localhost:18848/nacos/v3/admin/ns/instance/list?accessToken=${NACOS_TOKEN}&serviceName=resident-service&groupName=PERSONAL_GROUP&namespaceId=public&pageNo=1&pageSize=10" \
        | grep -o '"instanceId"' | wc -l | tr -d ' ')"
  fi
  [ "${instances:-0}" -ge 2 ] && break
  sleep 3
done
[ "${instances:-0}" -ge 2 ] && pass "resident-service 已注册 ${instances} 个实例" \
                            || fail "resident-service 实例数不足 2（当前 ${instances:-0}）"

# ---------- 4) 跨服务调用 + 负载均衡 ----------
echo
echo "--- 4/6 OpenFeign 负载均衡验证 ---"
resident_id="$(docker compose exec -T resident-db sh -c \
  'mysql -N -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" 2>/dev/null -e "
     INSERT INTO residents (full_name, phone, building, unit_number, created_at)
     VALUES (\"LB住户\", CONCAT(\"138\", LPAD(UNIX_TIMESTAMP(), 8, \"0\")), \"LB楼\", \"101\", NOW(3));
     SELECT LAST_INSERT_ID();"' | tail -n 1 | tr -d '\r')"

if [ -z "${resident_id}" ]; then
  fail "准备住户数据失败"
else
  pass "住户数据就绪 id=${resident_id}"

  # 记录调用前日志行数，便于只看本轮
  before_lines="$(docker logs "${REPAIR_CONTAINER}" 2>&1 | wc -l | tr -d ' ')"
  ok_count=0
  for i in $(seq 1 12); do
    http_code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 \
        -X POST "${REPAIR_URL}/api/repair-tickets" \
        -H 'Content-Type: application/json' \
        -d "{\"residentId\":${resident_id},\"title\":\"LB-test-${i}\",\"description\":\"lb probe\",\"location\":\"LB-${i}\"}")"
    [ "${http_code}" = "201" ] && ok_count=$((ok_count + 1)) || echo "    第 ${i} 次 HTTP=${http_code}"
    sleep 0.3
  done
  [ "${ok_count}" -eq 12 ] && pass "12 次跨服务报修全部 201" \
                           || fail "跨服务报修仅 ${ok_count}/12 成功"

  # Feign FULL 日志会打印 resident-service 返回的 X-Instance-Id。
  # 不能从请求 URL 判断实例：LoadBalancer 日志中的 URL 仍是逻辑服务名。
  targets="$(docker logs "${REPAIR_CONTAINER}" 2>&1 | tail -n +$((before_lines + 1)) \
      | grep -oiE 'x-instance-id:[[:space:]]*[^[:space:]]+' \
      | cut -d: -f2- | tr -d '\r' | awk '{$1=$1;print}' | sort -u)"
  distinct="$(printf '%s\n' "${targets}" | grep -c . || true)"
  echo "    本轮命中实例："
  if [ -n "${targets}" ]; then
    printf '%s\n' "${targets}" | sed 's/^/      -> /'
  else
    echo "      （未发现 X-Instance-Id 响应头）"
  fi
  [ "${distinct:-0}" -ge 2 ] && pass "请求命中 ${distinct} 个不同实例（负载均衡生效）" \
                             || fail "请求只命中 ${distinct} 个实例，未体现负载均衡"
fi

# ---------- 5) 404 语义保留 ----------
echo
echo "--- 5/6 业主不存在保留 404 ---"
http_code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 \
    -X POST "${REPAIR_URL}/api/repair-tickets" -H 'Content-Type: application/json' \
    -d '{"residentId":99999,"title":"x","description":"y","location":"z"}')"
[ "${http_code}" = "404" ] && pass "业主不存在返回 404" || fail "期望 404，实际 ${http_code}"

# ---------- 6) 停一个实例 → 恢复 ----------
echo
echo "--- 6/6 部分实例下线与恢复 ---"
stop_target="$(docker ps --filter "name=${RESIDENT_PREFIX}" --format '{{.Names}}' | head -n 1)"
if [ -z "${stop_target}" ]; then
  fail "找不到可停止的 resident-service 副本"
else
  docker stop "${stop_target}" >/dev/null 2>&1 || true
  sleep 8
  ok_after_stop=0
  for i in $(seq 1 6); do
    http_code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 \
        -X POST "${REPAIR_URL}/api/repair-tickets" -H 'Content-Type: application/json' \
        -d "{\"residentId\":${resident_id:-1},\"title\":\"after-stop-${i}\",\"description\":\"y\",\"location\":\"z\"}")"
    [ "${http_code}" = "201" ] && ok_after_stop=$((ok_after_stop + 1))
  done
  [ "${ok_after_stop}" -gt 0 ] && pass "停掉 1 个副本后仍可服务（${ok_after_stop}/6 成功）" \
                               || fail "停掉 1 个副本后全部失败"

  docker start "${stop_target}" >/dev/null 2>&1 || true
  recovered=0
  for _ in $(seq 1 30); do
    status="$(docker inspect --format '{{.State.Health.Status}}' "${stop_target}" 2>/dev/null || echo unknown)"
    [ "${status}" = "healthy" ] && { recovered=1; break; }
    sleep 3
  done
  [ "${recovered}" -eq 1 ] && pass "副本 ${stop_target} 已恢复健康" \
                           || fail "副本 ${stop_target} 未恢复健康"

  http_code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 \
      -X POST "${REPAIR_URL}/api/repair-tickets" -H 'Content-Type: application/json' \
      -d "{\"residentId\":${resident_id:-1},\"title\":\"after-recover\",\"description\":\"y\",\"location\":\"z\"}")"
  [ "${http_code}" = "201" ] && pass "恢复后调用返回 201" || fail "恢复后调用返回 ${http_code}"
fi

# ---------- 汇总 ----------
echo
echo "============================================="
echo "  PASS=${PASS_COUNT}  FAIL=${FAIL_COUNT}"
echo "============================================="
[ "${FAIL_COUNT}" -eq 0 ]
