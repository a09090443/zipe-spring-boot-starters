package com.zipe.util.doc;

/**
 * @author : Gary Tsai
 * @created : @Date 2020/11/23 下午 05:20
 **/
import java.lang.reflect.Field;

public class FieldForSortting {
    private Field field;
    private int index;

    public FieldForSortting(Field field) {
        this.field = field;
    }

    public FieldForSortting(Field field, int index) {
        this.field = field;
        this.index = index;
    }

    public Field getField() {
        return this.field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
