#!/usr/bin/env bash
# 套用整合測試資料庫初始化腳本（MySQL + PostgreSQL）
# 用法：於本目錄執行 ./apply.sh
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "[1/2] 套用 MySQL 初始化（example1 / example2）..."
docker exec -i mysql-db mysql -uroot -prootpass < "$DIR/mysql-init.sql"
echo "      完成。"

echo "[2/2] 套用 PostgreSQL 初始化（pgdb）..."
docker exec -i postgres-db psql -U pguser -d pgdb -q < "$DIR/postgres-init.sql"
echo "      完成。"

echo ""
echo "驗證："
echo "  MySQL example1 :" $(docker exec -i mysql-db mysql -uroot -prootpass example1 -N -e "SELECT GROUP_CONCAT(Name) FROM user_main;" 2>/dev/null)
echo "  MySQL example2 :" $(docker exec -i mysql-db mysql -uroot -prootpass example2 -N -e "SELECT GROUP_CONCAT(Name) FROM user_main;" 2>/dev/null)
echo "  PostgreSQL pgdb:" $(docker exec -i postgres-db psql -U pguser -d pgdb -t -A -c "SELECT string_agg(name, ',') FROM user_main;" 2>/dev/null)
echo ""
echo "測試資料初始化完成。"
