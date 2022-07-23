package me.eatnows.demospringbatch.preventRestart;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class PreventRestartConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

//    @Bean
    public Job batchJob() {
        return this.jobBuilderFactory.get("batchJob")
                .start(step1())
                .next(step2())
                .preventRestart()
                .build();
    }

//    @Bean
    public Step step1() {
        return stepBuilderFactory.get("step1")
                .tasklet(new Tasklet() {
                    @Override
                    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
                        System.out.println("step1 was executed!");
                        return RepeatStatus.FINISHED;
                    }
                })
                .build();
    }

//    @Bean
    public Step step2() {
        return stepBuilderFactory.get("step2")
                .tasklet((contribution, chunkContext) -> {
//                    throw new RuntimeException("step2 was failed");
                    System.out.println("step2 was executed!");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}
