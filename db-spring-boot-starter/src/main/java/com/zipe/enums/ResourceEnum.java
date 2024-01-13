package com.zipe.enums;

import com.zipe.util.string.StringConstant;
import org.apache.commons.lang3.StringUtils;

/**
 * 記錄Class path下特定目錄的資源檔位址
 */
public enum ResourceEnum {

    SQL("/sql", ".sql");

    private String dir;
    private String file;
    private final String extension;

    /**
     * @param dir       資源擋路徑
     * @param extension 檔案類型
     */
    ResourceEnum(String dir, String extension) {
        this.dir = dir;
        this.extension = extension;
    }

    public ResourceEnum getResource(String fileName) {
        this.file = StringConstant.SLASH + fileName;
        return this;
    }

    public ResourceEnum getResource(String dir, String fileName) {
        if (StringUtils.isNotBlank(dir)) {
            this.dir = this.dir + StringConstant.SLASH + dir;
        }
        this.file = StringConstant.SLASH + fileName;
        return this;
    }

    public String file() {
        return this.file;
    }

    public String dir() {
        return this.dir;
    }

    public String extension() {
        return this.extension;
    }

}
