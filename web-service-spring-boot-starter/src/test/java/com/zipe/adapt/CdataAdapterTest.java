package com.zipe.adapt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 驗證 {@link CdataAdapter} 的 CDATA 包裝（marshal）與還原（unmarshal）邏輯。
 */
class CdataAdapterTest {

    private CdataAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CdataAdapter();
    }

    /**
     * 含 {@code <} 或 {@code >} 的字串應被包裹為 CDATA 區段。
     */
    @Test
    void marshalWrapsValueContainingReservedChars() {
        assertEquals("<![CDATA[<b>hi</b>]]>", adapter.marshal("<b>hi</b>"));
        assertEquals("<![CDATA[a>b]]>", adapter.marshal("a>b"));
    }

    /**
     * 不含保留字元的字串應原值回傳；null 回傳 null。
     */
    @Test
    void marshalReturnsValueAsIsWhenNoReservedChars() {
        assertEquals("plain", adapter.marshal("plain"));
        assertNull(adapter.marshal(null));
    }

    /**
     * 含轉義 CDATA 標記的字串應移除包裝並還原角括號。
     */
    @Test
    void unmarshalRestoresEscapedCdata() {
        assertEquals("<b>", adapter.unmarshal("&lt;![CDATA[&lt;b&gt;]]&gt;"));
        assertEquals("hello", adapter.unmarshal("&lt;![CDATA[hello]]&gt;"));
    }

    /**
     * 不含 CDATA 的字串應原值回傳；null 回傳 null。
     */
    @Test
    void unmarshalReturnsValueAsIsWhenNoCdata() {
        assertEquals("plain", adapter.unmarshal("plain"));
        assertNull(adapter.unmarshal(null));
    }
}
