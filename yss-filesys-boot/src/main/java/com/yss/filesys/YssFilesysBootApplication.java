package com.yss.filesys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.yss.filesys")
@EnableScheduling
public class YssFilesysBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(YssFilesysBootApplication.class, args);
    }
}
