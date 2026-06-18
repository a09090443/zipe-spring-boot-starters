-- iam-spring-boot-starter 示範用資料表與種子資料（MySQL / H2 相容）
-- 範例的 spring.sql.init.mode 預設為 never、Hibernate ddl-auto=none，
-- 故此檔不會自動執行；於主資料源（example1）手動套用後即可體驗 iam 整合。
--   MySQL:  mysql -u user1 -p example1 < init/iam-demo.sql
--   H2:     於 H2 console 貼上執行
-- 註：display_name 含中文，MySQL 請確保資料庫／連線使用 utf8mb4，
--     並在 JDBC URL 帶上 ?useUnicode=true&characterEncoding=utf8 以免亂碼。

-- ===== 資料表（同 iam-starter 的 schema-iam.sql）=====
CREATE TABLE IF NOT EXISTS iam_account (
    id           BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
    username     VARCHAR(100) NOT NULL UNIQUE,
    password     VARCHAR(200),
    display_name VARCHAR(100),
    enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    locked       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP,
    updated_at   TIMESTAMP
);

CREATE TABLE IF NOT EXISTS iam_group (
    id          BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(100) NOT NULL UNIQUE,
    name        VARCHAR(100),
    description VARCHAR(255),
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS iam_permission (
    id          BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(100) NOT NULL UNIQUE,
    name        VARCHAR(100),
    description VARCHAR(255),
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS iam_account_group (
    account_id BIGINT NOT NULL,
    group_id   BIGINT NOT NULL,
    PRIMARY KEY (account_id, group_id)
);

CREATE TABLE IF NOT EXISTS iam_group_permission (
    group_id      BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (group_id, permission_id)
);

-- ===== 種子資料 =====
-- 權限（泛用具名授權點）
INSERT INTO iam_permission (id, code, name, description) VALUES
    (1, 'ORDER_EXPORT', '匯出訂單', '允許匯出訂單報表'),
    (2, 'USER_MANAGE',  '使用者管理', '允許管理帳號');

-- 群組（角色）
INSERT INTO iam_group (id, code, name, description) VALUES
    (1, 'ADMIN', '系統管理員', '具備全部權限'),
    (2, 'USER',  '一般使用者', '僅可匯出訂單');

-- 帳號（password 欄為 BCrypt）。範例預設 custom 模式（帳密查 user_login，此欄不參與登入）；
--  alice/bob 供 /iam-demo/authorities/{username} 展示授權解析；
--  user01/user02 對應 user_login 的登入帳號(user01/1234、user02/abcd)，custom 登入後即帶上 iam 權限。
--  註：basic 模式下 iam 的 IamUserDetailsService 會改以此表驗證登入，但種子資料 password 皆為 NULL，
--  故 basic 表單登入無法使用；若要用 basic，請於此欄填入真實 BCrypt 密碼雜湊（並補一個 admin 帳號）。
INSERT INTO iam_account (id, username, password, display_name, enabled, locked) VALUES
    (1, 'alice',  NULL, 'Alice（管理員）',   TRUE, FALSE),
    (2, 'bob',    NULL, 'Bob（一般使用者）', TRUE, FALSE),
    (3, 'user01', NULL, 'User01（一般使用者）', TRUE, FALSE),
    (4, 'user02', NULL, 'User02（管理員）',   TRUE, FALSE);

-- 群組掛權限：ADMIN→(ORDER_EXPORT, USER_MANAGE)、USER→(ORDER_EXPORT)
INSERT INTO iam_group_permission (group_id, permission_id) VALUES
    (1, 1), (1, 2), (2, 1);

-- 帳號加入群組：alice/user02→ADMIN（ORDER_EXPORT + USER_MANAGE）、
--  bob/user01→USER（僅 ORDER_EXPORT）。
--  對應 custom 登入：user01/1234 僅可匯出訂單，user02/abcd 另可管理使用者。
INSERT INTO iam_account_group (account_id, group_id) VALUES
    (1, 1), (2, 2), (3, 2), (4, 1);
