package com.zipe.adapt;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

/**
 * @author zipe1
 * @created 2024/10/29
 */
public class CdataAdapter extends XmlAdapter<String, String> {

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

    @Override
    public String marshal(String v) {
        if (v != null && (v.contains("<") || v.contains(">"))) {
            return "<![CDATA[" + v + "]]>";
        }
        return v;
    }
}
