/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.task.TaskSchedulerCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ErrorHandler;

@Configuration
@EnableScheduling
public class SchedulingConfig implements TaskSchedulerCustomizer {
    @Override
    public void customize(ThreadPoolTaskScheduler taskScheduler) {
        taskScheduler.setErrorHandler(new CustomErrorHandler());
    }

    /*
     * Don't care about database errors in scheduled tasks
     */
    private static class CustomErrorHandler implements ErrorHandler {
        Logger logger = LoggerFactory.getLogger(getClass().getName());

        @Override
        public void handleError(Throwable t) {
            if (t instanceof DataAccessException) {
                logger.info("Scheduled task threw an exception, ignoring: {}", t.getMessage());
            } else {
                logger.error("Scheduled task threw an exception: {}", t.getMessage(), t);
            }
        }
    }
}
