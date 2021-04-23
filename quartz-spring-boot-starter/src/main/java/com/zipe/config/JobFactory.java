package com.zipe.config;

import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.scheduling.quartz.AdaptableJobFactory;
import org.springframework.stereotype.Component;

@Component
public class JobFactory extends AdaptableJobFactory {

    private final AutowireCapableBeanFactory autowireCapableBeanFactory;

    @Autowired
    JobFactory(AutowireCapableBeanFactory autowireCapableBeanFactory) {
        this.autowireCapableBeanFactory = autowireCapableBeanFactory;
    }

    @Override
    protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
        //呼叫父類
        Object instance = super.createJobInstance(bundle);
        //自動注入
        autowireCapableBeanFactory.autowireBean(instance);
        return instance;
    }
}
