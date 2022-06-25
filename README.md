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


Job이 실행되는 동안 JobExecution이 실행된다. Step이 실행되는 동안 StepExecution이 실행된다.
각각 job과 step의 실행정보, 상세정보가 저장된다.


### 스프링 배치 메타 데이터 (실행정보, 상세정보 등)
- 스프링 배치의 실행 및 관리를 위한 목적으로 여러 도메인(Job, Step, JobParameters 등)들의 정보들을 저장, 업데이트, 조회할 수 있는 스키마를 제공한다. (해당 스키마를 사용하는 것은 선택이다)
- 과거, 현재의 실행에 대한 상세정보, 실행 정보(성공, 실패 여부 등) 등을 관리함으로서 예외나 오류 발생시 빠른 대처가 가능하다
- DB와 연동할 경우 필수적으로 메타 테이블이 생성되어야 한다 (대부분 DB와 연도해서 사용함)

DB 스키마는 `/org/springframework/batch/core/schema-*.sql` 로 DB 유형별로 제공하고 있다

스키마를 생성하는 방법은 수동생성과, 자동생성 두가지 방법이 있다.
- 수동생성 : 쿼리를 직접 실행
- 자동생성 : 스프링 배치를 실행하면 자동적으로 스키마를 실행한다. `properties` 파일에 스키마 자동 생성 속성을 줄 수 있다.
  - spring.batch.jdbc.initialize-schema
    - `ALWAYS` : 스크립트가 항상 실행된다. 애플리케이션에 RDBMS가 설정이 되어있으면 내장 DB보다 우선적으로 실행된다.
    - `EMBEDDED` : 내장 DB일 때만 실행되어 스키마가 자동으로 생성된다. (Default 설정) 내장 DB일 때만 사용할 수 있음
    - `NEVER` : 스크립트를 실행하지 않는다. 내장 DB일 경우 스크립트가 생성안되기 때문에 오류가 발생한다. 운영에서 수동으로 스크립트를 생성 후 설정할 때 쓰는 설정값이다

    
```yaml
spring:
  config:
    activate:
      on-profile: mysql
  datasource:
    hikari:
      jdbc-url: jdbc:mysql://localhost:3306/demo_batch?useUnicode=true&characterEncoding=utf8
      username: root
      password: password
      driver-class-name: com.mysql.jdbc.Driver
  batch:
    jdbc:
      initialize-schema: always
```



