package com.zipe.base.aspect;

import com.zipe.base.annotation.DS;
import com.zipe.base.database.DataSourceHolder;
import java.util.Objects;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 動態資料來源 AOP 切面。
 *
 * <p>攔截標註 {@link com.zipe.base.annotation.DS} 的類別或方法，
 * 在目標方法執行前將對應的資料來源名稱寫入 {@link DataSourceHolder}，
 * 以驅動 {@link com.zipe.base.database.DynamicDataSource} 切換至正確的資料來源。</p>
 *
 * <p>透過 {@link Order}(-1) 確保本切面在 {@code @Transactional} 切面之前執行，
 * 避免在事務已開啟後才切換資料來源導致切換無效。</p>
 *
 * @author : Gary Tsai
 * @created : @Date 2021/3/18 下午 05:14
 **/
@Aspect
@Component
@Order(-1)// 保證該AOP在@Transactional之前執行
public class DynamicDataSourceAspect {

    /**
     * 切入點定義：攔截類別層級或方法層級標注了 {@link com.zipe.base.annotation.DS} 的所有方法。
     */
    @Pointcut("@within(com.zipe.base.annotation.DS) || @annotation(com.zipe.base.annotation.DS)")
    public void dataSourcePointCut(){}

    /**
     * 環繞通知：在目標方法執行前設定資料來源，執行後於 {@code finally} 區塊還原為外層原值。
     *
     * <p>進入方法時先保存當前執行緒既有的資料來源名稱（外層值），再切換為 {@code @DS} 指定的目標；
     * 方法結束後一律還原為外層值，若外層原本未設定（最外層呼叫）則清除 ThreadLocal。
     * 此「保存 / 還原」機制同時解決兩個問題：</p>
     * <ul>
     *   <li>執行緒池環境（Tomcat/Undertow）下執行緒被後續請求重用時，殘留設定導致的錯誤路由；</li>
     *   <li>巢狀 {@code @DS} 呼叫返回後，外層方法剩餘的資料庫操作被內層資料來源污染的問題。</li>
     * </ul>
     *
     * @param joinPoint 正在執行的連接點，用於取得目標方法及其所屬類別的資訊
     * @return 目標方法的實際回傳值
     * @throws Throwable 目標方法拋出的任何例外，原樣向上傳遞
     */
    @Around("dataSourcePointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 保存外層既有的資料來源名稱，供方法結束後還原（支援巢狀 @DS 呼叫）
        String previousDsKey = DataSourceHolder.getDataSourceName();
        // 取得 @DS 指定的資料來源索引鍵，並設定至當前執行緒的 ThreadLocal 中
        String dsKey = getDSAnnotation(joinPoint).value();
        DataSourceHolder.setDataSourceName(dsKey);
        try {
            return joinPoint.proceed();
        } finally {
            // 還原為外層原值；若外層未設定（最外層呼叫）則清除 ThreadLocal，避免執行緒池殘留
            if (Objects.nonNull(previousDsKey)) {
                DataSourceHolder.setDataSourceName(previousDsKey);
            } else {
                DataSourceHolder.clearDataSourceName();
            }
        }
    }

    /**
     * 根據類別或方法取得 {@link DS} 資料來源註解。
     *
     * <p>優先採用類別層級的 {@code @DS} 宣告；若類別層級未標注，
     * 則退而取得方法層級的 {@code @DS} 宣告。
     * 此優先順序與 Spring 的 {@code @Transactional} 解析慣例一致。</p>
     *
     * @param joinPoint 正在執行的連接點
     * @return 解析到的 {@link DS} 註解實例
     */
    private DS getDSAnnotation(ProceedingJoinPoint joinPoint){
        Class<?> targetClass = joinPoint.getTarget().getClass();
        DS dsAnnotation = targetClass.getAnnotation(DS.class);
        // 先判斷類別的註解，再判斷方法註解
        if(Objects.nonNull(dsAnnotation)){
            return dsAnnotation;
        }else{
            MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
            return methodSignature.getMethod().getAnnotation(DS.class);
        }
    }
}
