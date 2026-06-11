# 套用整合測試資料庫初始化腳本（MySQL + PostgreSQL）
# 用法：於本目錄執行 ./apply.ps1
$ErrorActionPreference = 'Stop'
$dir = $PSScriptRoot

Write-Host '[1/2] 套用 MySQL 初始化（example1 / example2）...'
Get-Content "$dir/mysql-init.sql" -Raw | docker exec -i mysql-db mysql -uroot -prootpass
Write-Host '      完成。'

Write-Host '[2/2] 套用 PostgreSQL 初始化（pgdb）...'
Get-Content "$dir/postgres-init.sql" -Raw | docker exec -i postgres-db psql -U pguser -d pgdb -q
Write-Host '      完成。'

Write-Host ''
Write-Host '驗證：'
$e1 = docker exec -i mysql-db mysql -uroot -prootpass example1 -N -e 'SELECT GROUP_CONCAT(Name) FROM user_main;'
$e2 = docker exec -i mysql-db mysql -uroot -prootpass example2 -N -e 'SELECT GROUP_CONCAT(Name) FROM user_main;'
$pg = docker exec -i postgres-db psql -U pguser -d pgdb -t -A -c 'SELECT string_agg(name, '','') FROM user_main;'
Write-Host "  MySQL example1 : $e1"
Write-Host "  MySQL example2 : $e2"
Write-Host "  PostgreSQL pgdb: $pg"
Write-Host ''
Write-Host '測試資料初始化完成。'
