package com.example.service;

import org.springframework.stereotype.Service;

/**
 * @author Gary.Tsai
 */
@Service
public class ExampleServiceImpl implements ExampleService {

  @Override
  public String sayHello(String name) {
    return "Hello,  " + name + "!";
  }
}
