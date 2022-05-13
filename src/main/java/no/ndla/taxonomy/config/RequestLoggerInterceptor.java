/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

@Component
public class RequestLoggerInterceptor implements AsyncHandlerInterceptor {
    Logger logger = LoggerFactory.getLogger(getClass().getName());

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        logger.info(String.format("(%s) %s %s?%s (%s)",
                Optional.ofNullable(request.getHeader("X-Correlation-ID")).orElse(""), request.getMethod(),
                request.getRequestURI(), Optional.ofNullable(request.getQueryString()).orElse(""),
                Optional.ofNullable(request.getHeader("VersionHash")).orElse("")));
        return true;
    }
}
