-- =====================================================================
-- starters_example 整合測試用 MySQL 初始化腳本
-- 對應 data-source.properties 的 example1（primary）與 example2 資料源
-- 以 root 帳號執行；可重複執行（冪等）
--
-- 套用方式：
--   docker exec -i mysql-db mysql -uroot -prootpass < mysql-init.sql
-- =====================================================================

-- ---------- 資料庫 ----------
CREATE DATABASE IF NOT EXISTS example1 CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS example2 CHARACTER SET utf8mb4;

-- ---------- 帳號與授權 ----------
CREATE USER IF NOT EXISTS 'user1'@'%' IDENTIFIED BY 'example1';
CREATE USER IF NOT EXISTS 'user2'@'%' IDENTIFIED BY 'example2';
GRANT ALL PRIVILEGES ON example1.* TO 'user1'@'%';
GRANT ALL PRIVILEGES ON example2.* TO 'user2'@'%';
-- 動態切換測試會以單一執行緒跨資料源查詢，故兩帳號互相授權
GRANT ALL PRIVILEGES ON example2.* TO 'user1'@'%';
GRANT ALL PRIVILEGES ON example1.* TO 'user2'@'%';
FLUSH PRIVILEGES;

-- ---------- example1（含獨有資料 OnlyExample1）----------
USE example1;
DROP TABLE IF EXISTS user_main;
DROP TABLE IF EXISTS user_detail;
CREATE TABLE user_main (
  Id   INT(11)      NOT NULL AUTO_INCREMENT,
  Name VARCHAR(100) NOT NULL,
  PRIMARY KEY (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE user_detail (
  Name   VARCHAR(100) NOT NULL,
  Gender VARCHAR(1)   NOT NULL,
  PRIMARY KEY (Name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO user_main (Name) VALUES ('Tom'), ('Jen'), ('Andy'), ('Gary'), ('OnlyExample1');
INSERT INTO user_detail (Name, Gender) VALUES ('Tom','M'), ('Jen','F'), ('Andy','M'), ('Gary','M');

-- ---------- example2（含獨有資料 OnlyExample2）----------
USE example2;
DROP TABLE IF EXISTS user_main;
DROP TABLE IF EXISTS user_detail;
CREATE TABLE user_main (
  Id   INT(11)      NOT NULL AUTO_INCREMENT,
  Name VARCHAR(100) NOT NULL,
  PRIMARY KEY (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE user_detail (
  Name   VARCHAR(100) NOT NULL,
  Gender VARCHAR(1)   NOT NULL,
  PRIMARY KEY (Name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO user_main (Name) VALUES ('Tom'), ('Jen'), ('Andy'), ('Gary'), ('OnlyExample2');
INSERT INTO user_detail (Name, Gender) VALUES ('Tom','M'), ('Jen','F'), ('Andy','M'), ('Gary','M');
