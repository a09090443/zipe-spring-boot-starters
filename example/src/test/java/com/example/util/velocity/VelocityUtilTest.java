package com.example.util.velocity;

import com.example.base.TestBase;
import com.zipe.util.VelocityUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VelocityUtilTest extends TestBase {

    private final VelocityUtil velocityUtil;

    @Autowired
    VelocityUtilTest(VelocityUtil velocityUtil) {
        this.velocityUtil = velocityUtil;
    }

    @Test
    public void testGenFile(){
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("name", "Gary");
        params.put("project", "Example project");
        String result = velocityUtil.generateContent("hello.vm", params);
        assertEquals(result, "Hello World! Name is Gary. Project is Example project");
    }
}
