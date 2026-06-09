package com.zipe.enums;

import com.zipe.util.string.StringConstant;
import org.apache.commons.lang3.StringUtils;

/**
 * 記錄 classpath 下特定目錄的資源檔位址。
 * <p>
 * 每個列舉值對應一種資源類型（例如 SQL 腳本），
 * 並封裝該類型的預設目錄路徑與副檔名。
 * 透過 {@link #getResource(String)} 或 {@link #getResource(String, String)}
 * 設定具體檔案名稱後，可取得完整的路徑資訊供載入使用。
 * </p>
 *
 * @author adam.yeh
 * @create date: NOV 16, 2017
 */
public enum ResourceEnum {

    /** SQL 腳本資源類型，預設目錄為 /sql，副檔名為 .sql */
    SQL("/sql", ".sql");

    /** 資源檔所在目錄路徑（可在呼叫 getResource 時附加子目錄） */
    private String dir;
    /** 資源檔的相對路徑（含前置斜線與檔名） */
    private String file;
    /** 資源檔的純檔案名稱（不含路徑） */
    private String fileName;
    /** 資源檔的副檔名（例如 .sql） */
    private final String extension;

    /**
     * 初始化資源列舉值。
     *
     * @param dir       資源檔目錄路徑
     * @param extension 檔案副檔名（含點號，例如 {@code ".sql"}）
     */
    ResourceEnum(String dir, String extension) {
        this.dir = dir;
        this.extension = extension;
    }

    /**
     * 以預設目錄設定目標資源檔名，並回傳自身以支援鏈式呼叫。
     *
     * @param fileName 資源檔名稱（不含路徑）
     * @return 目前 {@link ResourceEnum} 實例（已設定 file 與 fileName）
     */
    public ResourceEnum getResource(String fileName) {
        this.file = StringConstant.SLASH + fileName;
        this.fileName = fileName;
        return this;
    }

    /**
     * 以指定子目錄與檔名設定目標資源檔，並回傳自身以支援鏈式呼叫。
     * <p>
     * 若 {@code dir} 不為空白，則會將其附加至預設目錄後，
     * 組成完整的子目錄路徑；若為空白則略過，維持原目錄不變。
     * </p>
     *
     * @param dir      子目錄名稱；若為空白則不附加
     * @param fileName 資源檔名稱（不含路徑）
     * @return 目前 {@link ResourceEnum} 實例（已設定 dir、file 與 fileName）
     */
    public ResourceEnum getResource(String dir, String fileName) {
        // 僅在 dir 非空白時才附加子目錄，避免產生多餘的斜線
        if (StringUtils.isNotBlank(dir)) {
            this.dir = this.dir + StringConstant.SLASH + dir;
        }
        this.file = StringConstant.SLASH + fileName;
        this.fileName = fileName;
        return this;
    }

    /**
     * 取得資源檔的相對路徑（含前置斜線）。
     *
     * @return 資源檔相對路徑，例如 {@code "/example.sql"}
     */
    public String file() {
        return this.file;
    }

    /**
     * 取得資源檔的純檔案名稱。
     *
     * @return 資源檔名稱，例如 {@code "example.sql"}
     */
    public String fileName() {
        return this.fileName;
    }

    /**
     * 取得資源檔所在的目錄路徑。
     *
     * @return 目錄路徑，例如 {@code "/sql"} 或 {@code "/sql/subdir"}
     */
    public String dir() {
        return this.dir;
    }

    /**
     * 取得資源檔的副檔名。
     *
     * @return 副檔名（含點號），例如 {@code ".sql"}
     */
    public String extension() {
        return this.extension;
    }

}
