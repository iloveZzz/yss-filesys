package com.yss.filesys.schedule;

import com.yss.filesys.application.port.FileRecycleUseCase;
import com.yss.filesys.config.ApplicationContextProvider;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Slf4j
@DisallowConcurrentExecution
public class RecycleCleanupJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        FileRecycleUseCase fileRecycleUseCase = ApplicationContextProvider.getBean(FileRecycleUseCase.class);
        int purged = fileRecycleUseCase.purgeExpiredRecycleItems();
        log.info("Recycle cleanup schedule triggered, purged {} records.", purged);
    }
}
