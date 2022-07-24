package me.eatnows.demospringbatch.incrementer;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersIncrementer;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CustomJobParametersIncrementer implements JobParametersIncrementer {

    static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyyHHdd-hhmmss");

    @Override
    public JobParameters getNext(JobParameters parameters) {

        String id = FORMAT.format(new Date());

        return new JobParametersBuilder().addString("run.id", id).toJobParameters();
    }
}
