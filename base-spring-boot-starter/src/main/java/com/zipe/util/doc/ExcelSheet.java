package com.zipe.util.doc;

import java.util.Collection;
import java.util.Map;

/**
 * 用於匯出多個 Sheet 的資料封裝值物件（Value Object）。
 *
 * <p>搭配 {@code ExcelUtil} 使用，每個 {@code ExcelSheet} 代表 Excel 活頁簿中的一個工作表，
 * 包含頁簽名稱、欄位標題對照表，以及要匯出的資料集合。
 * 型別參數 {@code T} 為資料列所對應的 JavaBean 型別。</p>
 *
 * @param <T> 資料集合的元素型別
 */
public class ExcelSheet<T> {
    /** Excel 工作表的頁簽名稱 */
    private String sheetName;
    /** 欄位標題對照表，Key 為 JavaBean 屬性名稱，Value 為顯示用的欄位標題文字 */
    private Map<String, String> headers;
    /** 要匯出至該工作表的資料集合 */
    private Collection<T> dataset;

    /**
     * 取得 Excel 工作表的頁簽名稱。
     *
     * @return 頁簽名稱字串
     */
    public String getSheetName() {
        return sheetName;
    }

    /**
     * 設定 Excel 工作表的頁簽名稱。
     *
     * @param sheetName 頁簽名稱
     */
    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    /**
     * 取得欄位標題對照表。
     * Key 為 JavaBean 屬性名稱，Value 為欄位顯示標題文字。
     *
     * @return 欄位標題對照表
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * 設定欄位標題對照表。
     * Key 為 JavaBean 屬性名稱，Value 為欄位顯示標題文字。
     *
     * @param headers 欄位標題對照表
     */
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    /**
     * 取得要匯出的資料集合。
     * 集合中每個元素代表 Excel 工作表的一列資料。
     *
     * @return 資料集合
     */
    public Collection<T> getDataset() {
        return dataset;
    }

    /**
     * 設定要匯出的資料集合。
     * 集合中每個元素代表 Excel 工作表的一列資料。
     *
     * @param dataset 資料集合
     */
    public void setDataset(Collection<T> dataset) {
        this.dataset = dataset;
    }

}
