package com.zipe.base.annotation;

import java.lang.annotation.Annotation;

/**
 * {@link DS} 註解的執行期動態實作。
 *
 * <p>Java 的 Annotation 本質上是介面，無法在執行期直接以 {@code new} 建立實例。
 * 當程式需要以程式碼方式（而非宣告式）指定資料來源名稱時（例如在 AOP 切面中
 * 透過反射取得或動態組裝 {@link DS}），可使用此類別包裝目標資料來源名稱，
 * 使其具備與 {@code @DS} 相同的語義，可直接傳遞給需要 {@link DS} 型別的方法。</p>
 */
public class DynamicDS implements DS {

  /** 資料來源名稱，對應 {@link DS#value()} 所指定的值。 */
  private String value;

  /**
   * 以指定的資料來源名稱建立 {@link DynamicDS} 實例。
   *
   * @param value 資料來源名稱，需與 {@code application.yml} 中配置的資料來源鍵一致
   */
  public DynamicDS(String value) {
    this.value = value;
  }

  /**
   * 回傳此實例所代表的資料來源名稱。
   *
   * @return 資料來源名稱
   */
  @Override
  public String value() {
    return value;
  }

  /**
   * 回傳此物件所實作的註解型別。
   *
   * <p>實作 {@link Annotation#annotationType()} 的合約，固定回傳 {@link DynamicDS} 本身的
   * {@link Class} 物件，以滿足反射機制對 Annotation 實例的型別識別需求。</p>
   *
   * @return {@code DynamicDS.class}
   */
  @Override
  public Class<? extends Annotation> annotationType() {
    return DynamicDS.class;
  }

}
