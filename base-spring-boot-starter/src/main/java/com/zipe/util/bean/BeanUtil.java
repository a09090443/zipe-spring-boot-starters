package com.zipe.util.bean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
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
import java.io.IOException;
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
 * @author : Gary Tsai
 * @created : @Date 2020/11/20 下午 01:27
 **/

public abstract class BeanUtil extends BeanUtils {
    private static final Logger logger = LoggerFactory.getLogger(BeanUtil.class);
    private static GsonBuilder builder;
    private static ObjectMapper mapper;
    private static SimpleModule module;

    public BeanUtil() {
    }

    public static <T> Map<String, Object> toMap(Object o) {
        return (Map)mapper.convertValue(o, Map.class);
    }

    public static <T> List<T> copyList(List<?> froms, Class<T> clazz) {
        if (froms != null && !froms.isEmpty()) {
            ArrayList tos = new ArrayList();

            try {
                Iterator var3 = froms.iterator();

                while(var3.hasNext()) {
                    Object from = var3.next();
                    Object to = clazz.newInstance();
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

    public static String toJson(Object src) {
        return toJson(src, builder);
    }

    public static String toJson(Object src, GsonBuilder builder) {
        return builder.create().toJson(src);
    }

    public static Map<String, Object> fromJson(String json) {
        try {
            return (Map)mapper.readValue(json, Map.class);
        } catch (IOException var2) {
            logger.error(var2.getMessage());
            return null;
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return fromJson(json, builder, clazz);
    }

    public static <T> T fromJson(String json, GsonBuilder builder, Class<T> clazz) {
        return builder.create().fromJson(json, clazz);
    }

    public static <T> List<T> fromJsonToList(String json, Class<?> clazz) {
        return (List)builder.create().fromJson(json, new ParameterTypeWrapper(clazz));
    }

    public static <T> List<T> fromJsonToList(String json, GsonBuilder builder, Class<?> clazz) {
        return (List)builder.create().fromJson(json, new ParameterTypeWrapper(clazz));
    }

    public static void copyProperties(Object source, Object target) throws BeansException {
        copyProperties(source, target, (Class)null, (String[])null);
    }

    public static void copyProperties(Object source, Object target, Class<?> editable) throws BeansException {
        copyProperties(source, target, editable, (String[])null);
    }

    public static void copyProperties(Object source, Object target, String... ignoreProperties) throws BeansException {
        copyProperties(source, target, (Class)null, ignoreProperties);
    }

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
                if (writeMethod != null && (ignoreList == null || !ignoreList.contains(targetPd.getName()))) {
                    PropertyDescriptor sourcePd = getPropertyDescriptor(source.getClass(), targetPd.getName());
                    if (sourcePd != null) {
                        Method readMethod = sourcePd.getReadMethod();
                        if (readMethod != null && ClassUtils.isAssignable(writeMethod.getParameterTypes()[0], readMethod.getReturnType())) {
                            try {
                                if (!Modifier.isPublic(readMethod.getDeclaringClass().getModifiers())) {
                                    readMethod.setAccessible(true);
                                }

                                Object value = readMethod.invoke(source);
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
        JsonSerializer<Date> serializer = new JsonSerializer<Date>() {
            public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
                return src == null ? null : new JsonPrimitive(src.getTime());
            }
        };
        JsonDeserializer<Date> deserializer = new JsonDeserializer<Date>() {
            public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                return json == null ? null : new Date(json.getAsLong());
            }
        };
        FieldNamingStrategy customPolicy = new FieldNamingStrategy() {
            public String translateName(Field f) {
                return BeanUtil.lowerCaseFirst(f.getName());
            }
        };
        builder = (new GsonBuilder()).setFieldNamingStrategy(customPolicy).registerTypeAdapter(Date.class, serializer).registerTypeAdapter(Date.class, deserializer).registerTypeAdapterFactory(new EnumAdapterFactory()).serializeNulls();
        module = new SimpleModule();
        module.addSerializer(Date.class, new DateSerializer());
        module.addKeyDeserializer(Object.class, new LowerCaseKeyDeserializer());
        mapper = new ObjectMapper();
        mapper.registerModule(module);
    }

    private static class ParameterTypeWrapper<T> implements ParameterizedType {
        private Class<?> wrapped;

        ParameterTypeWrapper(Class<?> wrapped) {
            this.wrapped = wrapped;
        }

        public Type[] getActualTypeArguments() {
            return new Type[]{this.wrapped};
        }

        public Type getRawType() {
            return List.class;
        }

        public Type getOwnerType() {
            return null;
        }
    }
}

