package com.zipe.base.config;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;

import java.time.LocalDateTime;

public class P6SpyLogger implements MessageFormattingStrategy {
    /**
     * 日誌格式
     *
     * @param connectionId 連線id
     * @param now          當前時間
     * @param elapsed      耗時多久
     * @param category     類別
     * @param prepared     mybatis帶佔位符的sql
     * @param sql          佔位符換成引數的sql
     * @param url          sql連線的 url
     * @return 自定義格式日誌
     */
    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared, String sql, String url) {
        return !"".equals(sql.trim()) ? "P6SpyLogger " + LocalDateTime.now() + " | elapsed " + elapsed + "ms | category " + category + " | connection " + connectionId + " | url " + url + " | sql \n" + sql : "";
    }
}
