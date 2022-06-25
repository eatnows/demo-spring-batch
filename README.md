# Spring Batch

스프링 배치를 시작하려면 먼저 애플리케이션 클래스에 스프링 배치를 활성화 할 수 있도록 애너테이션을 추가한다.

```java
@EnableBatchProcessing
@SpringBootApplication
public class DemoSpringBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoSpringBatchApplication.class, args);
    }
}
```

`@EnableBatchProcessing`은 총 4개의 설정 클래스를 실행시키며 스프링 배치의 초기화와 실행 구성이 이루어진다. (자동 설정 클래스가 실행된다.)


### 스프링 배치 초기화 설정 클래스
1. BatchAutoConfiguration
   - 스프링 배치가 초기화 될 때 자동으로 실행되는 클래스이다 <br/> (스프링 부트가 자동으로 실행해준다) 
   - Job을 수행하는 JobLauncherApplicationRunner 빈을 생성한다 <br/> (스프링 부트가 ApplicationRunner라는 인터페이스를 구현한 구현체들을 실행시킨다. `JobLauncherApplicationRunner`는  `ApplicationRunner`의 구현체이다)
   
2. SimpleBatchConfiguration
    - JobBuilderFactory와 StepBuilderFactory 생성 <br/> (이 두개의 클래스로 Job과 step을 생성)
    - 스프링 배치의 주요 구성 요소 생성 - 프록시 객체로 생성된다
   
3. BatchConfigurerConfiguration
   - BasicBatchConfigurer
     - SimpleBatchConfiguration에서 생성한 프록시 객체의 실제 대상 객체를 생성하는 설정 클래스이다
     - 해당 클래스를 빈으로 의존성 주입을 받아서 주요 객체들을 참조해서 사용할 수 있다
   
   - JpaBatchConfigurer
     - JPA 관련 객체를 생성하는 설정 클래스
   
   - 사용자 정의 BatchConfigurer 인터페이스를 구현하여 사용할 수 있다

`@EnableBatchProcessing` -> `SimpleBatchConfiguration` -> `BatchConfigurerConfiguration` -> `BatchAutoConfiguration` 순으로 초기화를 진행한다.





