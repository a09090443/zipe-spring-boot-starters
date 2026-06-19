-- =====================================================================
-- starters_example 整合測試用 PostgreSQL 初始化腳本
-- 對應 data-source.properties 的 postgres 資料源（驗證跨資料庫類型動態切換）
-- 以 pguser 連線 pgdb 執行；可重複執行（冪等）
--
-- 欄位名稱使用小寫（id / name / gender），對應 Hibernate 產生的 SQL
--
-- 套用方式：
--   docker exec -i postgres-db psql -U pguser -d pgdb < postgres-init.sql
-- =====================================================================

DROP TABLE IF EXISTS user_main;
DROP TABLE IF EXISTS user_detail;

CREATE TABLE user_main (
  id   SERIAL       PRIMARY KEY,
  name VARCHAR(100) NOT NULL
);
CREATE TABLE user_detail (
  name   VARCHAR(100) PRIMARY KEY,
  gender VARCHAR(1)   NOT NULL
);

-- 含 PostgreSQL 獨有資料 OnlyPostgres，供跨資料庫類型切換測試辨識
INSERT INTO user_main (name) VALUES ('Tom'), ('Jen'), ('Andy'), ('Gary'), ('OnlyPostgres');
INSERT INTO user_detail (name, gender) VALUES ('Tom','M'), ('Jen','F'), ('Andy','M'), ('Gary','M');
