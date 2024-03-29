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



### JobRepository

배치 작업 중에 정보를 저장하는 저장소 역할을 한다. Job이 언제 실행되었고, 언제 종료되었는지, 몇 번 실행 되었는지 등 실행에 대한 결과 등의 배치 작업의 수행과 관련된 ahems metadata를 저장한다. (JobLauncher, Job, Step 구현체 내부에서 CRUD 기능을 처리)

#### JobRepository 설정

- `@EnableBatchProcessing` 애너테이션을 선언하면 `JobRepository`가 자동으로 빈으로 생성된다.
- BatchConfigurer 인터페이스를 구현하거나, BatchConfigurer를 구현한 BasicBatchConfigurer 를 상속해서 JobRepository 설정을 커스터마이징 할 수 있다.
  1. JDBC 방식으로 설정 - JobRepositoryFactoryBean 를 통해 설정
    - 내부적으로 AOP 기술을 통해 트랜잭션 처리를 해주고 있다
    - 트랜잭션 isolation 의 기본값은 SERIALIZEBLE로 최고수준이지만 다른 레벨(READ_COMMITED, REPEATABLE_READ)로도 지정 가능
    - 메타테이블의 Table Prefix를 변경할 수 있다. 기본값은 `BATCH_` 이다.
  1. In Memory 방식으로 설정 - MapJobRepositoryFactoryBean
     - 성능 등의 이유로 도메인 오브젝트를 굳이 데이터베이스에 저장하고 싶지 않을 경우
     - 보통 Test나 프로토타입의 빠른 개발이 필요할 때 사용한다.



### JobLauncher

배치 Job을 실행시키는 역할을 한다. 실행을 할 때 Job과 Job Parameters를 인자로 받으며 요청된 배치 작업을 수행한 후 최종 client 에게 JobExecution을 반환한다 <br>
스프링 부트 배치가 구동이 되면 JobLauncher 빈이 자동으로 생성된다.

#### Job 실행

- JobLauncher.run(Job, JobParameters)
- 스프링 부트 배치에서는 JobLauncherApplicationRunner 가 자동적으로 JobLauncher를 실행시킨다.
  - 동기적 실행
    - taskExecutor를 SyncTaskExecutor로 설정할 경우 (기본값은 SyncTaskExecutor) 
    - JobExecution을 획득하고 배치 처리를 최종 완료한 이후 Client에게 JobExecution을 반환
    - 스케쥴러에 의한 배치처리에 적합 하다 - 배치처리시간이 길어도 상관없을 경우
  - 비 동기적 실행
    - taskExecutor 가 SimpleAsyncTaskExecutor로 설정할 경우
    - JobExecution을 획득한 후 Client에게 바로 JobExecution을 반환하고 배치처리를 완료한다
    - HTTP 요청에 의한 배치처리에 적합하다 - 배치처리 시간이 길 경우 응답이 늦어지지 않도록 한다



### JobLauncherApplicationRunner

- Spring Batch 작업을 시작하는 ApplicationRunner 로서 BatchAutoConfiguration에서 생성된다.
- 스프링 부트에서 제공하는 ApplicationRunner 의 구현체로 애플리케이션이 정상적으로 구동되자마자 실행된다.
- 기본적으로 빈으로 등록된 모든 job을 실행시킨다


### BatchProperties

- Spring Batch 의 환경 설정 클래스
- Job 이름, 스키마 초기화 설정, 테이블 Prefix 등의 값을 설정할 수 있다
- `application.properties` 또는 `application.yml` 파일에 설정할 수 잇다

```yaml
batch:
  job:
    names: ${job.name:NONE}
  initialize-schema: NEVER
  tablePrefix: SYSTEM
```


Job을 실행할 때 여러가지 옵션을 설정할 수 있는데 여러가지 Job 중에서 지정한 Batch Job 만 실행하도록 할 수 있다.
- `spring.batch.job.names: ${job.name:NONE}`
- 애플리케이션 실행시 Program arguments 로 job 이름을 입력한다

```text
--job.name=helloJob
--job.name=helloJob,simpleJob 
```

하나 이상의 job을 실행할 경우 `,`로 구분해서 입력한다


### JobBuilderFactory

JobBuilder를 생성하는 팩토리 클래스로서 get(String name) 메서드를 제공해준다.
jobBuilderFactory.get("jobName")
- `jobName`은 스프링 배치가 Job을 실행시킬 때 참조하는 Job의 이름으로 DB에도 해당 이름으로 저장된다

### JobBuilder

- Job을 구성하는 설정 조건에 따라 두 개의 하위 빌더 클래스를 생성하고 실제 Job 생성을 위임한다. (Job을 생성하는 클래스가 아님)

Job을 직접 생성하는 하위 클래스는 아래와 같다.

1. SimpleJobBuilder
  - SimpleJob을 생성하는 Builder 클래스 
  - Job 실행과 관련된 여러 설정 API 제공
2. FlowJobBuilder
  - FlowJob을 생성하는 Builder 클래스
  - 내부적으로 FlowBuilder 를 반환함으로써 Flow 실행과 관련된 여러 설정 API를 제공한다



### SimpleJob

- SimpleJob은 Step을 실행시키는 Job의 구현체
- SimpleJobBuilder에 의해 생성이 된다
- 여러 단계의 Step으로 구성할 수 있으며 Step을 순차적으로 실행
- 모든 Step의 실행이 성공적으로 완료되어야 Job이 성공적으로 완료된다
- 맨 마지막에 실행한 Step의 BatchStatus가 Job의 최종 BatchStatus가 된다

```java
public Job batchJob() {
    return jobBuilderFactory.get("batchJob")    // JobBuilder를 생성하는 팩토리, Job의 이름을 매개변수로 받음
        .start(Step)                           // 처음 실행할 Step 설정, 최초 한번 설정, 이 메서드를 실행하면 SimpleJobBuilder 반환     
        .next(Step)                             // 다음에 실행할 Step 설정, 횟수 제한이 없고 모든 next()의 Step이 종료되면 Job이 종료된다
        .incrementer(JobParametersIncrementer)   // JobParameter의 값을 자동으로 증가해 주는 JobParametersIncrementer 설정
        .preventRestart(true)                   // Job의 재시작 가능 여부 설정, 기본값은 true
        .validator(JobParameterValidator)       // JobParameter를 실행하기 전에 올바른 구성이 되었는지 검증하는 JobParametersValidator 설정
        .listener(JobExecutionListener)         // Job 라이프 사이클의 특정 시점에 콜백을 제공받도록 JobExecutionListener 설정
        .build();                               // SimpleJob 생성
}
```



#### validator()

Job실행에 꼭 필요한 JobParameter를 검증하는 용도로 사용되며 검증에 실패하면 Job을 실행하지 않는다 <br>
DefaultJobParametersValidator 구현체를 지원하며, 좀 더 복잡한 제약 조건이 있다면 인터페이스를 직접 구현할 수도 있다 (parameter의 key로 검증하는 경우에 사용하면 좋다)

validator는 총 두번 실행된다
1. Job이 수행되기전에 JobRepository의 기능이 수행가지 전 validate를 실행
2. Job이 실행되기 전에 validate 실행


#### preventRestart()

- job의 재시작 여부를 설정 (Job은 실패했을 경우에만 재시작할 수 있음)
- 기본값은 true이며 false로 설정 시 Job의 재시작을 지원하지 않는다 라는 의미이다 (Job이 실패하더라도 재 시작할 수 없음)
- Job이 실패해도 재시작이 안되며 Job을 재시작하려고 하면 JobParameterException이 발생된다
- 재시작과 관련 있는 기능으로 Job을 처음 실행하는 것 과는 관련없음
- `perventRestart()` 를 추가하기만 해도 재시작을 하지 않겠다는 의미 (restartable의 값이 false로 변경)


#### incrementer()

- JobParameters에서 필요한 값을 증가시켜 다음에 사용될 JobParameters 오브젝트를 반환
- 기존의 JobParameter 변경없이 Job을 여러 번 시작하고자 할 때 사용한다
- RunIdIncrementer 구현체를 지원하고 인터페이스를 직접 구현할 수도 있다



#### StepBuilderFactory 

- StepBuilder를 생성하는 팩토리 클래스로 get(String name) 메서드를 제공한다
- StepBuilderFactory.get("stepName") (stepName으로 Step을 생성)


#### StepBuilder

- Step을 구성하는 설정 조건에 따라 다섯 개의 하위 빌더 클래스를 생성하고 실제 Step 생성을 위임한다
- **TaskletStepBuilder**
  - TaskletStep 을 생성하는 기본 빌더 클래스 (Tasklet interfcae를 구현한 구현체를 실행)
- **SimpleStepBuilder**
  - TaskletStep 을 생성하며 내부적으로 청크기반의 작업을 처리하는 ChunkOrientedTasklet 클래스를 생성한다
- **PartitionStepBuilder**
  - PartitionStep 을 생성하며 멀티 스레드 방식으로 Job을 실행한다
- **JobStepBuilder**
  - JobStep 을 생성하야 Step 안에서 Job을 실행한다
- **FlowStepBuilder**
  - FlowStep 을 생성하여 Step 안에서 Flow를 실행한다
  
`JobRepository` 는 빌더 클래스를 통해 Step 객체에 전달되어 메타데이터를 기록하는데 사용된다




#### tasklet

- 스프링 배치에서 제공하는 Step의 구현체로 Tasklet을 실행시키는 도메인 객체이다
- RepeatTemplate를 사용해서 Tasklet의 구문을 트랜잭션 경계 내에서 반복해서 실행한다
- Task 기반과 Chunk 기반으로 나누어서 Tasklet을 실행한다

스프링 배치에서 Step의 실행 단위는 크게 2가지로 나뉜다
1. Chunk 기반
  - 하나의 큰 덩어리를 n개씩 나눠서 실행한다는 의미로 대량 처리를 하는 경우 효과적으로 설계되어있다.
  - ItemReader, ItemProcessor, ItemWriter를 사용하며 청크 기반 전용 Tasklet인 ChunkOrientedTasklet 구현체가 제공된다.
1. Task 기반
   - ItemReader와 ItemWriter와 같은 청크 기반의 작업보다 단일 작업 기반으로 처리되는 것이 더 효율적인 경우에 사용한다.
   - 주로 Tasklet 구현체를 만들어 사용한다
   - 대량 처리를 하는 경우 chunk 기반에 비해 더 복잡한 구현이 필요하다

```java
public Step batchStep() {
    return stepBuilderFactory.get("batchStep")   // StepBuilder 를 생성하는 팩토리, Step의 이름을 매개변수로 받는다
        .tasklet(Tasklet)                   // Tasklet 클래스 설정, 이 메서드를 실행하면 TaskletStepBuilder 를 반환 
        .startLimit(10)                     // Step의 실행 횟수를 설정, 설정한 만큼 실행이되고 초과시 오류를 발생, 기본값은 INTEGER.MAX_VALUE
        .allowStartIfComplete(true)         // Step의 성공, 실패와 상관없이 항상 Step을 실행하기 위한 설정 
        .listener(StepExecutionListener)    // Step 라이프 사이클의 특정 시점에 콜백을 제공받도록 StepExecutionListener 설정
        .build();                           // TaskletStep을 생성
}

```



#### tasklet()

- Step 내에서 구성되고 실행되는 도메인 객체로 주로 단일 task를 수행하기 위해 사용한다
- TaskeltStep 에 의해 반복적으로 수행되며 반환값에 따라 계속 수행 혹은 종료한다
  - RepeatStatus - Tasklet 의 반복 여부 상태값
    - RepeatStatus.FINISHED - Tasklet 종료, RepeatStatus 를 null 로 반환하면 RepeatStatus.FINISHED로 해석된다
    - RepeatStatus.CONTINUABLE - Tasklet 반복
    - RepeatStatus.FINISHED 가 리턴된거나 실패 예외가 던져지기 전까지 TaskletStep 에 의해 while 문 안에서 반복적으로 호출된다. (무한루프)
- 익명 클래스 혹은 구현 클래스를 만들어서 사용한다. (일반 적으로는 구현 클래스를 만들어서 사용을 많이함)
- 이 메서드를 실행하게 되면 TaskletStepBuilder 가 반환되어 관련 API를 설정할 수 있다.
- Step 에 오직 하나의 Tasklet 설정이 가능하며 두개 이상을 설정했을 경우 마지막 설정한 객체가 실행된다.



#### startLimit()

- Step의 실행 횟수를 조정할 수 있다
- Step마다 설정할 수 있다
- 설정 값을 초과해서 다시 실행하려고 하면 StartLimitExceededException이 발생
- start-limit의 디폴트 값은 Integer.MAX_VALUE


#### allowStartIfComplete()

- 재시작 가능한 job에서 Step의 이전 성공 여부와 상관없이 항상 step을 실행하기 위한 설정
- 실행 마다 유효성을 검증하는 Step이나 사전 작업이 꼭 필요한 Step 등은 성공이 되었어도 그때그떄마다 실행이 되어야하는 작업에 사용됨
- 기본적으로 COMPLETED 상태를 가진 Step은 Job 재 시작 시 실행하지 않고 스킵한다
- allow-start-if-complete가 "true"로 설정된 step은 항상 실행한다



#### JobStep

- Job에 속하는 Step 중 외부의 Job을 포함하고 있는 Step
- 외부의 Job이 실패하면 해당 Step이 실패하므로 결국 최종 기본 Job도 실패한다
- 모든 메타데이터는 기본 Job과 외부 Job 별로 각각 저장된다
- 커다란 시스템을 작은 모듈로 쪼개고 job의 흐름을 관리하고자 할 때 사용할 수 잇다.

```text
StepBuilderFactory -> StepBuilder -> JobStepBuilder -> JobStep
```

```java
public Step jobStep() {
    return stepBuilderFactory.get("jobStep")         // StepBuilder를 생성하는 팩토리, Step의 이름을 매개변수로 받음
        .job(Job)                                    // JobStep 내에서 실행될 Job 설정, JobStepBuilder반환
        .launcher(JobLauncher)                       // Job을 실행할 JobLauncher설정
        .parametersExtractor(JobParametersExtractor) //  Step의 executionContext를 Job이 실행되는 데 필요한 JobParameters로 변환
        .build();                                    // JobStep을 생성
}
```



## FlowJob

- Step을 순차적으로만 구성하는 것이 아니고 특정한 상태에 따라 흐름을 전환호도록 구성할 수 있다.
  - Step이 실패 하더라도 Job은 실패로 끝나지 않도록 해야하는 경우
  - Step이 성공 했을 때 다음에 실행해야 할 Step을 구분해서 실행해야 하는 경우
  - 특정 Step은 전혀 실행되지 않게 구성해야 하는 경우
- FlowJobBuilder에 의해 생성된다
- Flow 와 Job의 흐름을 구성하는데만 관여하고 실제 비즈니스 로직은 Step에서 이루어진다.
- 내부적으로 SimpleFlow 객체를 포함하고 있으며 Job 실행 시 호출한다

```text
JobBuilderFactory -> JobBuilder -> JobFlowBuilder -> FlowBuilder -> FlowJob
```

```java
public Job batchJob() {
    return jobBuilderFactory.get("batchJob")
        .start(Step)                                // Flow를 시작하는 Step을 설정
        .on(String pattern)                         // Step의 실행 결과로 돌려받는 종료상태(ExitStatus) 를 캐치하여 매칭하는 패턴, TransitionBuilder를 반환 (.start(Step)의 경우 SimpleJobBuilder를 반환하게 되지만 .on()와 함께라면 JobFlowBuilder를 반환한다)
        .to(Step)                                   // 다음으로 이동할 Step을 지정 (on 다음에 어떻게 할 것이냐)
        .step() / fail() / end() / stopAndRestart() // Flow를 중지/ 실패/ 종료 하도록 (Flow 자체가 종료)
        .from(Step)                                 // 이전 단계에서 정의한 Step의 Flow를 추가적으로 정의한다
        .next(Step)                                 // 다음으로 이동할 Step 지정
        .end()                                      // build() 앞에 위차하면 FlowBuilder를 종료하고 SimpleFlow 객체 생성
        .build()                                    // FlowJob을 생성하고 flow 핖드에 SimpleFlow 를 저장 
}
```

flow라 할지라도 start()와 next()로만 구성되어 있으면 step이 실패할 경우 job 전체가 실패된다.



### Transition

전환한다, 전이된다라는 의미를 가진다, 조건에 따라서 Job이 실행되고 Step이 실행될 때 해당 흐름들을 A 또는 B 또는 C로 전환시킬 수 있는 기능을 가지고 있다. (혹은 A의 상태에 따라 B의 흐름으로 넘어간다던지) 


#### BatchStatus

JobExecution과 StepExecution의 속성으로 Job과 Step의 종료 후 최종 결과 상태가 무엇인지 정의

- **SimpleJob**
  - 마지막 Step의 BatchStatus값을 Job의 최종 BatchStatus 값으로 반영
    - (Step이 실패할 경우 해당 Step이 마지막 Step이 된다)
- **FlowJob**
  - Flow(SimpleFlow) 내 Step의 ExitStatus 값을 FlowExecutionStatus 값으로 저장
  - 마지막 Flow의 FlowExecutionStatus 값을 Job의 최종 BatchStatus 값으로 반영

BatchStatus의 상태값 <br>
`COMPLETED`, `STARTING`, `STARTED`, `STOPPING`, `STOPPED`, `FAILED`, `ABANDONED`, `UNKNOWN`
- `ABANDONED`: 처리를 완료했지만 성공하지 못한 단계와 재시작 시 건더 뛰어야하는 단계 (일반적으로 실패하면 `FAILED` 단계로 재시작이 가능하지만 `ABANDONED`는 실패한건 맞지만 재시작이 안된다)


#### ExitStatus

- JobExecution과 StepExecution의 속성으로 Job과 Step의 실행 후 어떤 상태로 종료되었는지 정의
- 기본적으로 ExitStatus는 BatchStatus와 동일한 값으로 설정이 된다

- SimpleJob
  - 마지막 Step의 ExitStatus 값을 Job의 최종 ExitStatus 값으로 반영
- FlowJob
  - Flow 내 Step의 ExitStatus 값을 FlowExecutionStatus 값으로 저장
  - 마지막 Flow의 FlowExecutionStatus 값을 Job의 최종 ExitStatus 값으로 반영

`UNKNOWN`, `EXECUTING`, `COMPLETED`, `NOOP`, `FAILED`, `STOPPED`


#### FlowExecutionStatus

- FlowExecution의 속성으로 Flow의 실행 후 최종 결과 상태가 무엇인지 정의
- Flow 내 Step이 실행되고 나서 ExitStatus 값을 FlowExecutionStatus 값으로 저장
- FlowJob의 배치 결과 상태에 관여함
- `COMPLETED`, `STOPPED`, `FAILED`, `UNKNOWN`


#### Transition

- Flow 내 Step의 조건부 전환(전이)를 정의함
- Job의 API 설정에서 on(String pattern) 메서드를 호출하면 TransitionBuilder가 반환되어 Transition Flow를 구성할 수 있다
- Step의 종료상태가(ExitStatus) 어떤 pattern 과도 매칭되지 않으면 스프링 배치에서 예외를 발생하고 Job은 실패한다
- transition은 구체적인 것부터 그렇지 않은 순서로 적용된다


- on(String pattern)
  - Step의 실행 결과로 돌려받는 종료상태(ExitStatus)와 매칭되는 패턴 스키마, BatchStatus와 매칭되는 것이 아니다
  - pattern과 ExitStatus 와 매칭이되면 다음으로 실행할 Step을 지정할 수 있다.
  - 특수문자는 두 가지만 허용
    - `*`: 0개 이상의 문자와 매칭,모든 ExitStatus와 매칭된다.
    - `?`: 정확히 1개의 문자와 매칭
    - 예) "c*t"는 "cat"과 "count"에 매칭이 되고, "c?t"는 "cat"에는 매칭되지만 "count"에는 매치되지 않는다
- to()
  - 다음으로 실행할 단계를 지정
- from()
  - 이전 단계에서 정의한 Transition을 새롭게 추가 정의한다

Flow가 실행되면 FlowExecutionStatus에 상태값이 저장되고 최종적으로 Job의 BatchStatus와 ExitStatus에 반영된다 <br>
Step의 BatchStatus 및 ExitStatus에는 아무런 영향을 주지 않고 Job의 상태만을 변경한다

- stop()
  - FlowExecutionStatus가 `STOPPED` 상태로 종료되는 transition
  - Job의 BatchStatus와 ExitStatus가 `STOPPED` 로 **종료**가된다
- fail()
  - FlowExecutionStatus가 `FAILED` 상태로 종료되는 transition
  - Job의 BatchStatus와 ExitStatus가 `FAILED`로 종료된다
- end()
  - FlowExecutionStatus가 `COMPLETED` 상태로 종료되는 transition
  - Job의 BatchStatus와 ExitStatus가 `COMPLETED` 로 종료된다
  - Step의 ExitStatus가 FAILED 이더라도 Job의 BatchStatus 가 COMPLETED로 종료되하도록 가능하며 이 때 Job의 재시작은 불가능하다
- stopAndRestart(Step or Flow or JobExecutionDecider)
  - stop() transition 과 기본 흐름은 동일
  - 특정 step에서 작업을 중단하도록 설정하면 중단 이전의 Step만 COMPLETED 저장이 되고 이후의 Step은 실행되지 않고 STOPPED 상태로 Job이 종료된다
  - Job이 다시 실행됐을 때 실행해야 할 step을 restart 인자로 넘기면 이전에 COMPLETED로 저장된 step은 건너뛰고 중단 이후 Step부터 시작한다



#### Custom ExitStatus

- ExitStatus에 존재하지 않는 exitCode를 새롭게 정의해서 설정
- StepExecutionListener의 afterStep() 메서드에서 Custom exitCode 생성 후 새로운 ExitStatus 반환
- Step 실행 후 완료 시점에서 현재 exitCode를 사용자 정의 exitCode로 수정할 수 있다



#### JobExecutionDecider

- ExitStatus를 조작하거나 StepExecutionListener를 등록할 필요 없이 Transition 처리를 위한 전용 클래스
- Step 과 Transition 역할을 명확히 분리해서 설정할 수 있다
- Step의 ExitStatus가 아닌 JobExecutionDecider 의 FlowExecutionStatus 상태값을 새롭게 설정해서 반환한다



#### SimpleFlow 

- 스프링 배치에서 제공하는 Flow의 구현체로서 각 요소 (Step, FLow JobExecutionDecider) 들을 담고 있는 State를 실행시키는 도메인 객체이다
- FlowBuilder 를 사용해서 생성하며 Transition(조건 흐름을 구성할 수 있는 요소) 과 조합하여 여러개의 Flow 및 중첩 Flow 를 만들어 Job 을 구성할 수 있다



### FlowStep

Step 내에 Flow를 할당하여 실행시키는 도메인 객체 FlowStep 의 BatchStatus 와 ExitStatus 는 Flow의 최종 상태값에 따라 결정된 



#### Scope

- 스프링 컨테이너에서 빈이 관리되는 범위
- singleton(메모리에 하나에 객체만 올라가서 참조), prototype(객체가 참조될 때 마다 생성), request(요청과 응답에 대한 생명주기를 가짐), session(사용자의 세션범위), application(글로벌한 특징) 있으며 기본은 singleton으로 생성된다

#### 스프링 배치 스코프

@JobScope, @StepScope
- Job과 Step의 빈 생성과 실행에 관여하는 스코프
- 프록시 모드를 기본값으로 하는 스코프 - @Scope(value = "job", proxyMode = ScopedProxyMode.TARGET_CLASS)
- 해당 스코프가 선언되면 빈이 생성이 애플리케이션 구동시점이 아닌 빈의 실행시점에 이루어진다
  - @Values를 주입해서 빙늬 실행 시점에 값을 참조할 수 있으며 일종의 Lazy Binding이 가능해 진다.
  - @Value("#{jobParameters[파라미터명]}"), @Value("#{jobExecutionContext[파라미터명]}"), @Value("#{stepExecutionContext[파라미터명]}")
  - @Values를 사용할 경우 빈 선언문에 @JobScope, @StepScope를 정의하지 않으면 오류를 발생하므로 반드시 선언해야 한다
- 프록시 모드로 빈이 선언되기 때문에 애플리케이션 구동시점에는 빈의 프록시 객체가 생성되어 실행 시점에 실제 빈을 호출해 준다
- 병렬처리 시 각 스레드 마다 생성된 스코프 빈이 할당되기 떄문에 스레드에 안전하게 실행이 가능하다

#### @JobScope
- Step 선언문에 정의한다 (Step 빈을 만들 때 위에 추가하면됨)
- @Value : jobParameter, jobExecutionContext 만 사용가능


#### @StepScope
- Tasklet이나 ItemReader, ItemWriter, ItemProcessor 선언문에 정의한다
- @Value : jobParameter, jobExecutionContext, stepExecutionContext 사용가능


### 아키텍처
1. Proxy 객체 생성
   - @JobScope, @StepScope 애너테이션이 붙은 빈 선언은 내부적으로 빈의 Proxy 객체가 생성된다
     - @JobScope
       - @Scope(value = "job", proxyMode = ScopedProxyMode.TARGET_CLASS) (클래스 타입으로 프록시 객체가 생성됨(인터페이스 타입도 있음))
     - @StepScope
       - @Scope(value = "step", proxyMode = ScopedProxyMode.TARGET_CLASS)
   - Job 실행 시 Proxy 객체가 실제 빈을 호출해서 해당 메서드를 실행시키는 구조 (AOP 방식)
2. JobScope, StepScope 클래스
   - Proxy 객체의 실제 대상이 되는 Bean을 등록, 해제하는 역할
   - 실제 빈을 저장하고 있는 JobContext, StepContext 를 가지고 있다. (스프링에 ApplicationContext 와 유사한 개념)
3. JobContext, StepContext
   - 스프링 컨테이너에서 생성된 빈을 저장하는 컨텍스트 역할
   - Job의 실행 시점에서 프록시 객체가 실제 빈을 참조할 때 사용된다

