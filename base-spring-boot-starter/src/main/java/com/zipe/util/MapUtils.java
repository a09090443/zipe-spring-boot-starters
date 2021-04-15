package com.zipe.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * @author : Gary Tsai
 * @created : @Date 2020/11/12 下午 01:33
 **/
public class MapUtils {

    /**
     * 將 map key 值使用 groupingBy 的方法分類，並允許 null 值
     *
     * @param classifier
     * @param <T>
     * @param <A>
     * @return
     */
    public static <T, A> Collector<T, ?, Map<A, List<T>>> groupingBy_WithNullKeys(Function<? super T, ? extends A> classifier) {
        return Collectors.toMap(
                classifier,
                Collections::singletonList,
                (List<T> oldList, List<T> newEl) -> {
                    List<T> newList = new ArrayList<>(oldList.size() + 1);
                    newList.addAll(oldList);
                    newList.addAll(newEl);
                    return newList;
                });
    }

}
