package com.meryt.demographics.log;

import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Slf4j
public class RequestLogger extends CommonsRequestLoggingFilter {

    @Override
    protected void beforeRequest(HttpServletRequest request, String message) {
        // do nothing
    }

    @Override
    protected void afterRequest(HttpServletRequest request, String message) {
        log.info(String.format("%s %s", request.getMethod(), request.getRequestURI()));
    }
}
