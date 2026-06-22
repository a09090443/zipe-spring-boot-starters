package com.zipe.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 驗證 {@link XmlUtil} 物件與 XML 的序列化往返、未知屬性容錯與錯誤包裝。
 */
class XmlUtilTest {

    /**
     * 供測試使用的簡單 POJO，提供無參數建構子與標準 getter/setter。
     */
    public static class Person {
        private String name;
        private Integer age;

        public Person() {
        }

        public Person(String name, Integer age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }
    }

    /**
     * beanToXml 輸出應包含欄位名稱，且 xmlToBean 能完整還原物件。
     */
    @Test
    void beanToXmlAndBackRoundTrips() {
        String xml = XmlUtil.beanToXml(new Person("Gary", 30));
        assertTrue(xml.contains("name"));
        assertTrue(xml.contains("Gary"));

        Person parsed = XmlUtil.xmlToBean(xml, Person.class);
        assertEquals("Gary", parsed.getName());
        assertEquals(30, parsed.getAge());
    }

    /**
     * 含未知欄位的 XML 反序列化時不應拋例外（FAIL_ON_UNKNOWN_PROPERTIES=false 生效）。
     */
    @Test
    void xmlToBeanIgnoresUnknownProperties() {
        String xml = XmlUtil.beanToXml(new Person("Gary", 30));
        int idx = xml.lastIndexOf("</");
        String withUnknown = xml.substring(0, idx) + "<unknownField>x</unknownField>" + xml.substring(idx);

        Person parsed = XmlUtil.xmlToBean(withUnknown, Person.class);
        assertEquals("Gary", parsed.getName());
    }

    /**
     * 不合法的 XML 應被包裝為 RuntimeException 拋出。
     */
    @Test
    void xmlToBeanWrapsInvalidXmlAsRuntimeException() {
        assertThrows(RuntimeException.class, () -> XmlUtil.xmlToBean("<broken", Person.class));
    }
}
