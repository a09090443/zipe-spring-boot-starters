package com.zipe.enums;

/**
 * @author gary.tsai 2019/5/31
 */
public enum UserEnum {

    SYSTEM("system", ""),
    ADMIN("admin", "ADM");

    private final String name;
    private final String symbol;

    UserEnum(String name, String symbol) {
        this.name = name;
        this.symbol = symbol;
    }

    /**
     * 使用者群組文字
     *
     * @return
     */
    public String wording() {
        return this.name;
    }

    /**
     * 使用者群組代號
     *
     * @return
     */
    public String symbol() {
        return this.symbol;
    }

}
