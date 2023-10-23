package com.zipe.enums;

public enum DatasourceType {
    MYSQL("mysql"),
    MSSQL("mssql"),
    AS400("as400"),
    DB2("db2"),
    JNDI("jndi");

    private String datasourceName;

    DatasourceType(String datasourceName) {
        this.datasourceName = datasourceName;
    }

    public static DatasourceType ifContains(String name) {
        for (DatasourceType enumValue : values()) {
            if (name.contains(enumValue.datasourceName)) {
                return enumValue;
            }
        }
        return MYSQL;
    }
}
