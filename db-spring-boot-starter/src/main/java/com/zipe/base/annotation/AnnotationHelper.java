package com.zipe.base.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public class AnnotationHelper {

  private static final String ANNOTATIONS = "annotations";
  public static final String ANNOTATION_DATA = "annotationData";

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

  public static void alterAnnotationOn(Class clazzToLookFor,
      Class<? extends Annotation> annotationToAlter, Annotation annotationValue) {
    if (isJDK7OrLower()) {
      try {
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
        Object annotationData = method.invoke(clazzToLookFor);
        //We now look for the map called "annotations" within AnnotationData object.
        Field annotations = annotationData.getClass().getDeclaredField(ANNOTATIONS);
        annotations.setAccessible(true);
        Map<Class<? extends Annotation>, Annotation> map =
            (Map<Class<? extends Annotation>, Annotation>) annotations.get(annotationData);
        map.put(annotationToAlter, annotationValue);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}