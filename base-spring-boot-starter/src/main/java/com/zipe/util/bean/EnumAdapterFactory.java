package com.zipe.util.bean;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gson 的 Enum 型別轉接工廠。
 * <p>
 * 實作 {@link TypeAdapterFactory}，當 Gson 序列化 Enum 物件時，
 * 自動攔截並委派給 {@link EnumTypeAdapter} 處理，將 Enum 展開為包含
 * {@code value} 欄位及所有 Bean 屬性的 JSON 物件，以利前端取得完整資訊。
 * </p>
 *
 * @author : Gary Tsai
 * @created : @Date 2020/11/20 下午 01:51
 **/

public class EnumAdapterFactory implements TypeAdapterFactory {
    /** 用於記錄序列化過程中發生的例外訊息 */
    private static final Logger logger = LoggerFactory.getLogger(EnumAdapterFactory.class);

    /**
     * 建立 EnumAdapterFactory 實例。
     */
    public EnumAdapterFactory() {
    }

    /**
     * 建立指定型別的 {@link TypeAdapter}。
     * <p>
     * 若目標型別為 Enum，則回傳 {@link EnumTypeAdapter}；否則回傳 {@code null}，
     * 交由 Gson 的其他工廠繼續處理。
     * </p>
     *
     * @param <T>  目標型別的泛型參數
     * @param gson 當前 Gson 實例
     * @param type 目標型別的 {@link TypeToken}
     * @return Enum 型別回傳 {@link EnumTypeAdapter}，否則回傳 {@code null}
     */
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<? super T> rawType = type.getRawType();
        // 僅對 Enum 型別提供自訂轉接器，其他型別交由 Gson 預設機制處理
        return rawType.isEnum() ? new EnumTypeAdapter() : null;
    }

    /**
     * Enum 的自訂 Gson 型別轉接器。
     * <p>
     * 將 Enum 序列化為 JSON 物件，包含 {@code value}（{@code toString()} 結果）
     * 以及透過 Java Bean 反射取得的所有可讀屬性。反序列化（{@code read}）
     * 目前未實作，固定回傳 {@code null}。
     * </p>
     *
     * @param <T> Enum 型別的泛型參數
     */
    public class EnumTypeAdapter<T> extends TypeAdapter<T> {
        /**
         * 建立 EnumTypeAdapter 實例。
         */
        public EnumTypeAdapter() {
        }

        /**
         * 將 Enum 值序列化為 JSON 物件。
         * <p>
         * 輸出格式為包含 {@code value} 欄位（Enum 的 {@code toString()} 結果）
         * 及所有可讀 Bean 屬性的 JSON 物件。若 {@code value} 為 {@code null}
         * 或非 Enum 型別，則輸出 JSON null。
         * </p>
         *
         * @param out   JSON 輸出串流
         * @param value 待序列化的 Enum 值，可為 {@code null}
         * @throws IOException 寫入 JSON 時發生 I/O 錯誤
         */
        public void write(JsonWriter out, T value) throws IOException {
            if (value != null && value.getClass().isEnum()) {
                try {
                    out.beginObject();
                    // 寫入 "value" 欄位，值為 Enum 的 toString() 結果
                    out.name("value");
                    out.value(value.toString());
                    // 透過 Java Bean 內省取得該 Enum 類別的所有屬性描述
                    PropertyDescriptor[] descriptors = Introspector.getBeanInfo(value.getClass()).getPropertyDescriptors();
                    PropertyDescriptor[] var4 = descriptors;
                    int var5 = descriptors.length;

                    for(int var6 = 0; var6 < var5; ++var6) {
                        PropertyDescriptor descriptor = var4[var6];
                        if (this.isReadMethod(descriptor)) {
                            try {
                                // 將每個可讀屬性名稱與其值寫入 JSON
                                out.name(descriptor.getName());
                                out.value(String.valueOf(descriptor.getReadMethod().invoke(value)));
                            } catch (Exception var9) {
                                logger.error(var9.getMessage());
                            }
                        }
                    }

                    out.endObject();
                } catch (Exception var10) {
                    logger.error(var10.getMessage());
                }

            } else {
                out.nullValue();
            }
        }

        /**
         * 從 JSON 反序列化為 Enum 值。
         * <p>
         * 目前未實作，固定回傳 {@code null}。
         * </p>
         *
         * @param in JSON 輸入串流
         * @return 固定回傳 {@code null}
         * @throws IOException 讀取 JSON 時發生 I/O 錯誤
         */
        public T read(JsonReader in) throws IOException {
            return null;
        }

        /**
         * 判斷指定的 {@link PropertyDescriptor} 是否為有效的可讀屬性。
         * <p>
         * 排除 {@code class} 與 {@code declaringClass} 這兩個 Java 物件內建屬性，
         * 避免將非業務用途的中繼資料寫入 JSON。
         * </p>
         *
         * @param descriptor 屬性描述子
         * @return 若具有 getter 且非排除屬性則回傳 {@code true}，否則回傳 {@code false}
         */
        private boolean isReadMethod(PropertyDescriptor descriptor) {
            // 排除 "class" 與 "declaringClass"，這兩個屬性為 Java 反射內建，非 Enum 業務欄位
            return descriptor.getReadMethod() != null && !"class".equals(descriptor.getName()) && !"declaringClass".equals(descriptor.getName());
        }
    }
}
