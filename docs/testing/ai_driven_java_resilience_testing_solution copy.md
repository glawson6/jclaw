# AI-Driven Java Microservice Resilience Testing Platform

## 1. Executive Summary

This document defines a comprehensive architecture for an AI-driven resilience and chaos-testing platform for Java microservices.

The initial system under test is:

```text
JMeter → API A → Backend B Simulator
```

API A is a Java/Spring Boot microservice. Backend B is a controllable simulator representing a troubled or unreliable downstream service. JMeter provides load and request generation. Resilience4j is introduced into API A after establishing a baseline. JaiClaw serves as the AI/LLM harness and uses MCP-style tools to control traffic, inject faults, observe the system, analyze results, and progressively design experiments.

The key architectural principle is:

> Do not make JaiClaw a JMeter-script generator. Make it an experiment orchestrator that operates through safe, domain-specific tools.

The resulting platform can evolve from deterministic performance/resilience tests into an AI-assisted experiment system capable of answering questions such as:

- How does API A behave when Backend B becomes slow?
- How much downstream failure can API A tolerate?
- Does the Resilience4j circuit breaker open at the expected threshold?
- Are retries creating retry amplification or a retry storm?
- What happens when Backend B returns intermittent 503 responses?
- What is the maximum downstream failure rate that still satisfies the API SLO?
- How does the system recover after Backend B becomes healthy again?
- Which Resilience4j configuration provides the best behavior under a given failure profile?

---

# 2. Goals

## Primary Goals

1. Establish a deterministic baseline for API A.
2. Add Resilience4j to API A and quantify its effect.
3. Provide a configurable Backend B simulator capable of realistic failures.
4. Control JMeter through a safe service/tool abstraction.
5. Expose traffic, fault-injection, observation, and experiment-management tools to JaiClaw.
6. Collect metrics, logs, and traces.
7. Allow JaiClaw to compare experiments.
8. Eventually allow JaiClaw to autonomously design and execute resilience experiments.
9. Make experiments reproducible and auditable.
10. Keep the LLM isolated from arbitrary infrastructure access.

## Non-Goals

Initially, the platform should not:

- Give the LLM unrestricted shell access.
- Allow the LLM to generate arbitrary JMeter JMX files and execute them without validation.
- Allow unrestricted Kubernetes manipulation.
- Treat an LLM-generated conclusion as authoritative without measured evidence.
- Replace deterministic automated tests with AI-only testing.

---

# 3. Core Architecture

## 3.1 Initial System

```text
                    TEST SYSTEM

┌─────────────┐      ┌─────────────┐      ┌─────────────────────┐
│   JMeter    │─────▶│   API A     │─────▶│ Backend B Simulator │
│             │      │             │      │                     │
│ Load        │      │ Spring Boot │      │ Normal              │
│ Generation  │      │             │      │ Slow                │
│             │      │ Resilience4j│      │ Errors              │
└─────────────┘      └─────────────┘      │ Timeout             │
                                          │ Unavailable         │
                                          └─────────────────────┘
```

The first milestone should intentionally be simple.

There should be no AI involved in the first baseline implementation.

---

# 4. Target AI-Driven Architecture

```text
                             ┌──────────────────────────┐
                             │          JaiClaw          │
                             │                          │
                             │ Planner                  │
                             │ Experiment Reasoner      │
                             │ Analyzer                 │
                             │ Report Generator         │
                             └────────────┬─────────────┘
                                          │
                                  MCP / Tool Interface
                                          │
             ┌────────────────────────────┼────────────────────────────┐
             │                            │                            │
             ▼                            ▼                            ▼
    ┌─────────────────┐        ┌─────────────────┐        ┌─────────────────┐
    │ Traffic Tools   │        │ Fault Tools     │        │ Observation     │
    │                 │        │                 │        │ Tools           │
    │ start           │        │ latency         │        │ metrics         │
    │ stop            │        │ errors          │        │ traces          │
    │ set rate        │        │ timeout         │        │ logs            │
    │ concurrency     │        │ disconnect      │        │ resilience      │
    │ randomize       │        │ restore         │        │ comparisons     │
    └────────┬────────┘        └────────┬────────┘        └────────┬────────┘
             │                          │                          │
             ▼                          ▼                          ▼
       ┌───────────┐             ┌───────────────┐          ┌──────────────┐
       │  JMeter   │             │ Backend B     │          │ Observability│
       │ Controller│             │ Simulator     │          │              │
       └─────┬─────┘             └───────────────┘          │ Prometheus   │
             │                                              │ OpenTelemetry│
             ▼                                              │ Grafana      │
       ┌─────────────┐                                      │ Logs/Traces  │
       │   JMeter    │                                      └──────────────┘
       └──────┬──────┘
              │
              ▼
       ┌─────────────┐
       │    API A    │
       │             │
       │ Spring Boot │
       │ Resilience4j│
       └──────┬──────┘
              │
              ▼
       ┌─────────────┐
       │ Backend B   │
       │ Simulator   │
       └─────────────┘

                 ┌───────────────────────────┐
                 │ Experiment Registry       │
                 │                           │
                 │ configurations             │
                 │ hypotheses                 │
                 │ results                    │
                 │ observations               │
                 │ conclusions                │
                 └───────────────────────────┘
```

---

# 5. Architectural Principle: Domain Tools, Not Infrastructure Tools

The LLM should not need to know how JMeter, Kubernetes, Prometheus, or the simulator are implemented.

Avoid exposing:

```text
execute_shell_command()
execute_jmeter()
execute_promql()
kubectl()
```

Prefer:

```text
traffic.start()
traffic.stop()
traffic.set_rate()
traffic.set_concurrency()

fault.set_latency()
fault.set_error_rate()
fault.set_http_status()
fault.set_timeout()
fault.disconnect()
fault.restore()

observe.get_metrics()
observe.get_latency()
observe.get_resilience()
observe.get_backend_state()
observe.get_traces()

experiment.create()
experiment.start()
experiment.stop()
experiment.compare()
```

This creates a stable semantic contract between JaiClaw and the test platform.

---

# 6. Components

## 6.1 JaiClaw

JaiClaw is the AI experiment orchestrator.

Responsibilities:

- Understand the experiment objective.
- Select appropriate tools.
- Establish or retrieve baselines.
- Execute controlled experiments.
- Analyze observations.
- Compare experiments.
- Adjust future experiments.
- Generate conclusions and reports.

JaiClaw should not directly manipulate infrastructure.

---

# 7. Traffic Control Service

A dedicated Java/Spring Boot service should provide an abstraction around JMeter.

```text
JaiClaw
   │
   │ MCP
   ▼
┌─────────────────────────────┐
│ Resilience Test Controller  │
│                             │
│ startTest()                 │
│ stopTest()                  │
│ pauseTest()                 │
│ setRate()                   │
│ setConcurrency()            │
│ randomizeRequests()         │
│ getResults()                │
└──────────────┬──────────────┘
               │
               ▼
            JMeter
```

## Why not have the LLM generate JMX?

A raw JMX-generation approach creates several problems:

- Poor reproducibility.
- Increased security risk.
- Difficulty validating generated tests.
- Coupling the LLM to JMeter implementation details.
- Difficult auditing.
- Potentially dangerous load levels.

Instead, expose a controlled semantic API.

Example:

```json
{
  "rate": 500,
  "durationSeconds": 300,
  "rampSeconds": 60,
  "distribution": "steady"
}
```

The controller translates this into the appropriate JMeter execution.

---

# 8. Backend B Simulator

Backend B should not be a simple mock.

It should be a purpose-built fault simulator.

## Normal Behavior

```text
NORMAL
 ├── configurable latency
 ├── configurable response size
 ├── configurable throughput
 └── configurable response data
```

## Fault Behavior

```text
FAULTS
 ├── HTTP 500
 ├── HTTP 502
 ├── HTTP 503
 ├── HTTP 429
 ├── connection refused
 ├── connection reset
 ├── timeout
 ├── slow response
 ├── malformed response
 ├── intermittent failure
 └── unavailable
```

---

# 9. Fault Injection Model

The simulator should expose a runtime configuration such as:

```json
{
  "scenario": "degraded-backend",
  "latencyMs": 2000,
  "errorRate": 0.30,
  "errorCode": 503,
  "timeoutRate": 0.05,
  "durationSeconds": 120
}
```

The simulator should expose APIs such as:

```text
GET  /fault/state
POST /fault/latency
POST /fault/error-rate
POST /fault/http-status
POST /fault/timeout
POST /fault/disconnect
POST /fault/restore
POST /fault/scenario
```

These HTTP APIs can then be wrapped by MCP tools.

---

# 10. Compound Fault Scenarios

A major feature should be support for time-based scenarios.

Example:

```text
0-30 seconds
    Normal

30-60 seconds
    latency = 500 ms

60-90 seconds
    latency = 2,000 ms
    error rate = 10%

90-120 seconds
    error rate = 50%

120 seconds
    restore
```

This allows realistic degradation testing rather than a binary healthy/unhealthy model.

---

# 11. API A

API A is the system under test.

Example:

```text
JMeter
   │
   ▼
API A
   │
   ├── Spring Boot
   │
   ├── REST API
   │
   └── Resilience4j
          ├── Retry
          ├── CircuitBreaker
          ├── TimeLimiter
          ├── Bulkhead
          └── RateLimiter
   │
   ▼
Backend B
```

The system should support running API A in at least two configurations:

```text
Configuration A:
    Resilience4j OFF

Configuration B:
    Resilience4j ON
```

This makes direct comparison possible.

---

# 12. Observability

Observability is a first-class component.

Recommended architecture:

```text
API A ─────────────┐
Backend B ─────────┤
JMeter ────────────┤
                   ▼
          ┌─────────────────┐
          │ Observability   │
          │                 │
          │ Metrics         │
          │ Logs            │
          │ Traces          │
          └────────┬────────┘
                   │
          ┌────────┼──────────┐
          ▼        ▼          ▼
     Prometheus   OTEL      Logs
                   │
                Grafana
```

Metrics should include:

- Request count.
- Success count.
- Error count.
- Error percentage.
- Throughput.
- p50 latency.
- p90 latency.
- p95 latency.
- p99 latency.
- Backend latency.
- Backend errors.
- Retry count.
- Circuit breaker state.
- Circuit breaker failure rate.
- Bulkhead saturation.
- TimeLimiter events.
- RateLimiter events.

---

# 13. Resilience4j Metrics

Resilience4j integrates with Micrometer.

Important measurements include:

```text
CircuitBreaker:
    calls
    failures
    slow calls
    state

Retry:
    retry attempts
    successful calls
    failed calls

Bulkhead:
    available concurrent calls
    rejected calls

RateLimiter:
    available permissions
    rejected calls
```

These measurements are extremely valuable to JaiClaw because they provide direct evidence of resilience behavior.

---

# 14. Observation Tools

The AI should have tools such as:

```text
observe.get_api_metrics()

observe.get_backend_metrics()

observe.get_resilience_metrics()

observe.get_latency_percentiles()

observe.get_error_rates()

observe.get_circuit_breaker_state()

observe.get_retry_statistics()

observe.get_bulkhead_statistics()

observe.get_trace_summary()

observe.find_errors()

observe.compare_experiments()
```

The tools should return normalized, concise data.

For example:

```json
{
  "requests": 100000,
  "successful": 98650,
  "failed": 1350,
  "errorRate": 0.0135,
  "p50Ms": 42,
  "p95Ms": 481,
  "p99Ms": 1202,
  "retryAttempts": 15320,
  "circuitBreaker": "OPEN"
}
```

---

# 15. Experiment Registry

An Experiment Registry should be introduced early.

Suggested model:

```text
Experiment
──────────────
id
name
description
hypothesis

trafficConfiguration
faultConfiguration
apiConfiguration
resilienceConfiguration

startTime
endTime

baselineExperimentId
parentExperimentId

metrics
observations
analysis
conclusion
```

This gives JaiClaw persistent experiment context.

---

# 16. Experiment Lifecycle

```text
                 ┌──────────────────┐
                 │ Define hypothesis│
                 └────────┬─────────┘
                          ▼
                 ┌──────────────────┐
                 │ Establish        │
                 │ baseline         │
                 └────────┬─────────┘
                          ▼
                 ┌──────────────────┐
                 │ Inject fault     │
                 └────────┬─────────┘
                          ▼
                 ┌──────────────────┐
                 │ Generate traffic │
                 └────────┬─────────┘
                          ▼
                 ┌──────────────────┐
                 │ Observe system   │
                 └────────┬─────────┘
                          ▼
                 ┌──────────────────┐
                 │ Analyze results  │
                 └────────┬─────────┘
                          ▼
                 ┌──────────────────┐
                 │ Compare baseline │
                 └────────┬─────────┘
                          ▼
                 ┌──────────────────┐
                 │ Adjust experiment│
                 └────────┬─────────┘
                          │
                          └───────▶ Repeat
```

---

# 17. Baseline Testing

Baseline must be established before resilience behavior is evaluated.

## Experiment 001

```text
API A:
    Resilience4j OFF

Backend B:
    Healthy

Traffic:
    100 requests/sec
    10 minutes
```

Example result:

```text
p50       32 ms
p95       71 ms
p99       103 ms
errors    0.01%
throughput 99.8 req/sec
```

## Experiment 002

Same experiment with Resilience4j enabled.

This allows the system to determine whether resilience configuration itself introduces measurable overhead.

---

# 18. Fault Experiment

Example:

```text
API A:
    Resilience4j ON

Backend B:
    20% HTTP 503

Traffic:
    100 requests/sec
```

The experiment should capture:

```text
Incoming API requests
Backend requests
Backend failures
Retries
Circuit breaker state
API failures
Latency
```

This is important because retry behavior can dramatically change downstream load.

---

# 19. Retry Amplification

One of the most important experiments is detecting retry amplification.

Example:

```text
JMeter
100 requests/sec
        │
        ▼
API A
Retry × 3
        │
        ▼
Backend B
```

Potential result:

```text
Original requests: 100/sec
Retry requests:     200/sec
Backend traffic:    300/sec
```

The system can calculate:

```text
Retry Amplification =
Backend Requests / Original Requests
```

For example:

```text
300 / 100 = 3.0x
```

JaiClaw can identify this behavior and determine whether retry policy is making the incident worse.

---

# 20. Circuit Breaker Experiments

The platform should test:

- Time before circuit opens.
- Number of calls before opening.
- Failure threshold.
- Slow-call threshold.
- Half-open behavior.
- Recovery behavior.
- Number of rejected calls.
- Downstream load after opening.
- Interaction between Retry and CircuitBreaker.

Example:

```text
Backend failure rate
        │
        ▼
Circuit Breaker
        │
        ▼
API behavior
```

The AI can correlate these events.

---

# 21. AI-Driven Threshold Discovery

This is one of the most valuable future capabilities.

Suppose the objective is:

```text
SLO:
p95 < 500 ms
error rate < 1%
```

JaiClaw can execute:

```text
10% backend failure
    ↓
observe

20%
    ↓
observe

30%
    ↓
observe

40%
    ↓
observe

50%
    ↓
observe
```

It might discover:

```text
Backend Failure Rate     SLO
--------------------------------
0%                       PASS
10%                      PASS
20%                      PASS
30%                      PASS
40%                      FAIL
50%                      FAIL
```

It can then narrow the search:

```text
32%
34%
36%
38%
```

Eventually:

```text
Maximum observed sustainable failure rate:
approximately 34%
```

This turns resilience testing into an empirical search problem.

---

# 22. Recommended Tool Taxonomy

## Traffic Tools

```text
traffic.start
traffic.stop
traffic.pause
traffic.set_rate
traffic.set_concurrency
traffic.set_duration
traffic.randomize_requests
traffic.get_status
traffic.get_results
```

## Fault Tools

```text
fault.get_state
fault.set_latency
fault.set_error_rate
fault.set_http_status
fault.set_timeout
fault.disconnect
fault.restore
fault.start_scenario
fault.stop_scenario
```

## Observation Tools

```text
observe.get_api_metrics
observe.get_backend_metrics
observe.get_resilience_metrics
observe.get_latency
observe.get_error_rate
observe.get_circuit_breaker
observe.get_retry_statistics
observe.get_bulkhead_statistics
observe.get_traces
observe.find_errors
```

## Experiment Tools

```text
experiment.create
experiment.start
experiment.stop
experiment.get
experiment.list
experiment.compare
experiment.save_baseline
experiment.analyze
```

---

# 23. MCP Architecture

The tools can be organized into separate MCP servers or logical tool domains.

```text
                       JaiClaw
                          │
                       MCP
                          │
          ┌───────────────┼────────────────┐
          │               │                │
          ▼               ▼                ▼
     Traffic MCP      Fault MCP       Observe MCP
          │               │                │
          ▼               ▼                ▼
       JMeter         Backend B         Metrics
                                       Traces
                                       Logs

                          │
                          ▼
                   Experiment MCP
                          │
                          ▼
                  Experiment Registry
```

Initially, these can even live in one Spring Boot application. They should still be separated logically by package/interface.

Later they can become independently deployable services.

---

# 24. Suggested Java Project Structure

A practical initial implementation could be:

```text
resilience-test-platform/
│
├── test-control/
│   ├── traffic/
│   ├── fault/
│   ├── observation/
│   └── experiment/
│
├── backend-simulator/
│   ├── api/
│   ├── fault/
│   ├── scenario/
│   └── metrics/
│
├── api-a/
│   ├── controller/
│   ├── service/
│   ├── resilience/
│   └── observability/
│
├── jmeter/
│   ├── test-plans/
│   └── extensions/
│
├── experiment-registry/
│
└── jaiclaw/
    └── mcp-tools/
```

A multi-module Maven build would work well initially.

---

# 25. Recommended Technology Stack

## API A

- Java 21+
- Spring Boot
- Spring Web
- Resilience4j
- Micrometer
- OpenTelemetry
- Maven

## Backend Simulator

- Java 21+
- Spring Boot
- Spring WebFlux or Spring MVC
- Configurable fault engine
- Micrometer

## Test Control

- Java 21+
- Spring Boot
- JMeter integration
- MCP interfaces

## Observability

- Prometheus
- Grafana
- OpenTelemetry
- OpenTelemetry Collector
- Centralized logs as appropriate

## AI

- JaiClaw
- MCP
- Selected LLM provider

## Deployment

Initially:

```text
Docker Compose
```

Later:

```text
Kubernetes
```

---

# 26. Guided Implementation Plan

## Phase 0 — Repository and Architecture

Create the project structure.

Tasks:

- [ ] Create Maven parent project.
- [ ] Create API A module.
- [ ] Create Backend Simulator module.
- [ ] Create Test Control module.
- [ ] Create Experiment Registry module.
- [ ] Create JMeter test plans.
- [ ] Create Docker Compose environment.
- [ ] Define API contracts.

Deliverable:

```text
JMeter → API A → Backend B
```

running locally.

---

# 27. Phase 1 — Build API A

Implement a simple API.

Example:

```text
GET /api/v1/data/{id}
```

API A calls:

```text
GET /backend/data/{id}
```

Backend B returns a normal response.

Add:

- Request logging.
- Micrometer metrics.
- OpenTelemetry tracing.
- Correlation/request ID.

Success criteria:

```text
JMeter
   ↓
API A
   ↓
Backend B
```

works reliably.

---

# 28. Phase 2 — Build Baseline JMeter Test

Create a deterministic JMeter test.

Start with:

```text
100 requests/sec
10 minutes
```

Capture:

- Throughput.
- Error rate.
- p50.
- p95.
- p99.
- Backend request rate.

Store the results as Experiment 001.

Do not introduce randomness yet.

---

# 29. Phase 3 — Build Backend Simulator

Implement:

```text
Normal
Latency
HTTP Error
Error Rate
Timeout
Disconnect
Restore
Scenario
```

Example:

```http
POST /fault/latency

{
  "latencyMs": 2000
}
```

Then:

```http
POST /fault/error-rate

{
  "errorRate": 0.30,
  "status": 503
}
```

Success criteria:

Backend behavior can be changed without restarting the application.

---

# 30. Phase 4 — Add Resilience4j

Introduce:

```text
CircuitBreaker
Retry
TimeLimiter
Bulkhead
RateLimiter
```

Do not enable everything simultaneously.

Recommended progression:

1. CircuitBreaker.
2. Retry.
3. TimeLimiter.
4. Bulkhead.
5. RateLimiter.

This allows each behavior to be characterized independently.

---

# 31. Phase 5 — Establish Resilience Baselines

Create controlled experiments.

### Experiment A

```text
Healthy backend
Resilience OFF
```

### Experiment B

```text
Healthy backend
Resilience ON
```

### Experiment C

```text
20% backend failures
Resilience OFF
```

### Experiment D

```text
20% backend failures
CircuitBreaker ON
```

### Experiment E

```text
20% backend failures
CircuitBreaker + Retry
```

Compare every experiment against its baseline.

---

# 32. Phase 6 — Observability

Deploy:

```text
Prometheus
Grafana
OpenTelemetry Collector
```

Create dashboards for:

```text
API A
Backend B
JMeter
Resilience4j
```

Important Grafana panels:

```text
Request Rate
Error Rate
p50
p95
p99

Backend Request Rate
Backend Error Rate

Circuit Breaker State
Retry Attempts
Bulkhead Rejections

API → Backend Trace Latency
```

---

# 33. Phase 7 — Build Tool Layer

Create Java interfaces first.

Example:

```java
public interface TrafficController {
    ExperimentId start(TrafficConfiguration configuration);
    void stop(ExperimentId experimentId);
    TrafficStatus status(ExperimentId experimentId);
}
```

```java
public interface FaultController {
    FaultState getState();
    void setLatency(Duration latency);
    void setErrorRate(double errorRate);
    void restore();
}
```

```java
public interface ObservationService {
    ApiMetrics getApiMetrics();
    BackendMetrics getBackendMetrics();
    ResilienceMetrics getResilienceMetrics();
}
```

```java
public interface ExperimentService {
    Experiment create(ExperimentDefinition definition);
    ExperimentResult compare(String baselineId, String experimentId);
}
```

Then expose these interfaces as MCP tools.

---

# 34. Phase 8 — Connect JaiClaw

Start with human-directed execution.

Example:

```text
User:
Run a 100 requests/sec test against API A
with Backend B configured for 20% 503 responses.
```

JaiClaw should:

```text
1. Configure Backend B.
2. Start JMeter.
3. Wait for stabilization.
4. Collect metrics.
5. Stop JMeter.
6. Restore Backend B.
7. Analyze results.
```

At this stage, the AI should not autonomously change parameters.

---

# 35. Phase 9 — AI-Assisted Analysis

Next allow JaiClaw to analyze results.

Example:

```text
Experiment:
100 req/sec
20% backend failures

Results:
API error rate: 3.1%
p95: 1.2 sec
retry amplification: 2.4x
circuit breaker: OPEN
```

JaiClaw can report:

```text
The circuit breaker successfully opened, but retry
amplification increased downstream traffic by approximately
2.4x before the circuit opened.
```

The conclusion must always reference measured data.

---

# 36. Phase 10 — AI Experiment Planning

Give JaiClaw an objective:

```text
Determine the maximum Backend B failure rate
that API A can tolerate while maintaining:

p95 < 500 ms
error rate < 1%
```

JaiClaw creates experiments automatically.

Potential strategy:

```text
10%
20%
30%
40%
```

Then narrow the range:

```text
30%
35%
40%
```

Then:

```text
32%
34%
36%
```

This is effectively an AI-directed search algorithm.

---

# 37. Phase 11 — Randomized Testing

Only after deterministic experiments are reliable should randomization be enabled.

Possible random variables:

```text
Request ID
Customer ID
Payload size
Endpoint
Request frequency
Latency
Failure rate
Failure type
Concurrency
```

Use bounded randomization.

For example:

```text
latency:       0-3000 ms
error rate:    0-50%
rate:          10-1000 req/sec
payload size:  1KB-1MB
```

Every generated experiment must record its random seed and configuration.

This makes failures reproducible.

---

# 38. Phase 12 — Advanced Autonomous Testing

The final system could support:

```text
User:
Find weaknesses in API A's resilience configuration.
```

JaiClaw:

```text
1. Establish baseline.
2. Discover resilience configuration.
3. Test latency degradation.
4. Test error degradation.
5. Test timeout behavior.
6. Test retry amplification.
7. Test circuit breaker behavior.
8. Test recovery.
9. Test concurrency pressure.
10. Compare results.
11. Identify weaknesses.
12. Recommend configuration changes.
13. Re-test.
```

This becomes an AI-driven resilience laboratory.

---

# 39. Safety Controls

This is essential because the AI controls load generation and fault injection.

Implement hard limits.

Example:

```text
Maximum traffic:
    10,000 req/sec

Maximum duration:
    30 minutes

Maximum concurrency:
    2,000

Maximum backend latency:
    60 seconds

Maximum error rate:
    100%

Allowed targets:
    explicit test environments only
```

The tool layer should enforce these limits independently of the LLM.

Never rely on the model to obey safety constraints.

---

# 40. Experiment Reproducibility

Every experiment should record:

```text
Experiment ID
Git commit
API version
Backend simulator version
JMeter version
JaiClaw version
LLM/model
Prompt/objective
Traffic configuration
Fault configuration
Resilience configuration
Random seed
Start/end time
Metrics
Conclusion
```

This makes the system useful for engineering evidence rather than merely experimentation.

---

# 41. Example AI Experiment

User objective:

```text
Determine whether API A can tolerate a 30% Backend B failure rate.
```

JaiClaw:

```text
1. Retrieve baseline.
2. Verify Backend B is healthy.
3. Configure Backend B:
       errorRate = 0.30
       status = 503
4. Configure JMeter:
       rate = 100/sec
       duration = 300 sec
5. Start test.
6. Monitor:
       p95
       error rate
       retries
       circuit breaker
       backend load
7. Stop test.
8. Restore Backend B.
9. Compare with baseline.
```

Example analysis:

```text
Baseline:
p95 = 71ms
error rate = 0.01%

Fault:
p95 = 486ms
error rate = 0.8%
retry amplification = 1.9x
circuit breaker = OPEN

Conclusion:
The resilience configuration maintained the target SLO during
the 30% backend failure experiment. However, retry amplification
increased backend traffic by approximately 1.9x before circuit
breaker activation.
```

---

# 42. Advanced Architecture: Experiment State Machine

The Experiment Service should eventually model experiments as a state machine.

```text
CREATED
   │
   ▼
VALIDATED
   │
   ▼
PREPARING
   │
   ▼
RUNNING
   │
   ▼
COLLECTING
   │
   ▼
ANALYZING
   │
   ▼
COMPLETED
```

Failure paths:

```text
PREPARING ──▶ FAILED
RUNNING ────▶ FAILED
COLLECTING ─▶ FAILED
ANALYZING ──▶ FAILED
```

This prevents the LLM from getting confused about partial experiments.

---

# 43. Recommended Repository Architecture

A practical Git repository could look like:

```text
resilience-ai-lab/
│
├── README.md
├── pom.xml
│
├── api-a/
│   ├── pom.xml
│   └── src/
│
├── backend-simulator/
│   ├── pom.xml
│   └── src/
│
├── test-control/
│   ├── pom.xml
│   └── src/
│
├── experiment-registry/
│   ├── pom.xml
│   └── src/
│
├── jmeter/
│   ├── baseline.jmx
│   ├── resilience.jmx
│   └── randomized.jmx
│
├── observability/
│   ├── prometheus/
│   ├── grafana/
│   └── otel/
│
├── docker/
│   └── compose/
│
└── docs/
    ├── architecture.md
    ├── experiments.md
    └── tool-contracts.md
```

---

# 44. Initial Docker Compose Architecture

```text
┌─────────────────────────────────────────────────────────┐
│                    Docker Compose                        │
│                                                         │
│  ┌─────────────┐     ┌─────────────┐                   │
│  │   JMeter    │────▶│    API A    │                   │
│  └─────────────┘     └──────┬──────┘                   │
│                             │                           │
│                             ▼                           │
│                      ┌─────────────┐                    │
│                      │ Backend B   │                    │
│                      │ Simulator   │                    │
│                      └─────────────┘                    │
│                                                         │
│  ┌─────────────┐     ┌─────────────┐                   │
│  │ Prometheus  │────▶│   Grafana   │                   │
│  └─────────────┘     └─────────────┘                   │
│                                                         │
│  ┌───────────────────────────────┐                      │
│  │ Test Control / MCP Service    │                      │
│  └───────────────────────────────┘                      │
└─────────────────────────────────────────────────────────┘
```

JaiClaw can initially run outside the Compose environment and connect to the Test Control/MCP service.

---

# 45. Kubernetes Evolution

Once the local platform works:

```text
                     Kubernetes
┌─────────────────────────────────────────────────────┐
│                                                     │
│  jmeter                                              │
│     │                                               │
│     ▼                                               │
│  api-a ────────────────▶ backend-simulator          │
│    │                                                │
│    ├──────────▶ Prometheus                          │
│    │                                                │
│    └──────────▶ OTEL Collector                      │
│                                                     │
│  test-control                                       │
│                                                     │
│  experiment-registry                                │
│                                                     │
└─────────────────────────────────────────────────────┘
```

JaiClaw remains external to the test cluster and communicates through the MCP/Test Control boundary.

This separation is desirable because the LLM should not require Kubernetes credentials.

---

# 46. Key Design Decisions

## Decision 1

**JMeter remains the traffic engine.**

Do not replace it with an LLM.

## Decision 2

**Backend B is a programmable simulator.**

Do not depend on an actual broken external system for deterministic tests.

## Decision 3

**The Test Control service abstracts JMeter.**

JaiClaw should express intent rather than JMeter implementation details.

## Decision 4

**Fault injection is a first-class capability.**

Backend degradation should be configurable and reproducible.

## Decision 5

**Observability is part of the experiment.**

A resilience test without measurements is incomplete.

## Decision 6

**Experiments are persisted.**

The AI should be able to compare current behavior with historical experiments.

## Decision 7

**AI autonomy is introduced gradually.**

Start with execution and analysis. Move to experiment planning only after deterministic behavior is proven.

---

# 47. Recommended MVP

The first useful MVP should contain exactly this:

```text
JMeter
   │
   ▼
API A
   │
   ▼
Backend B Simulator

        │
        ▼

Prometheus
Grafana

        │
        ▼

Test Control Service
        │
        ▼
       MCP
        │
        ▼
     JaiClaw
```

And only these initial tools:

```text
traffic.start
traffic.stop
traffic.status

fault.set_latency
fault.set_error_rate
fault.set_http_status
fault.restore

observe.get_metrics
observe.get_resilience

experiment.create
experiment.get
experiment.compare
```

That is enough to prove the entire concept.

---

# 48. MVP Demonstration

A compelling first demonstration would be:

### Step 1

Run baseline:

```text
100 req/sec
Backend healthy
Resilience OFF
```

### Step 2

Enable CircuitBreaker.

### Step 3

Tell JaiClaw:

```text
Run the same experiment with Backend B returning
503 for 20% of requests.
```

### Step 4

JaiClaw executes the experiment.

### Step 5

JaiClaw reports:

```text
Baseline:
p95 = 72ms
error rate = 0.01%

Fault:
p95 = 410ms
error rate = 0.6%

Circuit breaker:
OPEN

Backend failures:
20%

Retry amplification:
1.0x

SLO:
PASS
```

### Step 6

Ask:

```text
Find the highest backend failure rate that maintains
error rate below 1%.
```

JaiClaw then performs multiple experiments.

That would demonstrate the entire platform concept.

---

# 49. Long-Term Vision

The final platform can become a **Resilience Engineering AI Laboratory**.

```text
                       User
                        │
                        ▼
                 ┌──────────────┐
                 │    JaiClaw   │
                 │              │
                 │ Hypothesis   │
                 │ Planning     │
                 │ Execution    │
                 │ Analysis     │
                 │ Learning     │
                 └──────┬───────┘
                        │
                  Experiment API
                        │
       ┌────────────────┼────────────────┐
       ▼                ▼                ▼
    Traffic           Fault          Observation
       │                │                │
       ▼                ▼                ▼
    JMeter          Simulator       Telemetry
       │                │                │
       └────────────────┼────────────────┘
                        ▼
                   API A
                        │
                        ▼
                   Backend B

                        │
                        ▼
                Experiment Registry
```

Eventually the user could state an engineering objective rather than a test script:

> "Determine whether this service can tolerate a degraded downstream dependency while maintaining its SLO."

JaiClaw could then:

1. Understand the SLO.
2. Establish a baseline.
3. Select fault dimensions.
4. Generate controlled traffic.
5. Inject failures.
6. Observe system behavior.
7. Detect retry amplification.
8. Measure circuit breaker behavior.
9. Test recovery.
10. Narrow in on failure thresholds.
11. Compare resilience configurations.
12. Recommend changes.
13. Re-run the experiments.
14. Produce an engineering report.

That is the end-state architecture.

---

# 50. Final Recommendation

The strongest architectural direction is:

```text
             ┌──────────────────────────┐
             │          JaiClaw         │
             │   AI Experiment Agent    │
             └────────────┬─────────────┘
                          │
                     MCP / Tools
                          │
             ┌────────────┼────────────┐
             │            │            │
             ▼            ▼            ▼
         Traffic        Fault       Observation
          Tools         Tools          Tools
             │            │            │
             ▼            ▼            ▼
          JMeter      Backend B     Telemetry
                                      │
                                      ▼
                              Experiment Registry

                    ┌────────────────────┐
                    │    SYSTEM UNDER    │
                    │       TEST         │
                    │                    │
JMeter ────────────▶│ API A              │
                    │   │                │
                    │   ▼                │
                    │ Resilience4j       │
                    │   │                │
                    │   ▼                │
                    │ Backend B           │
                    └────────────────────┘
```

The most important architectural decision is to put a **controlled experiment/tool layer between JaiClaw and the infrastructure**. This lets the LLM reason about resilience experiments while keeping JMeter, fault injection, metrics, and infrastructure implementation details behind stable Java interfaces.

The project should therefore be built in this order:

```text
1. API A
      ↓
2. Backend B simulator
      ↓
3. Deterministic JMeter baseline
      ↓
4. Observability
      ↓
5. Resilience4j
      ↓
6. Experiment Registry
      ↓
7. Test Control APIs
      ↓
8. MCP tools
      ↓
9. JaiClaw integration
      ↓
10. AI-assisted analysis
      ↓
11. AI experiment planning
      ↓
12. Autonomous resilience discovery
```

This progression keeps the engineering system deterministic at the foundation while allowing JaiClaw to become increasingly sophisticated at the orchestration and analysis layers.
