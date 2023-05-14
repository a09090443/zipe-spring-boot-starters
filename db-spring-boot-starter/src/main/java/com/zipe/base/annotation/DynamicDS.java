package com.zipe.base.annotation;

import java.lang.annotation.Annotation;

public class DynamicDS implements DS {

  private String value;

  public DynamicDS(String value) {
    this.value = value;
  }

  @Override
  public String value() {
    return value;
  }

  @Override
  public Class<? extends Annotation> annotationType() {
    return DynamicDS.class;
  }

}
