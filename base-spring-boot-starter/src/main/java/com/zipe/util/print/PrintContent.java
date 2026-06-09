package com.zipe.util.print;

import java.awt.Font;

/**
 * 列印內容值物件（Value Object），封裝單一文字區塊的列印資訊。
 * <p>
 * 包含要輸出的文字內容、在列印頁面上的座標位置（x、y）以及字體設定。
 * 搭配 {@link PrintUtils} 使用，將此物件傳入後即可在指定座標繪製文字。
 * </p>
 *
 * @author : Gary Tsai
 * @created : @Date 2021/1/27 上午 10:10
 **/
public class PrintContent {
    /**
     * 字體風格；預設使用「標楷體」粗體 12 點，適合一般中文列印需求。
     */
    private Font font = new Font("標楷體", Font.BOLD, 12);
    /**
     * 列印內容；要輸出至列印頁面的文字字串。
     */
    private String content;
    /**
     * x 軸座標；以點（point）為單位，表示文字起始點距頁面左緣的距離。
     */
    private int x;
    /**
     * y 軸座標；以點（point）為單位，表示文字基準線距頁面上緣的距離。
     */
    private int y;

    /**
     * 無參數建構子；所有欄位保持預設值（字體為標楷體粗體 12 點，內容與座標為 Java 預設初始值）。
     */
    public PrintContent() {
    }

    /**
     * 使用文字內容與座標建立列印物件；字體沿用預設值（標楷體粗體 12 點）。
     *
     * @param content 要列印的文字內容
     * @param x       文字起始點的 x 軸座標（單位：點）
     * @param y       文字基準線的 y 軸座標（單位：點）
     */
    public PrintContent(String content, int x, int y) {
        this.content = content;
        this.x = x;
        this.y = y;
    }

    /**
     * 使用文字內容、座標與自訂字體建立列印物件。
     *
     * @param content 要列印的文字內容
     * @param x       文字起始點的 x 軸座標（單位：點）
     * @param y       文字基準線的 y 軸座標（單位：點）
     * @param font    自訂字體，覆蓋預設的標楷體粗體 12 點
     */
    public PrintContent(String content, int x, int y, Font font) {
        this.content = content;
        this.x = x;
        this.y = y;
        this.font = font;
    }

    /**
     * 取得列印使用的字體。
     *
     * @return 目前設定的 {@link Font} 物件
     */
    public Font getFont() {
        return font;
    }

    /**
     * 設定列印使用的字體。
     *
     * @param font 要套用的 {@link Font} 物件
     */
    public void setFont(Font font) {
        this.font = font;
    }

    /**
     * 取得要列印的文字內容。
     *
     * @return 列印文字字串
     */
    public String getContent() {
        return content;
    }

    /**
     * 設定要列印的文字內容。
     *
     * @param content 列印文字字串
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 取得文字起始點的 x 軸座標。
     *
     * @return x 軸座標值（單位：點）
     */
    public int getX() {
        return x;
    }

    /**
     * 設定文字起始點的 x 軸座標。
     *
     * @param x x 軸座標值（單位：點）
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * 取得文字基準線的 y 軸座標。
     *
     * @return y 軸座標值（單位：點）
     */
    public int getY() {
        return y;
    }

    /**
     * 設定文字基準線的 y 軸座標。
     *
     * @param y y 軸座標值（單位：點）
     */
    public void setY(int y) {
        this.y = y;
    }
}
