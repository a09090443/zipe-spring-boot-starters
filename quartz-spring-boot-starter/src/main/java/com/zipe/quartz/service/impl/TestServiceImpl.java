package com.zipe.quartz.service.impl;

import com.zipe.quartz.service.TestService;
import org.springframework.stereotype.Service;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/28 上午 09:10
 **/
@Service
public class TestServiceImpl implements TestService {
    @Override
    public void test() {
        System.out.println("test service");
    }
}
