package me.eatnows.demospringbatch.jobBuilderFactory;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
@RequiredArgsConstructor
public class JobConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;


//    @Bean
//    public Job batchJob1() {
//        return this.jobBuilderFactory.get("batchJob1")
//                .start(step1())
//                .next(step2())
//                .build();
//    }

//    @Bean
//    public Job batchJob2() {
//        return this.jobBuilderFactory.get("batchJob2")
//                .start(flow())
//                .next(step5())
//                .end() // flowJob을 마칠때는 end() 구문을 넣어줘야 함
//                .build();
//    }
//
//    @Bean
//    public Step step1() {
//        return stepBuilderFactory.get("step1")
//                .tasklet(new Tasklet() {
//                    @Override
//                    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
//                        System.out.println("step1 has executed");
//                        return RepeatStatus.FINISHED;
//                    }
//                })
//                .build();
//    }
//
//    @Bean
//    public Step step2() {
//        return stepBuilderFactory.get("step2")
//                .tasklet((contribution, chunckContext) -> {
//                    System.out.println("step2 has executed");
//                    return RepeatStatus.FINISHED;
//                })
//                .build();
//    }
//
//    @Bean
//    public Flow flow() {
//        // flowJob 안에 flow 객체가 포함되어 실행된다.
//        FlowBuilder<Flow> flowBuilder = new FlowBuilder<>("flow");
//        flowBuilder.start(step3())
//                .next(step4())
//                .end();
//
//        return flowBuilder.build();
//    }
//
//    @Bean
//    public Step step3() {
//        return stepBuilderFactory.get("step3")
//                .tasklet((contribution, chunckContext) -> {
//                    System.out.println("step3 has executed");
//                    return RepeatStatus.FINISHED;
//                })
//                .build();
//    }
//
//    @Bean
//    public Step step4() {
//        return stepBuilderFactory.get("step4")
//                .tasklet((contribution, chunckContext) -> {
//                    System.out.println("step4 has executed");
//                    return RepeatStatus.FINISHED;
//                })
//                .build();
//    }
//
//    @Bean
//    public Step step5() {
//        return stepBuilderFactory.get("step5")
//                .tasklet((contribution, chunckContext) -> {
//                    System.out.println("step5 has executed");
//                    return RepeatStatus.FINISHED;
//                })
//                .build();
//    }
}
