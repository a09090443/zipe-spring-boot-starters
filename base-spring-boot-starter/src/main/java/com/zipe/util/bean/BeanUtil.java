package com.zipe.util.bean;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import com.google.gson.FieldNamingStrategy;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Bean 轉換工具類別，繼承 Spring 的 {@link BeanUtils}，並擴充以下能力：
 * <ul>
 *   <li>物件與 JSON 字串的互轉（透過 Gson 與 Jackson）</li>
 *   <li>List 整批複製（{@link #copyList}）</li>
 *   <li>屬性複製時略過 {@code null} 值（覆寫 {@link #copyProperties}）</li>
 *   <li>物件轉 {@link Map}（{@link #toMap}）</li>
 * </ul>
 *
 * <p>預設的 Gson 實例（{@code builder}）已設定：
 * <ul>
 *   <li>欄位命名策略：首字母轉小寫</li>
 *   <li>{@link Date} 以 Unix timestamp（毫秒）序列化／反序列化</li>
 *   <li>序列化 {@code null} 值</li>
 *   <li>Enum 適配工廠 {@link EnumAdapterFactory}</li>
 * </ul>
 *
 * @author : Gary Tsai
 * @created : @Date 2020/11/20 下午 01:27
 **/

public abstract class BeanUtil extends BeanUtils {
    private static final Logger logger = LoggerFactory.getLogger(BeanUtil.class);

    /** 預設 Gson 建構器，已注冊日期序列化器與 Enum 適配工廠 */
    private static GsonBuilder builder;

    /** Jackson ObjectMapper，供 {@link #toMap} 與 {@link #fromJson(String)} 使用 */
    private static ObjectMapper mapper;

    /** Jackson 自訂模組，注冊日期序列化器與小寫鍵反序列化器 */
    private static SimpleModule module;

    public BeanUtil() {
    }

    /**
     * 將任意物件轉換為 {@code Map<String, Object>}。
     * <p>底層使用 Jackson 的 {@link ObjectMapper#convertValue} 完成轉換。</p>
     *
     * @param <T> 來源物件的型別（僅作為泛型佔位，實際不使用）
     * @param o   要轉換的物件
     * @return 鍵為欄位名稱、值為欄位值的 Map
     */
    public static <T> Map<String, Object> toMap(Object o) {
        return (Map)mapper.convertValue(o, Map.class);
    }

    /**
     * 將來源 List 中的每個元素，以 {@link #copyProperties} 方式複製屬性到目標型別後回傳新 List。
     * <p>若來源 List 為 {@code null} 或空集合，則回傳空 List。</p>
     * <p>複製時會略過值為 {@code null} 的屬性（詳見 {@link #copyProperties(Object, Object)}）。</p>
     *
     * @param <T>   目標元素型別
     * @param froms 來源 List
     * @param clazz 目標型別的 Class 物件，必須提供無參數建構子
     * @return 複製後的新 List；若來源為空則回傳 {@link Collections#emptyList()}
     */
    public static <T> List<T> copyList(List<?> froms, Class<T> clazz) {
        if (froms != null && !froms.isEmpty()) {
            ArrayList tos = new ArrayList();

            try {
                Iterator var3 = froms.iterator();

                while(var3.hasNext()) {
                    Object from = var3.next();
                    // 反射建立目標物件實例，再逐一複製非 null 屬性
                    Object to = clazz.getDeclaredConstructor().newInstance();
                    copyProperties(from, to);
                    tos.add(to);
                }
            } catch (Exception var6) {
                logger.error("The List is not correct.", var6);
            }

            return tos;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * 使用預設的 {@link GsonBuilder} 將物件序列化為 JSON 字串。
     *
     * @param src 要序列化的物件
     * @return JSON 字串
     */
    public static String toJson(Object src) {
        return toJson(src, builder);
    }

    /**
     * 使用指定的 {@link GsonBuilder} 將物件序列化為 JSON 字串。
     *
     * @param src     要序列化的物件
     * @param builder 自訂的 GsonBuilder
     * @return JSON 字串
     */
    public static String toJson(Object src, GsonBuilder builder) {
        return builder.create().toJson(src);
    }

    /**
     * 將 JSON 字串反序列化為 {@code Map<String, Object>}。
     * <p>底層使用 Jackson，Map 的鍵會經由 {@link LowerCaseKeyDeserializer} 轉為小寫。</p>
     *
     * @param json JSON 字串
     * @return 解析後的 Map；若解析失敗則記錄錯誤並回傳 {@code null}
     */
    public static Map<String, Object> fromJson(String json) {
        try {
            return (Map)mapper.readValue(json, Map.class);
        } catch (JacksonException var2) {
            logger.error(var2.getMessage());
            return null;
        }
    }

    /**
     * 使用預設的 {@link GsonBuilder} 將 JSON 字串反序列化為指定型別的物件。
     *
     * @param <T>   目標型別
     * @param json  JSON 字串
     * @param clazz 目標型別的 Class 物件
     * @return 反序列化後的物件
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return fromJson(json, builder, clazz);
    }

    /**
     * 使用指定的 {@link GsonBuilder} 將 JSON 字串反序列化為指定型別的物件。
     *
     * @param <T>     目標型別
     * @param json    JSON 字串
     * @param builder 自訂的 GsonBuilder
     * @param clazz   目標型別的 Class 物件
     * @return 反序列化後的物件
     */
    public static <T> T fromJson(String json, GsonBuilder builder, Class<T> clazz) {
        return builder.create().fromJson(json, clazz);
    }

    /**
     * 使用預設的 {@link GsonBuilder} 將 JSON 陣列字串反序列化為 {@code List<T>}。
     * <p>透過 {@link ParameterTypeWrapper} 傳遞泛型元素型別，以規避 Java 型別抹除的限制。</p>
     *
     * @param <T>   List 元素型別
     * @param json  JSON 陣列字串
     * @param clazz List 元素的 Class 物件
     * @return 反序列化後的 List
     */
    public static <T> List<T> fromJsonToList(String json, Class<?> clazz) {
        return (List)builder.create().fromJson(json, new ParameterTypeWrapper(clazz));
    }

    /**
     * 使用指定的 {@link GsonBuilder} 將 JSON 陣列字串反序列化為 {@code List<T>}。
     *
     * @param <T>     List 元素型別
     * @param json    JSON 陣列字串
     * @param builder 自訂的 GsonBuilder
     * @param clazz   List 元素的 Class 物件
     * @return 反序列化後的 List
     */
    public static <T> List<T> fromJsonToList(String json, GsonBuilder builder, Class<?> clazz) {
        return (List)builder.create().fromJson(json, new ParameterTypeWrapper(clazz));
    }

    /**
     * 複製 {@code source} 的屬性值到 {@code target}，略過值為 {@code null} 的欄位。
     *
     * @param source 來源物件
     * @param target 目標物件
     * @throws BeansException 若屬性複製失敗
     */
    public static void copyProperties(Object source, Object target) throws BeansException {
        copyProperties(source, target, (Class)null, (String[])null);
    }

    /**
     * 複製 {@code source} 的屬性值到 {@code target}，並限制目標物件的可編輯範圍為 {@code editable} 類別所定義的屬性。
     *
     * @param source   來源物件
     * @param target   目標物件
     * @param editable 限制可複製屬性的類別（須為 target 的父類別或本身）
     * @throws BeansException 若屬性複製失敗
     */
    public static void copyProperties(Object source, Object target, Class<?> editable) throws BeansException {
        copyProperties(source, target, editable, (String[])null);
    }

    /**
     * 複製 {@code source} 的屬性值到 {@code target}，並忽略指定名稱的屬性。
     *
     * @param source           來源物件
     * @param target           目標物件
     * @param ignoreProperties 不複製的屬性名稱（可傳入多個）
     * @throws BeansException 若屬性複製失敗
     */
    public static void copyProperties(Object source, Object target, String... ignoreProperties) throws BeansException {
        copyProperties(source, target, (Class)null, ignoreProperties);
    }

    /**
     * 核心屬性複製實作。
     * <p>與 Spring 原生 {@link BeanUtils#copyProperties} 的差異：
     * 當來源屬性值為 {@code null} 時，<strong>不會</strong>覆蓋目標屬性的現有值。</p>
     *
     * @param source           來源物件；若為 {@code null} 則直接返回
     * @param target           目標物件；不可為 {@code null}
     * @param editable         限制可編輯範圍的類別；為 {@code null} 時使用 target 本身的類別
     * @param ignoreProperties 忽略不複製的屬性名稱陣列；為 {@code null} 時不排除任何屬性
     * @throws BeansException 若 target 為 {@code null}，或屬性複製過程中發生錯誤
     */
    private static void copyProperties(Object source, Object target, Class<?> editable, String... ignoreProperties) throws BeansException {
        if (source != null) {
            Assert.notNull(target, "Target must not be null");
            Class<?> actualEditable = target.getClass();
            if (editable != null) {
                if (!editable.isInstance(target)) {
                    throw new IllegalArgumentException("Target class [" + target.getClass().getName() + "] not assignable to Editable class [" + editable.getName() + "]");
                }

                actualEditable = editable;
            }

            PropertyDescriptor[] targetPds = getPropertyDescriptors(actualEditable);
            List<String> ignoreList = ignoreProperties != null ? Arrays.asList(ignoreProperties) : null;
            PropertyDescriptor[] var7 = targetPds;
            int var8 = targetPds.length;

            for(int var9 = 0; var9 < var8; ++var9) {
                PropertyDescriptor targetPd = var7[var9];
                Method writeMethod = targetPd.getWriteMethod();
                // 只處理有 setter 且不在排除清單中的屬性
                if (writeMethod != null && (ignoreList == null || !ignoreList.contains(targetPd.getName()))) {
                    PropertyDescriptor sourcePd = getPropertyDescriptor(source.getClass(), targetPd.getName());
                    if (sourcePd != null) {
                        Method readMethod = sourcePd.getReadMethod();
                        // 確認 getter 存在且來源型別可指派給目標型別（相容性檢查）
                        if (readMethod != null && ClassUtils.isAssignable(writeMethod.getParameterTypes()[0], readMethod.getReturnType())) {
                            try {
                                if (!Modifier.isPublic(readMethod.getDeclaringClass().getModifiers())) {
                                    readMethod.setAccessible(true);
                                }

                                Object value = readMethod.invoke(source);
                                // 略過 null 值，不覆蓋目標物件的現有值（與 Spring 原生行為的關鍵差異）
                                if (value != null) {
                                    if (!Modifier.isPublic(writeMethod.getDeclaringClass().getModifiers())) {
                                        writeMethod.setAccessible(true);
                                    }

                                    writeMethod.invoke(target, value);
                                }
                            } catch (Throwable var15) {
                                throw new FatalBeanException("Could not copy property '" + targetPd.getName() + "' from source to target", var15);
                            }
                        }
                    }
                }
            }

        }
    }

    /**
     * 將字串首字母轉換為小寫。
     * <p>供 Gson 的 {@link FieldNamingStrategy} 使用，確保 JSON 鍵名符合駝峰命名（首字母小寫）。</p>
     *
     * @param value 原始字串
     * @return 首字母小寫後的字串；若為空白字串則回傳空字串
     */
    private static String lowerCaseFirst(String value) {
        if (StringUtils.isBlank(value)) {
            return "";
        } else {
            char[] array = value.toCharArray();
            array[0] = Character.toLowerCase(array[0]);
            return new String(array);
        }
    }

    static {
        // Date 序列化為 Unix timestamp（毫秒），便於前後端統一處理時區問題
        JsonSerializer<Date> serializer = new JsonSerializer<Date>() {
            public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
                return src == null ? null : new JsonPrimitive(src.getTime());
            }
        };
        // Date 反序列化：從 long 型態的毫秒數還原為 Date 物件
        JsonDeserializer<Date> deserializer = new JsonDeserializer<Date>() {
            public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                return json == null ? null : new Date(json.getAsLong());
            }
        };
        // 自訂欄位命名策略：將欄位名稱首字母轉為小寫，對應前端 camelCase 命名慣例
        FieldNamingStrategy customPolicy = new FieldNamingStrategy() {
            public String translateName(Field f) {
                return BeanUtil.lowerCaseFirst(f.getName());
            }
        };
        builder = (new GsonBuilder()).setFieldNamingStrategy(customPolicy).registerTypeAdapter(Date.class, serializer).registerTypeAdapter(Date.class, deserializer).registerTypeAdapterFactory(new EnumAdapterFactory()).serializeNulls();
        module = new SimpleModule();
        module.addSerializer(Date.class, new DateSerializer());
        module.addKeyDeserializer(Object.class, new LowerCaseKeyDeserializer());
        // Jackson 3 的 mapper 為不可變物件，模組須於 builder 階段注冊
        mapper = JsonMapper.builder().addModule(module).build();
    }

    /**
     * 將指定的元素類別包裝為 {@link ParameterizedType}，
     * 用來告知 Gson 反序列化時目標型別為 {@code List<T>}，
     * 以規避 Java 泛型型別抹除（Type Erasure）的限制。
     *
     * @param <T> List 元素型別
     */
    private static class ParameterTypeWrapper<T> implements ParameterizedType {
        /** 實際的泛型元素 Class，作為 List 的型別參數 */
        private Class<?> wrapped;

        ParameterTypeWrapper(Class<?> wrapped) {
            this.wrapped = wrapped;
        }

        /**
         * 回傳泛型實際型別參數陣列，即 {@code [wrapped]}。
         *
         * @return 包含單一元素型別的 Type 陣列
         */
        public Type[] getActualTypeArguments() {
            return new Type[]{this.wrapped};
        }

        /**
         * 回傳原始型別，固定為 {@link List}。
         *
         * @return {@code List.class}
         */
        public Type getRawType() {
            return List.class;
        }

        /**
         * 回傳擁有者型別，此處為頂層類別，故回傳 {@code null}。
         *
         * @return {@code null}
         */
        public Type getOwnerType() {
            return null;
        }
    }
}
