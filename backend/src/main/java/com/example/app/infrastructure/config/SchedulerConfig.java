package com.example.app.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerConfig {

    /**
     * 스케줄러 전용 스레드풀.
     * 동기화 작업(시스템 수 기준)과 유지보수 작업을 분리해 처리한다.
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("sync-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setErrorHandler(t ->
                org.slf4j.LoggerFactory.getLogger(SchedulerConfig.class)
                        .error("[Scheduler] 작업 실행 중 오류 발생", t)
        );
        return scheduler;
    }
}
