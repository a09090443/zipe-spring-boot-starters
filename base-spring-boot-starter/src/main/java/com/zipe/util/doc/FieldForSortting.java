package com.zipe.util.doc;

/**
 * 用於 Excel 欄位排序的封裝類別。
 *
 * <p>在透過反射讀取 Java 物件欄位並輸出至 Excel 時，
 * 此類別同時持有欄位的 {@link Field} 物件與其對應的欄位順序索引（index），
 * 方便排序後依序寫入各欄。</p>
 *
 * @author : Gary Tsai
 * @created : @Date 2020/11/23 下午 05:20
 **/
import java.lang.reflect.Field;

public class FieldForSortting {
    /** 對應 Java 物件的反射欄位 */
    private Field field;
    /** 欄位在 Excel 中的輸出順序索引，數值越小越靠前 */
    private int index;

    /**
     * 以欄位建立排序封裝物件，索引預設為 0。
     *
     * @param field 反射取得的 Java 欄位
     */
    public FieldForSortting(Field field) {
        this.field = field;
    }

    /**
     * 以欄位與自訂索引建立排序封裝物件。
     *
     * @param field 反射取得的 Java 欄位
     * @param index 欄位在 Excel 中的輸出順序索引
     */
    public FieldForSortting(Field field, int index) {
        this.field = field;
        this.index = index;
    }

    /**
     * 取得反射欄位。
     *
     * @return 封裝的 {@link Field} 物件
     */
    public Field getField() {
        return this.field;
    }

    /**
     * 設定反射欄位。
     *
     * @param field 要設定的 {@link Field} 物件
     */
    public void setField(Field field) {
        this.field = field;
    }

    /**
     * 取得欄位的輸出順序索引。
     *
     * @return 排序索引值
     */
    public int getIndex() {
        return this.index;
    }

    /**
     * 設定欄位的輸出順序索引。
     *
     * @param index 排序索引值
     */
    public void setIndex(int index) {
        this.index = index;
    }
}
