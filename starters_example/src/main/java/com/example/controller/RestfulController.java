package com.example.controller;

import com.example.service.ExampleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * RESTful API 控制器，提供範例用的 HTTP 端點，
 * 示範普通字串回應以及 Server-Sent Events（SSE）串流輸出。
 *
 * @author Gary Tsai
 */
@Slf4j
@RestController
@RequestMapping("/rest")
public class RestfulController {

    /** 通用範例服務，負責實際業務邏輯處理。 */
    private final ExampleService exampleService;

    /**
     * 建構子，透過 Spring 依賴注入初始化 {@link ExampleService}。
     *
     * @param exampleService 通用範例服務實例
     */
    @Autowired
    public RestfulController(ExampleService exampleService){
        this.exampleService = exampleService;
    }

    /**
     * 接收姓名參數並回傳問候字串。
     *
     * @param name 使用者輸入的姓名
     * @return 由 ExampleService 組合的問候訊息
     */
    @GetMapping("/sayHello")
    public String sayHello(@RequestParam String name){
        return exampleService.sayHello(name);
    }

    /**
     * 以 Server-Sent Events（SSE）格式串流輸出字母序列，
     * 每個元素之間間隔 1 秒，用於示範 Reactive 串流與 SSE 整合。
     *
     * @return 包含格式化字母字串的 {@link Flux}，以 text/event-stream 形式輸出
     */
    @GetMapping(value = "/flux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> flux() {
        return Flux.fromArray(new String[]{"a", "b", "c", "d"}).map(s -> {
            try {
                // 模擬資料產生延遲，每秒推送一筆事件
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "<letter:" + s + ">";
        });
    }
}
