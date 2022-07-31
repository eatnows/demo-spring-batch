package me.eatnows.demospringbatch.startNext;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@RequiredArgsConstructor
public class StartNextConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

//    @Bean
    public Job batchJob() {
        // flow라 할지라도 start next로만 구성되어 있으면 step이 실패할 경우 job 전체가 실패된다.
        return jobBuilderFactory.get("batchJob")
                .start(flowA())
                .next(step3())
                .next(flowB())
                .next(step6())
                .end()
                .build();
    }

//    @Bean
    public Flow flowA() {

        FlowBuilder<Flow> flowFlowBuilder = new FlowBuilder<>("flowA");
        flowFlowBuilder.start(step1())
                .next(step2())
                .end();

        return flowFlowBuilder.build();
    }

//    @Bean
    public Flow flowB() {

        FlowBuilder<Flow> flowFlowBuilder = new FlowBuilder<>("flowB");
        flowFlowBuilder.start(step4())
                .next(step5())
                .end();

        return flowFlowBuilder.build();
    }

//    @Bean
    public Step step1() {
        return stepBuilderFactory.get("step1")
                .tasklet(((contribution, chunkContext) -> {
                    System.out.println(">> step1 was executed");
                    return RepeatStatus.FINISHED;
                }))
                .build();
    }

//    @Bean
    public Step step2() {
        return stepBuilderFactory.get("step2")
                .tasklet(((contribution, chunkContext) -> {
                    System.out.println(">> step2 was executed");
                    return RepeatStatus.FINISHED;
                }))
                .build();
    }

//    @Bean
    public Step step3() {
        return stepBuilderFactory.get("step3")
                .tasklet(((contribution, chunkContext) -> {
                    System.out.println(">> step3 was executed");
                    return RepeatStatus.FINISHED;
                }))
                .build();
    }

//    @Bean
    public Step step4() {
        return stepBuilderFactory.get("step4")
                .tasklet(((contribution, chunkContext) -> {
                    System.out.println(">> step4 was executed");
                    throw new RuntimeException("step4 was failed");
//                    return RepeatStatus.FINISHED;
                }))
                .build();
    }

//    @Bean
    public Step step5() {
        return stepBuilderFactory.get("step5")
                .tasklet(((contribution, chunkContext) -> {
                    System.out.println(">> step5 was executed");
                    return RepeatStatus.FINISHED;
                }))
                .build();
    }

//    @Bean
    public Step step6() {
        return stepBuilderFactory.get("step6")
                .tasklet(((contribution, chunkContext) -> {
                    System.out.println(">> step6 was executed");
                    return RepeatStatus.FINISHED;
                }))
                .build();
    }
}
