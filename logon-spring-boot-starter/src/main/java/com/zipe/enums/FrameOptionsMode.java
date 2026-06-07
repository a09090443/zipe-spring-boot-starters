package com.zipe.enums;

/**
 * X-Frame-Options（點擊劫持防護）模式。
 *
 * <ul>
 *   <li>{@link #SAMEORIGIN}：僅允許同源頁面以 iframe 內嵌（安全預設）。</li>
 *   <li>{@link #DENY}：禁止任何頁面以 iframe 內嵌。</li>
 *   <li>{@link #DISABLE}：完全停用此標頭（不建議，會失去點擊劫持防護）。</li>
 * </ul>
 */
public enum FrameOptionsMode {
    SAMEORIGIN,
    DENY,
    DISABLE
}
