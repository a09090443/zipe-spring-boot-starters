package com.zipe.util.doc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The <code>ExcelCell</code><br>
 * 數值型的欄位只能使用Double
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

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Valid {
        /**
         * 必須與in中String相符,目前僅支持String類型
         *
         * @return e.g. {"key","value"}
         */

        String[] in() default {};

        /**
         * 是否允許為空,用於驗證數據 default true
         *
         * @return allowNull
         */
        boolean allowNull() default true;

        /**
         * Apply a "greater than" constraint to the named property
         *
         * @return gt
         */
        double gt() default Double.NaN;

        /**
         * Apply a "less than" constraint to the named property
         *
         * @return lt
         */
        double lt() default Double.NaN;

        /**
         * Apply a "greater than or equal" constraint to the named property
         *
         * @return ge
         */
        double ge() default Double.NaN;

        /**
         * Apply a "less than or equal" constraint to the named property
         *
         * @return le
         */
        double le() default Double.NaN;
    }
}
