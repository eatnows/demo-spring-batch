package me.eatnows.demospringbatch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * ApplicationRunner: 스프링부트가 초기화되고 완료가 되면 가장 먼저 호출하는 클래스 (run 메서드가 호출된다)
 */
//@Component
public class JobRunner implements ApplicationRunner {

//    @Autowired
//    private JobLauncher jobLauncher;
//
//    @Autowired
//    private Job job; // JobInstanceConfiguration에서 만든 Job bean을 주입

    @Override
    public void run(ApplicationArguments args) throws Exception {

//        JobParameters jobParameters = new JobParametersBuilder()
//                .addString("name", "user2")
//                .toJobParameters();

//        jobLauncher.run(job, jobParameters);
    }
}
