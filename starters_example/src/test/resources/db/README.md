# 整合測試資料庫初始化

本目錄提供 `starters_example` 整合測試所需的資料庫初始化腳本，涵蓋 MySQL 與 PostgreSQL，
用於驗證 `db-spring-boot-starter` 的多資料來源動態切換（含跨資料庫類型切換）。

## 資料源與測試資料對照

對應 `src/main/resources/data-source.properties` 的設定：

| 資料源 key | 類型 | 連線 | 帳號 | 共用資料 | 獨有標記 |
|---|---|---|---|---|---|
| `example1`（primary） | MySQL | `localhost:3306/example1` | user1 / example1 | Tom, Jen, Andy, Gary | `OnlyExample1` |
| `example2` | MySQL | `localhost:3306/example2` | user2 / example2 | Tom, Jen, Andy, Gary | `OnlyExample2` |
| `postgres` | PostgreSQL | `localhost:5432/pgdb` | pguser / pgpass | Tom, Jen, Andy, Gary | `OnlyPostgres` |

每個資料源都有相同的基礎資料（Tom/Jen/Andy/Gary），可跑一般查詢測試；
各自再放一筆**獨有標記**，供切換測試辨識查詢究竟被路由到哪個資料源（排除 fallback 假象）。

## 對應的測試

| 測試類別 | 驗證內容 |
|---|---|
| `DBExampleServiceTest` | primary 資料源的 JPA 查詢（`Gary`） |
| `DynamicDataSourceSwitchTest` | MySQL 內 example1 ↔ example2 動態切換 |
| `CrossDbSwitchTest` | 跨資料庫類型 MySQL(example1) ↔ PostgreSQL(postgres) 切換 |

## 前置：Docker 容器

腳本假設使用下列容器（容器名稱固定為 `mysql-db` / `postgres-db`）：

```yaml
mysql:
  image: mysql:latest
  container_name: mysql-db
  environment:
    MYSQL_ROOT_PASSWORD: rootpass
  ports: [ "3306:3306" ]

postgres:
  image: postgres:latest
  container_name: postgres-db
  environment:
    POSTGRES_USER: pguser
    POSTGRES_PASSWORD: pgpass
    POSTGRES_DB: pgdb
  ports: [ "5432:5432" ]
```

## 套用方式

### 方式一：一鍵腳本（建議）

```bash
# Git Bash / WSL
./apply.sh
```

```powershell
# PowerShell
./apply.ps1
```

### 方式二：手動以 docker exec 灌入

```bash
docker exec -i mysql-db    mysql -uroot -prootpass        < mysql-init.sql
docker exec -i postgres-db psql  -U pguser -d pgdb        < postgres-init.sql
```

腳本皆為**冪等**（資料表 DROP 後重建），可重複執行以重置測試資料。
