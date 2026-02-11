# domain-lending-loan-servicing

Domain layer microservice for loan servicing within the Firefly lending platform. This service orchestrates the full post-origination loan lifecycle -- from disbursement and amortization schedule generation through repayments, interest accrual, rate changes, payment holidays, arrears management, charge-offs, and loan closure -- using saga-based workflows with compensation support.

## Overview

The Lending Domain Loan Servicing service acts as the domain orchestration layer between API consumers and the core lending servicing persistence layer. It receives high-level business commands (e.g., "disburse loan", "apply repayment", "charge-off") and coordinates them through the FireflyFramework transactional saga engine. The service covers the entire post-disbursement loan lifecycle including interest management, delinquency handling, restructuring, and final closure.

### Key Features

- **Complete loan servicing lifecycle** -- submit loan cases, disburse, generate schedules, process repayments and prepayments, accrue and capitalize interest, manage rate changes, handle payment holidays, manage arrears, charge-off, write-back, and close loans
- **Saga-based orchestration** -- loan case submission runs as a multi-step compensating saga with automatic rollback on failure
- **CQRS architecture** -- strict separation of command and query responsibilities with dedicated buses, configurable timeouts, and query-level caching
- **Event-driven integration** -- saga steps emit domain events to Kafka (`domain-layer` topic) via the FireflyFramework EDA module
- **Reactive non-blocking I/O** -- built on Spring WebFlux with Project Reactor for end-to-end asynchronous processing
- **Virtual threads** -- Java virtual threads are enabled for improved concurrency under load
- **OpenAPI documentation** -- auto-generated Swagger UI for interactive API exploration (non-production profiles)

## Architecture

### Module Structure

| Module | Purpose |
|--------|---------|
| `domain-lending-loan-servicing-web` | Spring Boot application entry point, REST controller, actuator, and OpenAPI configuration |
| `domain-lending-loan-servicing-core` | Business logic: CQRS commands, command handlers, saga workflow definitions, and the `LoanServicingService` |
| `domain-lending-loan-servicing-interfaces` | Interface adapters bridging core logic and the web layer |
| `domain-lending-loan-servicing-infra` | Infrastructure concerns: `ClientFactory` for downstream API clients, `LoanServicingProperties` configuration binding |
| `domain-lending-loan-servicing-sdk` | Client SDK module for consumers of this service |

### Technology Stack

- **Language:** Java 25
- **Framework:** Spring Boot with Spring WebFlux (reactive)
- **Parent POM:** [FireflyFramework Parent](https://github.com/fireflyframework/) (`fireflyframework-parent 1.0.0-SNAPSHOT`)
- **BOM:** FireflyFramework BOM `26.01.01`
- **Key FireflyFramework modules:**
  - `fireflyframework-web` -- common web layer configuration
  - `fireflyframework-domain` -- domain layer abstractions
  - `fireflyframework-utils` -- shared utilities
  - `fireflyframework-validators` -- validation support
- **CQRS:** FireflyFramework CQRS (`CommandBus`, `@CommandHandlerComponent`)
- **Saga Engine:** FireflyFramework Transactional Saga Engine (`@Saga`, `@SagaStep`, `@StepEvent`, `SagaEngine`, `SagaContext`)
- **Event-Driven Architecture:** FireflyFramework EDA with Kafka publisher
- **Mapping:** MapStruct with Lombok binding
- **API Documentation:** SpringDoc OpenAPI (WebFlux starter)
- **Metrics:** Micrometer with Prometheus registry

### Saga Workflows

| Saga | Description | Steps |
|------|-------------|-------|
| `RegisterLoanServicingSaga` | Orchestrates full loan case submission | Register loan servicing case, then in parallel: loan accrual, loan disbursement, loan rate change, loan repayment record, loan repayment schedule, and loan servicing event (all with compensation) |

### Downstream API Clients

The `ClientFactory` provisions the following API clients that call the core lending servicing persistence layer:

- `LoanServicingCaseApi`
- `LoanAccrualApi`
- `LoanDisbursementApi`
- `LoanRateChangeApi`
- `LoanRepaymentRecordApi`
- `LoanRepaymentScheduleApi`
- `LoanServicingEventApi`

## Setup

### Prerequisites

- **Java 25** (or later)
- **Apache Maven 3.9+**
- **Apache Kafka** running on `localhost:9092` (for event publishing)
- **Core Lending Servicing service** running on `http://localhost:8082` (downstream persistence layer)

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_ADDRESS` | `localhost` | Bind address for the HTTP server |
| `SERVER_PORT` | `8080` | HTTP server port |

### Configuration Highlights (application.yaml)

```yaml
firefly:
  cqrs:
    enabled: true
    command:
      timeout: 30s
      metrics-enabled: true
      tracing-enabled: true
    query:
      timeout: 15s
      caching-enabled: true
      cache-ttl: 15m

  eda:
    enabled: true
    default-publisher-type: KAFKA
    publishers:
      kafka:
        default:
          default-topic: domain-layer
          bootstrap-servers: localhost:9092

api-configuration:
  core-lending.loan-servicing:
    base-path: http://localhost:8082
```

### Spring Profiles

| Profile | Behavior |
|---------|----------|
| `dev` | DEBUG logging for `com.firefly`, R2DBC, and Flyway; Swagger UI enabled |
| `testing` | DEBUG logging for `com.firefly`; Swagger UI enabled |
| `prod` | WARN-level logging; Swagger UI and API docs disabled |

### Build

```bash
mvn clean install
```

### Run

```bash
mvn -pl domain-lending-loan-servicing-web spring-boot:run
```

Or with a specific profile:

```bash
mvn -pl domain-lending-loan-servicing-web spring-boot:run -Dspring-boot.run.profiles=dev
```

## API Endpoints

All endpoints are served under the base path `/api/v1/loans`.

### Loan Case Management

| Method | Path | Summary | Description |
|--------|------|---------|-------------|
| `POST` | `/api/v1/loans` | Submit loan case | Submit a new loan servicing case with accrual, disbursement, rate change, repayment record, repayment schedule, and servicing event data |
| `GET` | `/api/v1/loans/{loanId}` | Get loan details | Retrieve loan details and schedule (projection) |

### Disbursement and Schedule

| Method | Path | Summary | Description |
|--------|------|---------|-------------|
| `POST` | `/api/v1/loans/{loanId}/disburse` | Disburse loan | Disburse to target account on value date and update principal |
| `POST` | `/api/v1/loans/{loanId}/schedule` | Generate schedule | Generate amortization schedule (French, German, or other method) |

### Repayments

| Method | Path | Summary | Description |
|--------|------|---------|-------------|
| `POST` | `/api/v1/loans/{loanId}/repayments` | Apply repayment | Apply repayment following interest, penalties, fees, then principal order |
| `POST` | `/api/v1/loans/{loanId}/prepayment` | Apply prepayment | Apply early principal reduction; recalculate or shorten per policy |

### Interest Management

| Method | Path | Summary | Description |
|--------|------|---------|-------------|
| `POST` | `/api/v1/loans/{loanId}/interest/accrue` | Accrue interest | Accrue interest for a specified period |
| `POST` | `/api/v1/loans/{loanId}/interest/capitalize` | Capitalize interest | Capitalize past-due interest into principal |

### Penalties

| Method | Path | Summary | Description |
|--------|------|---------|-------------|
| `POST` | `/api/v1/loans/{loanId}/penalties` | Generate penalties | Generate late fees and penalties for a specified period |

### Rate Management

| Method | Path | Summary | Description |
|--------|------|---------|-------------|
| `POST` | `/api/v1/loans/{loanId}/rate` | Set fixed rate | Set a new fixed rate effective from a given date |
| `POST` | `/api/v1/loans/{loanId}/index` | Change reference index | Change the reference index and spread for a variable rate loan |

### Payment Holidays

| Method | Path | Summary | Description |
|--------|------|---------|-------------|
| `POST` | `/api/v1/loans/{loanId}/payment-holiday/grant` | Grant payment holiday | Grant a payment holiday and adjust schedule/accrual accordingly |
| `POST` | `/api/v1/loans/{loanId}/payment-holiday/end` | End payment holiday | End the holiday and resume normal accrual |

### Restructuring

| Method | Path | Summary | Description |
|--------|------|---------|-------------|
| `POST` | `/api/v1/loans/{loanId}/restructure` | Apply restructuring | Apply restructuring (tenor, rate, write-offs) with a new schedule |

### Arrears and Delinquency

| Method | Path | Summary | Description |
|--------|------|---------|-------------|
| `POST` | `/api/v1/loans/{loanId}/arrears/mark` | Mark in arrears | Flag loan as in arrears as of a business date |
| `POST` | `/api/v1/loans/{loanId}/arrears/normalize` | Normalize arrears | Return loan to current status |
| `POST` | `/api/v1/loans/{loanId}/charge-off` | Charge-off loan | Charge-off to losses when delinquency policy is met |
| `POST` | `/api/v1/loans/{loanId}/write-back` | Write back | Write back a previously charged-off amount |

### Loan Closure

| Method | Path | Summary | Description |
|--------|------|---------|-------------|
| `POST` | `/api/v1/loans/{loanId}/close` | Close loan | Close the loan with zero exposure and no pending installments |

### Interactive API Documentation

When running in non-production profiles, Swagger UI is available at:

- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

## Development Guidelines

### Project Conventions

- **CQRS pattern:** All write operations are modeled as `Command` objects dispatched through the `CommandBus`
- **Saga orchestration:** Multi-step operations must be defined as `@Saga` classes with `@SagaStep` methods and corresponding compensate methods
- **Step events:** Every saga step should emit a `@StepEvent` for downstream event consumers
- **Constants:** Saga names, step IDs, compensate method names, and event types are centralized in `RegisterLoanServicingConstants` and `GlobalConstants`
- **DTOs as command bases:** Command classes extend SDK-generated DTOs (e.g., `RegisterLoanServicingCaseCommand extends LoanServicingCaseDTO`) to avoid redundant field definitions
- **Handlers:** Command handlers are annotated with `@CommandHandlerComponent` and extend `CommandHandler<C, R>`

### Adding a New Operation

1. Define a `Command` class in the `commands` package
2. Create a `CommandHandler` in the `handlers` package annotated with `@CommandHandlerComponent`
3. If the operation is multi-step, define a new `@Saga` class in the `workflows` package with steps, compensations, and events
4. Add constants for the saga, steps, compensations, and events to the appropriate constants class
5. Add the method signature to `LoanServicingService` and implement it in `LoanServicingServiceImpl`
6. Expose the operation through the `LoanServicingController`

### Domain Events

The `RegisterLoanServicingSaga` emits the following domain events via Kafka:

| Event | Trigger |
|-------|---------|
| `loanServicing.registered` | Loan servicing case created |
| `loanAccrual.registered` | Loan accrual record created |
| `loanDisbursement.registered` | Loan disbursement record created |
| `loanRateChange.registered` | Loan rate change record created |
| `loanRepaymentRecord.registered` | Loan repayment record created |
| `loanRepaymentSchedule.registered` | Loan repayment schedule created |
| `loanServicingEvent.registered` | Loan servicing event created |

## Monitoring

### Actuator Endpoints

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Health check with liveness and readiness probes (detailed view) |
| `/actuator/info` | Application information |
| `/actuator/prometheus` | Prometheus-compatible metrics (CQRS command/query metrics included) |

### Health Probes

Kubernetes-compatible liveness and readiness probes are enabled:

- **Liveness:** `/actuator/health/liveness`
- **Readiness:** `/actuator/health/readiness`

### Logging

Structured console logging with pattern: `%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n`

## License

This project is licensed under the Apache License 2.0. See [LICENSE](LICENSE) for details.

## Links

- **Repository:** [https://github.com/firefly-oss/domain-lending-loan-servicing](https://github.com/firefly-oss/domain-lending-loan-servicing)
- **FireflyFramework:** [https://github.com/fireflyframework/](https://github.com/fireflyframework/)
- **Team Contact:** [dev@getfirefly.io](mailto:dev@getfirefly.io)
