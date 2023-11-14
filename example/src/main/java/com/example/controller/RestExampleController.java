package com.example.controller;

import com.zipe.annotation.ResponseResultBody;
import com.example.dto.CommonMessageReq;
import com.example.model.User;
import com.example.service.ExampleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@ResponseResultBody
@RequestMapping(value = "/rest", produces = MediaType.APPLICATION_JSON_VALUE)
public class RestExampleController {
    private final ExampleService exampleService;

    RestExampleController(ExampleService exampleService) {
        this.exampleService = exampleService;
    }

    @GetMapping("/sayHello")
    public String sayHello(String name) {
        return "Hello, " + name + "!";
    }

    @GetMapping("/getUsers")
    public List<User> getUsers() {
        return exampleService.findUsers();
    }

    @GetMapping("/getDataUsers")
    public String getDataUsers() {
        exampleService.findExample1Data();
        return HttpStatus.OK.getReasonPhrase();
    }

    @GetMapping("/getDataInfos")
    public String getDataInfos() {
        exampleService.findExample2Data();
        return HttpStatus.OK.getReasonPhrase();
    }

    @GetMapping("/getDb2Test")
    public String getDb2TestData() {
        exampleService.findDb2Data();
        return HttpStatus.OK.getReasonPhrase();
    }

    @GetMapping("/getJdbcData")
    public String getJdbcData(@RequestParam String name) {
        exampleService.findByNativeSQL(name);
        return HttpStatus.OK.getReasonPhrase();
    }

    @PostMapping(value = "/passXml", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public CommonMessageReq passXml(@RequestBody CommonMessageReq request) {
        log.info("ToUserName:{}", request.getToUserName());
        log.info("FromUserName:{}", request.getFromUserName());
        log.info("MsgType:{}", request.getMsgType());
        return request;
    }

    @GetMapping("err")
    public int err() {
        exampleService.testException();
        return 1;
    }
}
