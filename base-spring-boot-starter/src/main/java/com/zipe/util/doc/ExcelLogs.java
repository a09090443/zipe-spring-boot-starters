package com.zipe.util.doc;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel 匯入／匯出的整批日誌容器。
 *
 * <p>封裝一次 Excel 處理作業所產生的所有 {@link ExcelLog} 記錄，
 * 並提供快速判斷「是否含有錯誤」的旗標，以及篩選出非空白錯誤訊息的便利方法。</p>
 */
public class ExcelLogs {
    /** 是否含有錯誤；預設為 {@code false}，出現錯誤列時設為 {@code true}。 */
    private Boolean hasError;
    /** 所有列的日誌清單，每個元素對應一列的處理結果。 */
    private List<ExcelLog> logList;

    /**
     * 建立 ExcelLogs 實例，並將錯誤旗標初始化為 {@code false}。
     */
    public ExcelLogs() {
        super();
        hasError = false;
    }

    /**
     * 取得是否含有錯誤的旗標。
     *
     * @return 若本次處理有任一列發生錯誤則回傳 {@code true}，否則回傳 {@code false}
     */
    public Boolean getHasError() {
        return hasError;
    }

    /**
     * 設定是否含有錯誤的旗標。
     *
     * @param hasError {@code true} 表示處理過程中出現錯誤
     */
    public void setHasError(Boolean hasError) {
        this.hasError = hasError;
    }

    /**
     * 取得所有列的日誌清單（包含正常列與錯誤列）。
     *
     * @return 完整的 {@link ExcelLog} 清單
     */
    public List<ExcelLog> getLogList() {
        return logList;
    }

    /**
     * 從 {@code logList} 中篩選出含有錯誤訊息的日誌，回傳錯誤清單。
     *
     * <p>判斷條件：{@link ExcelLog} 不為 {@code null}，且其 log 訊息非空白。</p>
     *
     * @return 僅包含錯誤記錄的 {@link ExcelLog} 清單；若無錯誤則回傳空清單
     */
    public List<ExcelLog> getErrorLogList() {
        List<ExcelLog> errList = new ArrayList<ExcelLog>();
        for (ExcelLog log : this.logList) {
            // 只收錄 log 訊息非空白的記錄，代表該列確實有錯誤說明
            if (log != null && ExcelUtil.isNotBlank(log.getLog())) {
                errList.add(log);
            }
        }
        return errList;
    }

    /**
     * 設定所有列的日誌清單。
     *
     * @param logList 包含每列處理結果的 {@link ExcelLog} 清單
     */
    public void setLogList(List<ExcelLog> logList) {
        this.logList = logList;
    }

}
