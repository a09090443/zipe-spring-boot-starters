package com.example.example.service;

import com.example.example.base.TestBase;
import com.example.model.Gleepf;
import com.example.service.ExampleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

/**
 * @author : Gary Tsai
 **/
public class ExampleServiceTest extends TestBase {

    public final ExampleService exampleServiceImpl;

    @Autowired
    public ExampleServiceTest(ExampleService exampleServiceImpl){
        this.exampleServiceImpl = exampleServiceImpl;
    }

    @BeforeEach
    public void insertNewRecord() {
        Gleepf gleepf = new Gleepf();
        gleepf.setEe010("AB12345678");
        gleepf.setEe020(new BigDecimal("55555"));
        gleepf.setEe030("FG");
        gleepf.setEe040("1234");
        gleepf.setEe070("N");
        gleepf.setEe120("***Test@fglife.com.tw");
        gleepf.setEe150(new BigDecimal("10000"));
        gleepf.setEe170(new BigDecimal("212222"));
        gleepf.setEe180("E");
        gleepf.setEe181("E");
        gleepf.setEeMark("測試資料");
        gleepf.setEeDate(new BigDecimal("20210302"));
        gleepf.setEeTime(new BigDecimal("1231"));
        gleepf.setEeUser("TEST");
        exampleServiceImpl.saveGleepf(gleepf);
    }

    @AfterEach
    public void deleteRecord() {
        Gleepf record = exampleServiceImpl.findByEE010("AB12345678");
        exampleServiceImpl.saveGleepf(record);
    }

    @Test
    public void findGleepfTest(){
        Gleepf record = exampleServiceImpl.findByEE010("AB12345678");
        Assertions.assertNotNull(record);
    }

    @Test
    public void updateGleepfTest(){
        Gleepf record = exampleServiceImpl.findByEE010("AB12345678");
        String userEmail = "test1234@fglife.com.tw";
        record.setEe120(userEmail);
        exampleServiceImpl.updateGleepf(record);
        Gleepf newRecord = exampleServiceImpl.findByEE010("AB12345678");
        Assertions.assertEquals(newRecord.getEe120(), userEmail);
    }

}
