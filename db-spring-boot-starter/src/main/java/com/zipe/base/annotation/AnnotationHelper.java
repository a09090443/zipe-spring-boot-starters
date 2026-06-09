package com.zipe.base.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 執行期動態修改 Annotation 的輔助工具類別。
 *
 * <p>利用反射（Reflection）繞過 Java 對 Annotation 的唯讀限制，
 * 直接操作 JVM 內部的 Annotation 快取 Map，以達到在執行期替換
 * 指定類別上某個 Annotation 實例的目的。</p>
 *
 * <p>由於 JDK 7 與 JDK 8+ 的 {@link Class} 內部結構不同，本工具類別
 * 會先偵測 JDK 版本，再分別採用對應的反射路徑進行操作。</p>
 *
 * <p><strong>注意：</strong>此操作屬於非官方 API 的私有欄位存取，
 * 可能在未來的 JDK 版本中失效，使用時須謹慎評估相容性風險。</p>
 */
public class AnnotationHelper {

  /** JDK 7 及以下版本中，{@link Class} 直接持有 Annotation 的欄位名稱。 */
  private static final String ANNOTATIONS = "annotations";

  /** JDK 8+ 中，{@link Class} 持有 AnnotationData 物件的私有方法名稱。 */
  public static final String ANNOTATION_DATA = "annotationData";

  /**
   * 偵測目前 JVM 執行環境是否為 JDK 7 或更舊版本。
   *
   * <p>偵測方式：嘗試在 {@link Class} 中尋找名為 {@code "annotations"} 的欄位。
   * JDK 7 以下存在此欄位；JDK 8+ 已將 Annotation 資料移至私有的
   * {@code AnnotationData} 物件中，因此該欄位不存在。</p>
   *
   * @return 若為 JDK 7 或更舊版本則回傳 {@code true}，否則回傳 {@code false}
   */
  public static boolean isJDK7OrLower() {
    boolean jdk7OrLower = true;
    try {
      Class.class.getDeclaredField(ANNOTATIONS);
    } catch (NoSuchFieldException e) {
      //Willfully ignore all exceptions
      jdk7OrLower = false;
    }
    return jdk7OrLower;
  }

  /**
   * 在執行期動態替換指定類別上的 Annotation 實例。
   *
   * <p>根據 JDK 版本選擇不同的反射路徑：</p>
   * <ul>
   *   <li><strong>JDK 7 以下：</strong>直接存取 {@link Class} 的
   *       {@code annotations} 欄位（型別為 {@code Map}），並以新的 Annotation
   *       實例覆蓋原有值。</li>
   *   <li><strong>JDK 8+：</strong>先呼叫 {@link Class} 的私有方法
   *       {@code annotationData()} 取得 {@code AnnotationData} 物件，
   *       再存取其內部的 {@code annotations} Map 進行替換。</li>
   * </ul>
   *
   * @param clazzToLookFor     要修改 Annotation 的目標類別
   * @param annotationToAlter  欲替換的 Annotation 型別
   * @param annotationValue    用來替換的新 Annotation 實例
   */
  public static void alterAnnotationOn(Class clazzToLookFor,
      Class<? extends Annotation> annotationToAlter, Annotation annotationValue) {
    if (isJDK7OrLower()) {
      try {
        // JDK 7 以下：Class 直接以 Map 欄位保存所有 Annotation，可直接取得並修改
        Field annotations = Class.class.getDeclaredField(ANNOTATIONS);
        annotations.setAccessible(true);
        Map<Class<? extends Annotation>, Annotation> map =
            (Map<Class<? extends Annotation>, Annotation>) annotations.get(clazzToLookFor);
        map.put(annotationToAlter, annotationValue);
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      try {
        //In JDK8 Class has a private method called annotationData().
        //We first need to invoke it to obtain a reference to AnnotationData class which is a private class
        Method method = Class.class.getDeclaredMethod(ANNOTATION_DATA, null);
        method.setAccessible(true);
        //Since AnnotationData is a private class we cannot create a direct reference to it. We will have to
        //manage with just Object
        // 以 Object 接收回傳值，因為 AnnotationData 是私有類別無法直接宣告型別
        Object annotationData = method.invoke(clazzToLookFor);
        //We now look for the map called "annotations" within AnnotationData object.
        Field annotations = annotationData.getClass().getDeclaredField(ANNOTATIONS);
        annotations.setAccessible(true);
        Map<Class<? extends Annotation>, Annotation> map =
            (Map<Class<? extends Annotation>, Annotation>) annotations.get(annotationData);
        // 以新的 Annotation 實例取代 Map 中對應型別的舊值，完成動態替換
        map.put(annotationToAlter, annotationValue);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}