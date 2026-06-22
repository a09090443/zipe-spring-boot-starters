package com.zipe.util.bean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 驗證 {@link BeanUtil} 的屬性複製（略過 null）、List 複製與 JSON 互轉行為。
 */
class BeanUtilTest {

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
     * copyProperties 應略過來源的 null 欄位，不覆蓋目標既有值（與 Spring 原生行為的差異）。
     */
    @Test
    void copyPropertiesSkipsNullSourceValues() {
        Person source = new Person("Gary", null);
        Person target = new Person(null, 30);

        BeanUtil.copyProperties(source, target);

        assertEquals("Gary", target.getName());
        assertEquals(30, target.getAge());
    }

    /**
     * copyList 應逐一複製為目標型別並保留欄位值；空來源回傳空 List。
     */
    @Test
    void copyListCopiesEachElement() {
        List<Person> sources = List.of(new Person("A", 1), new Person("B", 2));
        List<Person> result = BeanUtil.copyList(sources, Person.class);

        assertEquals(2, result.size());
        assertEquals("A", result.get(0).getName());
        assertEquals(2, result.get(1).getAge());
        assertEquals(List.of(), BeanUtil.copyList(List.of(), Person.class));
    }

    /**
     * toJson 與 fromJson(String, Class) 應能往返還原物件。
     */
    @Test
    void toJsonAndFromJsonRoundTrip() {
        Person origin = new Person("Gary", 30);
        String json = BeanUtil.toJson(origin);
        Person parsed = BeanUtil.fromJson(json, Person.class);

        assertEquals("Gary", parsed.getName());
        assertEquals(30, parsed.getAge());
    }

    /**
     * fromJsonToList 應正確反序列化 JSON 陣列為 List。
     */
    @Test
    void fromJsonToListDeserializesArray() {
        List<Person> result = BeanUtil.fromJsonToList("[{\"name\":\"A\",\"age\":1},{\"name\":\"B\",\"age\":2}]", Person.class);

        assertEquals(2, result.size());
        assertEquals("A", result.get(0).getName());
        assertEquals(2, result.get(1).getAge());
    }

    /**
     * fromJson(String) 應回傳 Map，且解析失敗時回傳 null。
     */
    @Test
    void fromJsonReturnsMapAndNullOnFailure() {
        Map<String, Object> map = BeanUtil.fromJson("{\"name\":\"Gary\",\"age\":30}");
        assertEquals("Gary", map.get("name"));

        assertNull(BeanUtil.fromJson("not-a-json"));
    }

    /**
     * toMap 應將物件欄位轉為 Map 鍵值。
     */
    @Test
    void toMapConvertsObjectFields() {
        Map<String, Object> map = BeanUtil.toMap(new Person("Gary", 30));
        assertEquals("Gary", map.get("name"));
        assertEquals(30, map.get("age"));
    }
}
