/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.config;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

@Component
public class RequestLoggerInterceptor implements AsyncHandlerInterceptor {
    Logger logger = LoggerFactory.getLogger(getClass().getName());
    static final String START_TIME = "startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        request.setAttribute(START_TIME, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        long startTime = (Long) request.getAttribute(START_TIME);
        long latency = System.currentTimeMillis() - startTime;
        logger.info(String.format(
                "(%s) %s %s?%s (%s) executed in %sms with code %s",
                Optional.ofNullable(request.getHeader("X-Correlation-ID")).orElse(""),
                request.getMethod(),
                request.getRequestURI(),
                Optional.ofNullable(request.getQueryString()).orElse(""),
                Optional.ofNullable(request.getHeader("VersionHash")).orElse(""),
                "" + latency,
                response.getStatus()));
        AsyncHandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
