#!/bin/bash
set -euo pipefail
NACOS_CONSOLE_URL="http://localhost:18081/nacos"
RESIDENT_HEALTH="http://localhost:8091/actuator/health"
REPAIR_HEALTH="http://localhost:8092/actuator/health"
echo "===== 1. 检查 Nacos 控制台可访问 ====="
if ! curl --connect-timeout 5 -fs "$NACOS_CONSOLE_URL" > /dev/null; then
  echo "❌ Nacos 控制台访问失败: $NACOS_CONSOLE_URL"
  exit 1
fi
echo "✅ Nacos 控制台访问正常"
echo -e "\n===== 2. 检查 resident-service 健康端点 ====="
if ! curl --connect-timeout 5 -fs "$RESIDENT_HEALTH" > /dev/null; then
  echo "❌ resident-service health 检查失败"
  exit 2
fi
curl "$RESIDENT_HEALTH"
echo ""
echo "✅ resident-service 健康检查通过"
echo -e "\n===== 3. 检查 repair-service 健康端点 ====="
if ! curl --connect-timeout 5 -fs "$REPAIR_HEALTH" > /dev/null; then
  echo "❌ repair-service health 检查失败"
  exit 3
fi
curl "$REPAIR_HEALTH"
echo ""
echo "✅ repair-service 健康检查通过"
echo -e "\n🎉 全部健康检查通过！脚本退出码: 0"
exit 0
