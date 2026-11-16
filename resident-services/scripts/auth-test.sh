#!/usr/bin/env bash
# week-09 端到端鉴权验证：API Gateway + JWT + 角色校验
# 必须从仓库根目录执行：bash scripts/auth-test.sh
# 所有请求打 Gateway 统一入口（默认 8080），不经业务服务直连。

set -uo pipefail

PASS_COUNT=0
FAIL_COUNT=0

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin123}"
RESIDENT_USERNAME="${RESIDENT_USERNAME:-resident1}"
RESIDENT_PASSWORD="${RESIDENT_PASSWORD:-resident123}"

pass() { echo "[PASS] $1"; PASS_COUNT=$((PASS_COUNT + 1)); }
fail() { echo "[FAIL] $1"; FAIL_COUNT=$((FAIL_COUNT + 1)); }

assert_code() {
    local got="$1" expected="$2" label="$3"
    [ "${got}" = "${expected}" ] && pass "${label} (HTTP ${got})" \
                                 || fail "${label} (期望 HTTP ${expected}, 实际 ${got})"
}

echo "============================================="
echo "  Week-09 鉴权端到端验证"
echo "  Gateway: ${GATEWAY_URL}"
echo "============================================="

# 等待网关健康
echo
echo "--- 0/4 网关就绪检查 ---"
deadline=$((SECONDS + 60))
while [ ${SECONDS} -lt ${deadline} ]; do
    code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 "${GATEWAY_URL}/actuator/health" 2>/dev/null || echo 000)"
    [ "${code}" = "200" ] && break
    sleep 3
done
[ "${code}" = "200" ] && pass "Gateway 健康检查 UP" || fail "Gateway 健康检查超时 (HTTP ${code})"

# 确保 auth-service 已注册（网关依赖它）
sleep 3

# ---------- 1) 未登录访问受保护接口 → 401 ----------
echo
echo "--- 1/4 未登录访问应被拒绝 ---"
code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 8 \
    "${GATEWAY_URL}/api/repair-tickets")"
assert_code "${code}" "401" "未登录访问 GET /api/repair-tickets → 401"

# ---------- 2) 登录获取 Token ----------
echo
echo "--- 2/4 登录获取 Token ---"
admin_login_body="$(curl -sS --max-time 10 -X POST \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${ADMIN_USERNAME}\",\"password\":\"${ADMIN_PASSWORD}\"}" \
    "${GATEWAY_URL}/api/auth/login")"
admin_login_code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 -X POST \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${ADMIN_USERNAME}\",\"password\":\"${ADMIN_PASSWORD}\"}" \
    "${GATEWAY_URL}/api/auth/login")"
assert_code "${admin_login_code}" "200" "管理员登录 HTTP 200"

ADMIN_TOKEN="$(printf '%s' "${admin_login_body}" | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])' 2>/dev/null || \
              printf '%s' "${admin_login_body}" | grep -oE '"token":"[^"]+"' | head -1 | cut -d'"' -f4)"
[ -n "${ADMIN_TOKEN}" ] && pass "获取管理员 JWT (长度 ${#ADMIN_TOKEN})" \
                       || fail "未能解析 admin Token: ${admin_login_body}"

resident_login_body="$(curl -sS --max-time 10 -X POST \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${RESIDENT_USERNAME}\",\"password\":\"${RESIDENT_PASSWORD}\"}" \
    "${GATEWAY_URL}/api/auth/login")"
resident_login_code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 -X POST \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${RESIDENT_USERNAME}\",\"password\":\"${RESIDENT_PASSWORD}\"}" \
    "${GATEWAY_URL}/api/auth/login")"
assert_code "${resident_login_code}" "200" "业主登录 HTTP 200"

RESIDENT_TOKEN="$(printf '%s' "${resident_login_body}" | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])' 2>/dev/null || \
                 printf '%s' "${resident_login_body}" | grep -oE '"token":"[^"]+"' | head -1 | cut -d'"' -f4)"
[ -n "${RESIDENT_TOKEN}" ] && pass "获取业主 JWT (长度 ${#RESIDENT_TOKEN})" \
                           || fail "未能解析 resident Token: ${resident_login_body}"

# ---------- 3) 携带 Token 访问受保护接口 → 200 ----------
echo
echo "--- 3/4 携带 Token 应成功访问 ---"
code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 8 \
    -H "Authorization: Bearer ${RESIDENT_TOKEN}" \
    "${GATEWAY_URL}/api/repair-tickets")"
assert_code "${code}" "200" "业主 Token 访问 GET /api/repair-tickets → 200"

code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 8 \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    "${GATEWAY_URL}/api/repair-tickets")"
assert_code "${code}" "200" "管理员 Token 访问 GET /api/repair-tickets → 200"

# ---------- 4) 越权测试：业主 Token 访问管理接口 → 403 ----------
echo
echo "--- 4/4 低权限越权应被拒绝 ---"
code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 8 \
    -X POST \
    -H "Authorization: Bearer ${RESIDENT_TOKEN}" \
    -H 'Content-Type: application/json' \
    -d '{"residentId":1,"title":"越权测试","description":"x","location":"y"}' \
    "${GATEWAY_URL}/api/repair-tickets")"
assert_code "${code}" "403" "业主 Token 越权 POST /api/repair-tickets → 403"

# 管理员 Token 访问管理接口应成功
code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 8 \
    -X POST \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H 'Content-Type: application/json' \
    -d '{"residentId":1,"title":"admin-create","description":"x","location":"y"}' \
    "${GATEWAY_URL}/api/repair-tickets")"
assert_code "${code}" "201" "管理员 Token POST /api/repair-tickets → 201"

# ---------- 汇总 ----------
echo
echo "============================================="
echo "  PASS=${PASS_COUNT}  FAIL=${FAIL_COUNT}"
echo "============================================="
[ "${FAIL_COUNT}" -eq 0 ]
