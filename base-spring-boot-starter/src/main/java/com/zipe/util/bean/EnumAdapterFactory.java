package com.zipe.util.bean;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;

/**
 * @author : Gary Tsai
 * @created : @Date 2020/11/20 下午 01:51
 **/

public class EnumAdapterFactory implements TypeAdapterFactory {
    private static final Logger logger = LoggerFactory.getLogger(EnumAdapterFactory.class);

    public EnumAdapterFactory() {
    }

    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<? super T> rawType = type.getRawType();
        return rawType.isEnum() ? new EnumTypeAdapter() : null;
    }

    public class EnumTypeAdapter<T> extends TypeAdapter<T> {
        public EnumTypeAdapter() {
        }

        public void write(JsonWriter out, T value) throws IOException {
            if (value != null && value.getClass().isEnum()) {
                try {
                    out.beginObject();
                    out.name("value");
                    out.value(value.toString());
                    PropertyDescriptor[] descriptors = Introspector.getBeanInfo(value.getClass()).getPropertyDescriptors();
                    PropertyDescriptor[] var4 = descriptors;
                    int var5 = descriptors.length;

                    for(int var6 = 0; var6 < var5; ++var6) {
                        PropertyDescriptor descriptor = var4[var6];
                        if (this.isReadMethod(descriptor)) {
                            try {
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

        public T read(JsonReader in) throws IOException {
            return null;
        }

        private boolean isReadMethod(PropertyDescriptor descriptor) {
            return descriptor.getReadMethod() != null && !"class".equals(descriptor.getName()) && !"declaringClass".equals(descriptor.getName());
        }
    }
}
