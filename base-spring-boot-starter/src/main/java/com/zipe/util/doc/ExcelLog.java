package com.zipe.util.doc;

/**
 * Excel 匯入/匯出操作的日誌記錄模型。
 * <p>
 * 用於封裝 Excel 處理過程中的錯誤或提示訊息，記錄發生問題的列號、
 * 對應的資料物件，以及具體的日誌說明文字，便於呼叫端彙整並回報給使用者。
 * </p>
 */
public class ExcelLog {
    /** 發生問題的 Excel 列號（從 1 開始計算） */
    private Integer rowNum;
    /** 該列所對應的資料物件（通常為對應 Java Bean） */
    private Object object;
    /** 日誌訊息內容，用於描述該列的錯誤或提示說明 */
    private String log;

    /**
     * 取得發生問題的 Excel 列號。
     *
     * @return Excel 列號，若未設定則為 {@code null}
     */
    public Integer getRowNum() {
        return rowNum;
    }

    /**
     * 設定發生問題的 Excel 列號。
     *
     * @param rowNum Excel 列號（從 1 開始計算）
     */
    public void setRowNum(Integer rowNum) {
        this.rowNum = rowNum;
    }

    /**
     * 取得該日誌對應的資料物件。
     *
     * @return 對應的資料物件
     */
    public Object getObject() {
        return object;
    }

    /**
     * 設定該日誌對應的資料物件。
     *
     * @param object 對應的資料物件（通常為 Java Bean）
     */
    public void setObject(Object object) {
        this.object = object;
    }

    /**
     * 取得日誌訊息內容。
     *
     * @return 描述錯誤或提示的日誌文字
     */
    public String getLog() {
        return log;
    }

    /**
     * 以資料物件與日誌訊息建立 ExcelLog，不指定列號。
     * <p>
     * 適用於無需追蹤具體列號的簡易日誌情境。
     * </p>
     *
     * @param object 對應的資料物件
     * @param log    日誌訊息內容
     */
    public ExcelLog(Object object, String log) {
        super();
        this.object = object;
        this.log = log;
    }

    /**
     * 以資料物件、日誌訊息與列號建立 ExcelLog。
     * <p>
     * 適用於需要精確標示問題發生於哪一列的情境，方便使用者定位錯誤。
     * </p>
     *
     * @param object  對應的資料物件
     * @param log     日誌訊息內容
     * @param rowNum  Excel 列號（從 1 開始計算）
     */
    public ExcelLog(Object object, String log, Integer rowNum) {
        super();
        this.rowNum = rowNum;
        this.object = object;
        this.log = log;
    }

    /**
     * 設定日誌訊息內容。
     *
     * @param log 描述錯誤或提示的日誌文字
     */
    public void setLog(String log) {
        this.log = log;
    }

}
