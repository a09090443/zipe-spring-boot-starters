-- iam-spring-boot-starter 資料表結構（標準 SQL，相容 H2 / MySQL / PostgreSQL）
-- 預設不自動執行（iam.ddl.init=false），交由業務專案以 Flyway / Liquibase 或手動建表掌控。

-- 帳號
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

-- 群組（角色）
CREATE TABLE IF NOT EXISTS iam_group (
    id          BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(100) NOT NULL UNIQUE,
    name        VARCHAR(100),
    description VARCHAR(255),
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE
);

-- 權限
CREATE TABLE IF NOT EXISTS iam_permission (
    id          BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(100) NOT NULL UNIQUE,
    name        VARCHAR(100),
    description VARCHAR(255),
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE
);

-- 帳號↔群組 關聯表
CREATE TABLE IF NOT EXISTS iam_account_group (
    account_id BIGINT NOT NULL,
    group_id   BIGINT NOT NULL,
    PRIMARY KEY (account_id, group_id),
    CONSTRAINT fk_iam_ag_account FOREIGN KEY (account_id) REFERENCES iam_account (id),
    CONSTRAINT fk_iam_ag_group   FOREIGN KEY (group_id)   REFERENCES iam_group (id)
);

-- 群組↔權限 關聯表
CREATE TABLE IF NOT EXISTS iam_group_permission (
    group_id      BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (group_id, permission_id),
    CONSTRAINT fk_iam_gp_group      FOREIGN KEY (group_id)      REFERENCES iam_group (id),
    CONSTRAINT fk_iam_gp_permission FOREIGN KEY (permission_id) REFERENCES iam_permission (id)
);
