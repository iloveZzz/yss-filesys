package com.yss.filesys.config;

import com.yss.filesys.schedule.RecycleCleanupJob;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.CronScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail recycleCleanupJobDetail() {
        return JobBuilder.newJob(RecycleCleanupJob.class)
                .withIdentity("recycleCleanupJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger recycleCleanupTrigger(JobDetail recycleCleanupJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(recycleCleanupJobDetail)
                .withIdentity("recycleCleanupTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 5 0 * * ?"))
                .build();
    }
}
