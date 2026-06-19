package com.zipe.adapt;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

/**
 * CDATA XML 轉換器，用於在 JAXB 序列化／反序列化時處理 CDATA 區段。
 *
 * <p>當 XML 欄位值包含 HTML 標籤或特殊字元（如 {@code <}、{@code >}）時，
 * marshal 會自動將其包裹為 {@code <![CDATA[...]]>} 區段，
 * unmarshal 則負責將已轉義的 CDATA 字串還原為原始內容。</p>
 *
 * <p>使用方式：在 JAXB 欄位上加入 {@code @XmlJavaTypeAdapter(CdataAdapter.class)}。</p>
 *
 * @author zipe1
 * @created 2024/10/29
 */
public class CdataAdapter extends XmlAdapter<String, String> {

    /**
     * 將 XML 字串反序列化為 Java 字串。
     *
     * <p>若傳入值含有已轉義的 CDATA 標記（如 {@code &lt;![CDATA[...]]&gt;}），
     * 則先移除 CDATA 包裝，再將 HTML 實體字元（{@code &lt;}、{@code &gt;}）
     * 還原為原始的角括號，確保取得乾淨的字串值。</p>
     *
     * @param v 來自 XML 的原始字串，可為 {@code null}
     * @return 移除 CDATA 包裝並還原轉義字元後的字串；若 {@code v} 為 {@code null} 或不含 CDATA，則原值回傳
     */
    @Override
    public String unmarshal(String v) {
        if (v != null && v.contains("CDATA")) {
            // 移除 CDATA 包裝和轉義
            return v.replaceAll("&lt;!\\[CDATA\\[(.*)\\]\\]&gt;", "$1")
                    .replaceAll("&lt;", "<")
                    .replaceAll("&gt;", ">");
        }
        return v;
    }

    /**
     * 將 Java 字串序列化為 XML 字串。
     *
     * <p>若字串包含 {@code <} 或 {@code >} 等 XML 保留字元，
     * 則以 {@code <![CDATA[...]]>} 包裹，避免序列化時產生格式錯誤；
     * 否則直接回傳原值。</p>
     *
     * @param v 待序列化的 Java 字串，可為 {@code null}
     * @return 包裹 CDATA 區段的字串；若 {@code v} 為 {@code null} 或不含保留字元，則原值回傳
     */
    @Override
    public String marshal(String v) {
        if (v != null && (v.contains("<") || v.contains(">"))) {
            return "<![CDATA[" + v + "]]>";
        }
        return v;
    }
}
