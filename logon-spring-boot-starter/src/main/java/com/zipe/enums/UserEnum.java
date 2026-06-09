package com.zipe.enums;

/**
 * 系統內建使用者群組列舉。
 *
 * <p>定義系統保留的特殊使用者群組，每個列舉值對應一個群組名稱（{@code name}）
 * 與識別代號（{@code symbol}），可於登入驗證、權限判斷等流程中使用。</p>
 *
 * @author gary.tsai 2019/5/31
 */
public enum UserEnum {

    /** 系統級使用者，代號為空字串，通常用於系統內部背景作業。 */
    SYSTEM("system", ""),
    /** 管理員使用者，代號為 "ADM"，具有最高管理權限。 */
    ADMIN("admin", "ADM");

    /** 群組的顯示名稱（英文小寫）。 */
    private final String name;
    /** 群組的識別代號，用於權限判斷或角色對應。 */
    private final String symbol;

    /**
     * 建構 UserEnum 列舉常數。
     *
     * @param name   群組顯示名稱
     * @param symbol 群組識別代號
     */
    UserEnum(String name, String symbol) {
        this.name = name;
        this.symbol = symbol;
    }

    /**
     * 使用者群組文字
     *
     * @return 群組的顯示名稱（英文小寫字串，例如 {@code "admin"}）
     */
    public String wording() {
        return this.name;
    }

    /**
     * 使用者群組代號
     *
     * @return 群組的識別代號（例如 {@code "ADM"}），可用於權限判斷或角色對應
     */
    public String symbol() {
        return this.symbol;
    }

}
