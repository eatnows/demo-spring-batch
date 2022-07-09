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


### Job
Job은 배치 계층 구조에서 가장 상위에 있는 개념으로 하나의 배치작업 자체를 의미한다. 
Job Configuration 을 통해 생성되는 객체 단위로서 배치작업을 어떻게 구성하고 실행할 것인지를 전체적으로 설정하고 명세해 놓은 객체이다. 최상위 인터페이스이며 스프링 배치가 기본 구현체를 제공한다.
Job은 Step을 여러개 포함하고 있다. 최소한 하나의 Step은 포함 해야한다.

#### 기본 구현체

- SimpleJob
  - **순차적으로** `Step`을 실행시키는 Job
  - 모든 `Job`에서 유용하게 사용할 수 있는 표준 기능을 갖고 있다
  - `Step`을 포함하고 있는 일종의 컨테이너
- FlowJob
  - **특정한 조건과 흐름**에 따라 `Step`을 구성하여 실행시키는 Job
  - `Flow`객체를 실행시켜서 작업을 진행한다

배치를 실행시키는 주체는 `JobLauncher`라는 클래스이다, 이때 실행시킬 때 필요한 인자로 `Job객체`와 `JobParameters라는` 도메인 객체의 인자가 필요하다


### JobInstance

Job, Step, flow가 실행이 되고 수행이 되면 그 단계마다 메타데이터(실행 상태정보를 담은) 데이터 베이스의 메타데이터들을 저장하는 용도로 수행하는 도메인이다. <br>
Job이 실행될 때 생성되는 Job의 논리적 실행 단위 객체로서 고유하게 식별 가능한 작업 실행을 나타낸다. <br>
Job의 설정과 구성은 동일하지만 Job이 실행되는 시점에 처리하는 내용은 다르기 때문에 Job의 실행을 구분해야 한다.

- 처음 시작하는 Job + JobParameter 일 경우 새로운 JobInstance를 생성
- 이전과 동일한 Job + JobParameter 로 실행할 경우 이미 존재하는 JobInstance를 리턴
  - 내부적으로 JobName + JobKey (JobParamter의 해시값)를 가지고 DB로 부터 JobInstance 객체를 얻는다. (에러 발생)

JobInstance는 Job과 1:N 관계이다. (예로 매일 다른 JobParameter로 실행이 되기 때문에) <br>
동일한 JobName과 JobKey(JobParameters)를 실행하면 에러가 발생한다.

```text
Caused by: org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException: A job instance already exists and is complete for parameters={name=user1}.  If you want to run this job again, change the parameters.
```


#### JobInstance과 BATCH_JOB_INSTANCE

- `JobInstance`는 `BATCH_JOB_INSTANCE` 테이블에 해당 정보가 저장이 된다. 
- JOB_NAME (Job)과 JOB_KEY (JobParameter 해시값) 가 동일한 데이터는 중복햏서 저장할 수 없다


### JobParameter

`Job`을 실행할 때 파라미터를 설정해서 전달해주는 용도로 사용하는 객체이다. <br>
JobParameter에는 Key와 Value로 구성된 Map을 포함하고 있다. 하나의 Job에 존재할 수 있는 여러개의 `JobInstance`를 구분하기 위한 용도이다.

JobParameter를 생성하는 방법은 3가지가 있다.
1. 애플리케이션 실행 시 주입하는 방법
   - Java -jar LogBatch.jar requestData=20220630
     - 인자로 변수를 지정해서 실행 시키면 변수의 값이 JobParameter의 값으로 저장되도록 스프링 배치가 내부적으로 처리한다
     - ```text
       java -jar demo-spring-batch-0.0.1-SNAPSHOT.jar name=user20 seq\(long\)=200L date\(date\)=2022/07/01 age\(double\)=19.5
        ```

2. 코드로 생성
   - JobParameterBuilder, DefaultJobParametersConverter
     - JobParameterBuilder를 통해서 값을 지정하고 생성할 수 있는 방법
     - 주로 JobParameterBuilder 방법을 많이 사용한다
3. SpEL 이용 (스프링에서 제공하는 표현식)
   - @Value("#{jobParamter[requestDate]}"), @JobScope, @StepScope 선언 필수
   - @Value("#{jobParamter[requestDate]}") 하면 외부로 부터 넘어온 값을 표현식에 저장


JobParameter 는 `BATCH_JOB_EXECUTION_PARAM` 이라는 테이블에 저장이 된다 <br>
또 JobParameter 는 `JOB_EXECUTION` 테이블과 1:N 관계이다

스프링 배치에서 JobParameter의 타입은 `ParamterType` 이라는 Enum 클래스로 `STRING`, `DATE`, `LONG`, `DOUBLE` 총 4가지를 지원한다.

Step에서 tasklet의 `StepContribution`와 `ChunkContext` 에서 JobParameter의 값을 참조할 수 있다.

```java
@Bean
public Step step1() {
        return stepBuilderFactory.get("step1")
        .tasklet(new Tasklet() {
                @Override
                public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                
                        // contribution을 이용하여 jobParameter의 값을 참조하는 방법 (사용자가 전달한 jobParameter를 참조하는 방식)
                        JobParameters jobParameters = stepContribution.getStepExecution().getJobExecution().getJobParameters();
                        jobParameters.getString("name");
                        jobParameters.getLong("seq");
                        jobParameters.getDate("date");
                        jobParameters.getDouble("age");
                
                        // 동일한 값을 얻을 순 있지만 약간 다른식으로 맵 형태로 값을 가져온다 (값만 확인할 수 있는 방식)
                        Map<String, Object> jobParameters1 = chunkContext.getStepContext().getJobParameters();
                
                        return RepeatStatus.FINISHED;
                }
        })
        .build();
}
```

### JobExecution

JobInstance 는 Job이 실행하게 되면 JobInstance는 오직 한 번만 실행하게 된다. (JobParameter가 동일한 내용으로 실행이 될 때) 그런데 JobExecution은 JobInstance가 실행될 때 마다 생성이 된다. <Br>
JobExecution은 Job 실행 중에 발생한 정보들을 저장하고 있는 객체이다. (시작시간, 종료시간, 상태, 종료상태 속성)

JobInstance와의 관계에서 JobExecution은 한 번 또는 여러번 실행과 생성 될 수 있다.
- JobExecution은 `FAILED` 또는 `COMPLETED` 등의 Job의 실행 결과 상태를 가지고 있음 (Batch Status)
  - `JobExecution`의 실행 상태 결과가 `COMPLETED` 면 JobInstance 실행이 완료된 것으로 간주해서 재 실행이 불가하다
  - `JobExecution`의 실행 상태 결과가 `FAILED` 면 JobInstance 실행이 완료되지 않은 것으로 간주해서 재실행이 가능하다
    - JobParameter가 동일한 값으로 job을 실행할지라도 JobInstance를 계속 실행할 수 있음
  - `JobExecution`의 실행 상태 결과가 `COMPLETED` 될 때까지 하나의 JobInstance 내에서 여러 번 시도가 생길 수 있다

`JobExecution`의 정보가 `BATCH_JOB_EXECUTION` 테이블에 저장이 된다. JobInstance 와 JobExecution의 관계는 1:N 관계로 JobInstance에 대한 성공, 실패의 내역을 가지고 있다.



### Step

Batch job을 구성하는 독립적인 하나의 단계로서 실제 배치 처리를 정의하고 컨트롤하는데 필요한 모든 정보를 가지고 있는 도메인 객체이다. Step은 여러개를 둘 수가 있는데 각각의 Step은 독립적으로 생성되고 독립적으로 실행된다. Step 간의 데이터를 공유 등 간섭이 없다. 
단순한 단일 태스트 뿐만 아니라 입력과 처리 그리고 출력과 관련된 복잡한 비즈니스 로직을 포함하는 모든 설정들을 담고 있다. Job의 세부작업을 Task 기반으로 설정하고 명세해 놓은 객체이다. 모든 Job은 하나 이상ㅇml step으로 구성된다

- TaskletStep
  - 가장 기본이 되는 클래스로서 Tasklet 타입의 구현체들을 제어한다.
- PartitionStep
  - 멀티 스레드 방식으로 Step을 여러개로 분리해서 실행한다.
- JobStep
  - Step 내에서 Job을 실행하도록 한다
- FlowStep
  - Step 내에서 Flow를 실행하도록 한다


```java
// 직접 생성한 Tasklet 실행
public Step taskletStep(){
    return this.stepBuilderFactory.get("step")
        .tasklet(myTasklet())
        .build();
}
```

```java
// ChunkOrientedTasklet 실행
public Step taskletStep(){
    return this.stepBuilderFactory.get("step")
        .<Member,Member>chunk(100)
        .reader(reader)
        .writer(writer())
        .build();
}
```

```java
// Step에서 Job을 실행
public Step jobStep() {
    return this.stepBuilderFactory.get("step")
        .job(job())
        .launcher(jobLauncher)
        .parametersExtractor(jobParametersExtractor())  // jobParameter를 설정
        .build();
}
```

```java
// Step에서 Flow를 실행
public Step flowStep() {
    return this.stepBuilderFactory.get("step")
        .flow(myFlow())
        .build();
}
```


### StepExecution

Step과 StepExecution의 관계는 Job과 JobExecution의 관계와 유사하다. StepExecution은 Step에 대한 한 번의 시도를 의미하는 객체로서 Step 실행 중에 발생한 정보들을 저장하고 있는 객체이다. <br>
Step이 매번 시도될 때마다 생성되며 각 Step 별로 생성된다. Job이 재시작 되더라도 이미 성공적으로 완료된 Step은 재실행되지 않고 실패한 Step만 실행된다. (옵션을 제공하고있어 변경하면 성공한 Step도 재실행할 수 있다.) <br>
이전 단계 Step이 실패해서 현재 Step을 실행하지 않았다면 StepExecution은 생성하지 않는다. Step이 실제로 시작됐을 때만 StepExecution을 생성한다. <br>

- JobExecution과의 관계
  - Step의 StepExecution이 모두 정상적으로 완료 되어야 JobExecution이 정상적으로 완료된다.
  - Step의 StepExection 중 하나라도 실패하면 JobExecution은 실패한다.

- `BATCH_STEP_EXECUTION` 테이블과 매핑
  - JobExection과 StepExection은 1:N 관계
  - 하나의 Job에 여러개의 Step으로 구성했을 경우 각 StepExecution은 하나의 jobExecution을 부모로 갖는다.



### StepContribution

청크 프로세스의 변경 사항을 버퍼링 한 후 StepExecution 상태를 업데이트하는 도메인 객체이다. 청크 커밋 직전에 StepExection의 apply 메서드를 호출하여 상태를 업데이트 한다. <br>
일반적으로 StepExecution은 BatchStatus 값과 ExistStatus 값 두개를 가지는데 ExistStatus의 기본 종료코드 외 사용자 정의 종료코드를 생성해서 적용할 수 있다.


### ExecutionContext

Job이나 Step에 포함된 도메인은 아니다. 프레임워크에서 유지 및 관리하는 key/value 로 된 컬렉션 (일종의 map)으로 StepExecution 또는 JobExecution 객체의 상태를 저장하는 공유 객체이다. DB에 직렬화한 값으로 저장된다. `{"key":"value"}`
<br> 공유 객체라는 말은 여러 객체에서 ExecutionContext 참조해서 공유할 수 있다는 의미이다. 공유 범위로는 아래와 같다.

- Step 범위 - 각 Step의 StepExecution에 저장되며 Step 간 서로 공유 안됨
- Job 범위 - 각 Job의 JobExecution에 저장되며 Job 간 서로 공유는 되지 않지만 해당 Job의 Step 간 서로 공유가 가능하다

Job이 재시작 될 때 이미 처리한 row 데이터는 건너뛰고 이후로 수행하도록 할 때 상태 정보를 활용한다.

BATCH_JOB_EXECUTION_CONTEXT는 JobExecution의 ExecutionContext가 저장되어 있는 테이블이고 반대로 Step은 BATCH_STEP_EXECUTION_CONTEXT에 저장된다.