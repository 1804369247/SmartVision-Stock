package com.example.smartvisionstock.config;

import com.example.smartvisionstock.service.ExpiryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
public class SchedulerConfig {

    @Autowired
    private ExpiryService expiryService;

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("scheduler-");
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        return scheduler;
    }

    @Scheduled(cron = "0 0 8 * * ?")
    public void runDailyExpiryCheck() {
        expiryService.runDailyExpiryCheck();
    }

    @Scheduled(cron = "0 0 12 * * ?")
    public void runMiddayCheck() {
        expiryService.checkExpiringProducts(7);
    }

    @Scheduled(cron = "0 30 18 * * ?")
    public void runEveningCheck() {
        expiryService.checkExpiringProducts(3);
    }
}