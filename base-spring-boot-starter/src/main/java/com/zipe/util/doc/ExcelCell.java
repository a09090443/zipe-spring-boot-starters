package com.zipe.util.doc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 標記 Excel 欄位對應關係的自訂 Annotation。
 * <p>
 * 套用於 Java 欄位（Field），用於告知 {@code ExcelUtil} 該欄位在 Excel 中的欄位順序、
 * 空值預設顯示值，以及資料驗證規則。
 * </p>
 * <p>
 * 注意：數值型的欄位只能使用 {@code Double}，不支援其他數值型別。
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ExcelCell {
    /**
     * 順序 default 100
     *
     * @return index
     */
    int index();

    /**
     * 當值為null時要顯示的值 default StringUtils.EMPTY
     *
     * @return defaultValue
     */
    String defaultValue() default "";

    /**
     * 用於驗證
     *
     * @return valid
     */
    Valid valid() default @Valid();

    /**
     * 內嵌的欄位資料驗證規則 Annotation。
     * <p>
     * 可透過 {@code @ExcelCell} 的 {@code valid()} 屬性套用，
     * 用於驗證匯入的 Excel 欄位值是否符合允許的範圍或數值限制。
     * </p>
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Valid {
        /**
         * 允許的字串值清單；匯入值必須與清單中的某個字串相符。
         * 目前僅支援 {@code String} 型別的欄位。
         *
         * @return e.g. {"key","value"}
         */

        String[] in() default {};

        /**
         * 是否允許欄位值為空，用於驗證資料。預設為 {@code true}（允許為空）。
         *
         * @return allowNull
         */
        boolean allowNull() default true;

        /**
         * 大於（Greater Than）數值限制；欄位值必須嚴格大於此值。
         * 未設定時預設為 {@code Double.NaN}（表示不套用此限制）。
         *
         * @return gt
         */
        double gt() default Double.NaN;

        /**
         * 小於（Less Than）數值限制；欄位值必須嚴格小於此值。
         * 未設定時預設為 {@code Double.NaN}（表示不套用此限制）。
         *
         * @return lt
         */
        double lt() default Double.NaN;

        /**
         * 大於或等於（Greater Than or Equal）數值限制；欄位值必須大於或等於此值。
         * 未設定時預設為 {@code Double.NaN}（表示不套用此限制）。
         *
         * @return ge
         */
        double ge() default Double.NaN;

        /**
         * 小於或等於（Less Than or Equal）數值限制；欄位值必須小於或等於此值。
         * 未設定時預設為 {@code Double.NaN}（表示不套用此限制）。
         *
         * @return le
         */
        double le() default Double.NaN;
    }
}
