package com.example.service

import org.springframework.stereotype.Service

/**
 * [ExampleService] 的預設實作，提供通用範例服務的具體邏輯。
 *
 * 此類別作為 Spring Boot Starter 整合範例的基本示範，
 * 展示如何透過 [Service] 將服務實作納入 Spring 容器。
 *
 * @author Gary.Tsai
 */
@Service
class ExampleServiceImpl : ExampleService {

    /**
     * 依據傳入的姓名回傳問候語字串。
     *
     * @param name 要問候的對象名稱
     * @return 格式為 `"Hello,  <name>!"` 的問候語
     */
    override fun sayHello(name: String): String = "Hello,  $name!"
}
