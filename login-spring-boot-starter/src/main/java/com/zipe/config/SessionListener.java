package com.zipe.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import javax.servlet.http.HttpSessionEvent;

@Slf4j
public class SessionListener extends HttpSessionEventPublisher {

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        // Seconds
        event.getSession().setMaxInactiveInterval(60 * 15);
        super.sessionCreated(event);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        super.sessionDestroyed(event);
    }
}
