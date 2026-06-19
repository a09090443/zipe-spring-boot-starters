package com.zipe.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Map 相關的輔助工具類別，提供標準 Java Collection API 未涵蓋或不支援 {@code null} 鍵的進階操作。
 *
 * @author : Gary Tsai
 * @created : @Date 2020/11/12 下午 01:33
 **/
public class MapUtils {

    /**
     * 將 map key 值使用 groupingBy 的方法分類，並允許 {@code null} 值作為分組鍵。
     *
     * <p>{@link Collectors#groupingBy(Function)} 在分類函式回傳 {@code null} 時會拋出
     * {@link NullPointerException}，此方法改用 {@link Collectors#toMap} 搭配合併函式，
     * 讓 {@code null} 也能作為合法的分組鍵。</p>
     *
     * @param classifier 分類函式，將串流元素映射至分組鍵（允許回傳 {@code null}）
     * @param <T>        串流元素型別
     * @param <A>        分組鍵型別
     * @return 可用於 {@code Stream.collect()} 的 {@link Collector}，
     *         收集結果為 {@code Map<A, List<T>>}，同一鍵下的元素會聚合至同一 {@link List}
     */
    public static <T, A> Collector<T, ?, Map<A, List<T>>> groupingBy_WithNullKeys(Function<? super T, ? extends A> classifier) {
        return Collectors.toMap(
                classifier,
                // 將單一元素包裝為不可變的單元素 List，作為 Map 中的初始值
                Collections::singletonList,
                (List<T> oldList, List<T> newEl) -> {
                    // 當同一鍵已存在時，合併舊串列與新元素，避免覆蓋既有資料
                    List<T> newList = new ArrayList<>(oldList.size() + 1);
                    newList.addAll(oldList);
                    newList.addAll(newEl);
                    return newList;
                });
    }

}
