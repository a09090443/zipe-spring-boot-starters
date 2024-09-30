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
 * @author Gary Tsai
 */
@Slf4j
@RestController
@RequestMapping("/rest")
public class RestfulController {

    private final ExampleService exampleService;

    @Autowired
    public RestfulController(ExampleService exampleService){
        this.exampleService = exampleService;
    }

    @GetMapping("/sayHello")
    public String sayHello(@RequestParam String name){
        return exampleService.sayHello(name);
    }
    @GetMapping(value = "/flux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> flux() {
        return Flux.fromArray(new String[]{"a", "b", "c", "d"}).map(s -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "<letter:" + s + ">";
        });
    }
}
