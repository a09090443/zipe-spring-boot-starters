package com.zipe.model;

import lombok.Data;

@Data
public class ShanhyA implements Shanhy {
    private String test;

    @Override
    public void display() {
        System.out.println("AAAAAAAAAAAA");
    }

    public String testMethod(String arg){
        System.out.println(arg);
        return arg;
    }
}
