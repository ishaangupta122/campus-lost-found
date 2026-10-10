package com.campus.lostandfound.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on @Scheduled methods (used by UploadCleanup). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
