package com.zipe.config;

import com.zipe.service.UserService;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.ws.Endpoint;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/5/4 下午 02:09
 **/
@Configuration
public class CxfConfig {
    @Autowired
    private Bus bus;

    @Autowired
    private UserService userService;

    @Bean
    public ServletRegistrationBean cxfServlet() {
        return new ServletRegistrationBean(new CXFServlet(), "/webservice/*");
    }

    @Bean
    public Endpoint userEndpoint() {
        EndpointImpl endpoint =
                new EndpointImpl(bus, userService);
        endpoint.publish("/user");

        return endpoint;
    }
}
