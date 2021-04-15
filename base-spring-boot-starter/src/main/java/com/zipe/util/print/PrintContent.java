package com.zipe.util.print;

import java.awt.Font;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/1/27 上午 10:10
 **/
public class PrintContent {
    /**
     * 字體風格
     */
    private Font font = new Font("標楷體", Font.BOLD, 12);
    /**
     * 列印內容
     */
    private String content;
    /**
     * x軸
     */
    private int x;
    /**
     * y軸
     */
    private int y;

    public PrintContent() {
    }

    public PrintContent(String content, int x, int y) {
        this.content = content;
        this.x = x;
        this.y = y;
    }

    public PrintContent(String content, int x, int y, Font font) {
        this.content = content;
        this.x = x;
        this.y = y;
        this.font = font;
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
}
