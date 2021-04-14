package com.zipe.base.aspect;

import com.zipe.base.annotation.DS;
import com.zipe.base.database.DataSourceHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/3/18 下午 05:14
 **/
@Aspect
@Component
@Order(-1)// 保證該AOP在@Transactional之前執行
public class DynamicDataSourceAspect {
    @Pointcut("@within(com.zipe.base.annotation.DS) || @annotation(com.zipe.base.annotation.DS)")
    public void dataSourcePointCut(){}

    @Around("dataSourcePointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        String dsKey = getDSAnnotation(joinPoint).value();
        DataSourceHolder.setDataSourceName(dsKey);
        return joinPoint.proceed();
    }

    /**
     * 根據類或方法獲取數據源註解
     */
    private DS getDSAnnotation(ProceedingJoinPoint joinPoint){
        Class<?> targetClass = joinPoint.getTarget().getClass();
        DS dsAnnotation = targetClass.getAnnotation(DS.class);
        // 先判斷類的註解，再判斷方法註解
        if(Objects.nonNull(dsAnnotation)){
            return dsAnnotation;
        }else{
            MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
            return methodSignature.getMethod().getAnnotation(DS.class);
        }
    }
}
