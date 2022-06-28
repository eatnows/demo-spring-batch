# Spring Batch

[출처강의](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-%EB%B0%B0%EC%B9%98)

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


### Job 관련 테이블

- BATCH_JOB_INSTANCE
  - Job이 실행될 때 JobInstance 의 정보가 저장되며 job_name과 job_key(Job paramter롤 해쉬값으로 한 데이터)를 키로 하여 하나의 데이터를 저장한다 
  - 동일한 job_name 과 job_key로는 중복 저장할 수 없다
- BATCH_JOB_EXECUTION
  - job execution에 저장되어있는 도메인 객체의 데이터가 저장되는 테이블 
  - job 의 **실행정보가 저장**되며 Job 생성, 시작, 종료 시간, 실행상태, 메시지 등을 저장해서 관리한다
- BATCH_JOB_EXECUTION_PARAMS
  - Job과 함께 실행되는 JobParameter 정보를 저장
- BATCH_JOB_EXECUTION_CONTEXT
  - Job의 실행동안 여러가지 상태정보, 공유 데이터를 Json 형식으로 직렬화해서 저장한다
  - Step 간 서로 공유가 가능하다
  - ExecutionContext 라는 도메인 객체가 가지고 있는 데이터를 저장하는 테이블

### Step 관련 테이블

- BATCH_STEP_EXECUTION
  - Step의 실행정보가 저장되며 생성, 시작, 종료 시간, 실행상태, 메시지 등을 저장하여 관리
- BATCH_STEP_EXECUTION_CONTEXT
  - Step의 실행동안 여러가지 상내정보, 공유 데이터를 Json 형식으로 직렬화해서 저장한다
  - Step 별로 저장되며 Step 간 서로 공유할 수 없다


#### 테이블 칼럼 설명

```sql
CREATE TABLE BATCH_JOB_INSTANCE (
  JOB_INSTANCE_ID BIGINT PRIMARY KEY,   -- 식별자, 기본 키
  VERSION BIGINT,                       -- 업데이트 될 떄마다 1 증가 
  JOB_NAME VARCHAR(100) NOT NULL,       -- Job의 이름 (Job을 구성할 때 부여)
  JOB_KEY VARCHAR(2500)                 -- job_name과 jobParamerter를 합쳐서 해싱한 값 (유잃한 job_key만 저장된다) 
);
```

```sql
CREATE TABLE BATCH_JOB_EXECUTION (
    JOB_EXECUTION_ID BIGINT PRIMARY KEY,    -- 식별자, 기본 키, JOB_INSTANCE와 일대 다 관계
    VERSION BIGINT,                         
    JOB_INSTANCE_ID BIGINT NOT NULL,        -- JOB_INSTANCE의 KEY
    CREATE_TIME TIMESTAMP NOT NULL,         -- 실행(Execution)이 생성된 시점
    START_TIME TIMESTAMP DEFAULT NULL,      -- 실행(Execution)이 시작된 시점
    END_TIME TIMESTAMP DEFAULT NULL,        -- 실행(Execution)이 종료된 시점, Job 실행 도중 오류가 발생해서 Job이 중단된 경우 저장되지 않을 수 있다
    STATUS VARCHAR(10),                     -- 실행 상태 (BatchStatus)를 저장 (COMPLETED, FAILED, STOPPED ...)
    EXIT_CODE VARCHAR(20),                  -- 실행 종료코드 (ExitStatus) 를 저장 (COMPLETED, FAILED ...) 
    EXIT_MESSAGE VARCHAR(2500),             -- Status가 실패일 경우 실패 원인 등의 내용을 저장
    LAST_UPDATED TIMESTAMP,                 -- 마지막 실행 시점을 기록
    JOB_CONFIGURATION_LOCATION VARCHAR(2500) NULL
);
```

```sql
CREATE TABLE BATCH_JOB_EXECUTION_PARAMS (
  JOB_EXECUTION_ID BIGINT NOT NULL ,    -- JobExecution 식별자
  TYPE_CD VARCHAR(6) NOT NULL ,         -- STRING, LONG, DATE, DOUBLE 타입 정보
  KEY_NAME VARCHAR(100) NOT NULL,       -- (키) 파라미터 키 값
  STRING_VAL VARCHAR(250),              -- (밸류) 파라미터 문자 값
  DATE_VAL DATETIME DEFAULT NULL,       -- (밸류) 파라미터 날짜 값
  LONG_VAL BIGINT,                      -- (밸류) 파라미터 LONG 값
  DOUBLE_VAL DOUBLE PRECISION,          -- (밸류) 파라미터 DOUBLE 값
  IDENTIFYING CHAR(1) NOT NULL          -- 식별 여부 (true/ false)
);
```

```sql
CREATE TABLE BATCH_JOB_EXECUTION_CONTEXT (
  JOB_EXECUTION_ID BIGINT PRIMARY KEY,      -- JobExecution 식별자, Job_Execution마다 생성된다.
  SHORT_CONTEXT VARCHAR(2500) NOT NULL,     -- Job의 실행 상태정보, 공유데이터 등의 정보를 문자열로 저장한다
  SERIALIZED_CONTEXT CLOB                   -- 직렬화(serialized)된 전체 컨텍스트
);
```

```sql
CREATE TABLE BATCH_STEP_EXECUTION (
  STEP_EXECUTION_ID BIGINT PRIMARY KEY, -- Step의 실행정보를 식별할 수 있는 기본 키
  VERSION BIGINT NOT NULL,
  STEP_NAME VARCHAR(100) NOT NULL,      -- Step을 구성할 때 부여하는 Step 이름
  JOB_EXECUTION_ID BIGINT NOT NULL,     -- JobExecution 기본키, 일대 다 관계
  START_TIME TIMESTAMP NOT NULL,        -- 실행(Execution)이 시작된 시점
  END_TIME TIMESTAMP DEFAULT NULL,      -- 실행(Execution)이 종료된 시점, 오류가 발생하여 Job이 중단된 경우 저장되지 않을 수 있다
  STATUS VARCHAR(10),                   -- 실행 상태 (BatchStatus) 저장
  COMMIT_COUNT BIGINT,                  -- 트랜잭션 당 커밋되는 수를 기록
  READ_COUNT BIGINT,                    -- 실행시점에 Read한 Item 수
  FILTER_COUNT BIGINT,                  -- 실행도중 필터링된 Item 수
  WRITE_COUNT BIGINT,                   -- 실행도중 저장되고 커밋된 Item 수
  READ_SKIP_COUNT BIGINT,               -- 실행도중 Read가 skip된 Item 수
  WRITE_SKIP_COUNT BIGINT,              -- 실행도중 write가 skip된 Item 수
  PROCESS_SKIP_COUNT BIGINT,            -- 실행도중 Process가 Skip된 Item 수
  ROLLBACK_COUNT BIGINT,                -- 실행도중 rollback이 일어난 수
  EXIT_CODE VARCHAR(20),                -- 실행 종료코드 (ExitStatus) 를 저장
  EXIT_MESSAGE VARCHAR(2500),           -- Status가 실패일 경우 실패 원인 등을 저장
  LAST_UPDATED TIMESTAMP                -- 마지막 실행 (Execution) 시점을 TimeStamp 형식으로 기록
);
```

```sql
CREATE TABLE BATCH_STEP_EXECUTION_CONTEXT (
  STEP_EXECUTION_ID BIGINT PRIMARY KEY,     -- StepExecution 식별 자, STEP_EXECUTION 마다 각각 생성
  SHORT_CONTEXT VARCHAR(2500) NOT NULL,     -- STEP의 실행 상태정보, 공유데이터 등의 정보를 문자열로 저장
  SERIALIZED_CONTEXT CLOB                   -- 직렬화(serialized)된 전체 컨텍스트
);
```