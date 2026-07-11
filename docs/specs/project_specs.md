# Digital Twin Mobility-on-Demand Specification (v1 Baseline for v2 Engineering)

This specification describes the implementation in this repository as a version `v1` baseline for an engineering team evolving or reimplementing version `v2`. Claims marked **Code-verified** come from Java sources, Spring configuration, message clients, Maven descriptors, tests, and bundled data. Claims marked **Inferred** are design intent derived from those artifacts and the architecture documentation.

## 1. System Boundary and Context Analysis

The system is an event-driven digital-twin integration framework for Mobility-on-Demand simulation. It separates historical rider-demand supply, taxi decision behavior, fleet/platform coordination, algorithm execution, and simulation-engine actuation into independently runnable Java processes connected through RabbitMQ direct exchanges. The central objective is to let an external agent-based simulator publish state and lifecycle events, receive concrete taxi operations, and replace matching/repositioning algorithms without coupling those algorithms to simulator internals.

The code-verified runtime comprises `centralOperator-microservice`, `order-microservice`, `taxi-microservice`, and `demo-client`. The first three are Java 21 Spring Boot applications configured with `spring.main.web-application-type=none`; they expose AMQP consumers rather than HTTP controllers. `demo-client` uses the RabbitMQ Java client directly and represents both an executable integration harness and the intended boundary shape for an AnyLogic adapter. The external AnyLogic model is documented and linked but is outside this repository. RabbitMQ, the JVM, the local filesystem, and the order workbook are infrastructure dependencies.

External actors are simulation-engine taxi and rider agents, a simulation clock, algorithm developers, operators running the services, RabbitMQ, and offline analysts consuming generated workbooks. The simulator sends initialization, time updates, taxi-state updates, order batches, algorithm-window triggers, and operation-completion signals. The platform returns initialization acknowledgements and `NEW_TAXI_OPERATION` commands for idling, repositioning, or passenger service. The standalone taxi service also implements a smaller request/response decision contract, and the order service implements initialization and time-window demand queries.

Inside the boundary are queue/exchange declaration, JSON serialization, correlation-based RPC, order ingestion from Excel, in-memory fleet/order state, matching and repositioning orchestration, per-taxi operation queues, simulation time, utility and matching-rate metrics, and demo-side operation DTOs. Outside are GIS movement, AnyLogic statecharts/MOLUs, route and travel-time calculation, RabbitMQ hosting, model training, long-term database persistence, user interfaces, authentication infrastructure, and the notebooks' interactive execution environment.

Data crosses trust boundaries as JSON bytes plus AMQP metadata. Simulator-originated JSON is untrusted at listeners; central request dispatch validates the presence of `requestType` and `payload`, specialized update services deserialize typed maps, and request/response paths depend on `correlationId`. Excel input is trusted classpath data but still parsed through Apache POI. Generated metric workbooks cross from runtime state to analysts. RabbitMQ credentials and message transport sit at an infrastructure trust boundary.

Ownership is explicit. The order service owns the immutable loaded demand catalog and time-window filtering. The taxi service owns stateless single-taxi action selection. The central operator owns current fleet/order state, simulation time, matching/repositioning policy, operation sequencing, and metrics. RabbitMQ owns durable queueing and routing, not business idempotency. The simulation engine owns physical agent execution and reports completion/state. The demo client owns request correlation and maps central responses back toward simulator operations.

## 2. Overall Architecture Design

The architecture is a hybrid of microservices and event-driven request/response. Three separately packaged Spring Boot processes communicate through durable RabbitMQ queues bound to direct exchanges. The demo/AnyLogic-side client uses asynchronous consumers internally but offers blocking five-second RPC methods to simulation code. Central algorithm triggers and operation commands form an event-driven workflow; correlation identifiers provide RPC semantics for update acknowledgements and query responses.

The separation is justified by domain ownership. Historical demand ingestion has different scale and lifecycle from per-taxi decisions; central matching requires a global fleet/order view; simulator actuation must remain outside backend algorithm code. Direct exchanges make routing deterministic, while JSON allows simulator and service implementations to evolve independently if contracts are governed.

```text
Bundled trip workbook
    -> Order Service (AllOrders, FetchOrderService)
       <-> order.exchange / order.* queues

AnyLogic or demo-client
    <-> co.exchange
        -> co.requests -----------------------> CoListener
        -> co.activeTaxis.update.requests ----> CoTaxiUpdateListener
        -> co.activeOrders.update.requests ---> CoOrderUpdateListener
        <- co.responses ----------------------- TaxiOperationPublisher / CoInitService
        <- update response queues ------------- acknowledgement listeners

Central Operator
    TaxiStateMap + ActiveOrders + SimulationTime
       -> BMDDPGService / CoMatchingService
       -> TaxiOperationSequence
       -> TaxiOperationPublisher -> simulator
       -> UtilityMonitor + MatchingRateMonitor -> output/*.xlsx

Optional standalone taxi RPC
    simulator -> taxi.exchange/taxi.requests -> Taxi Service
              <- taxi.exchange/taxi.responses
```

Runtime topology is one JVM per Spring application, one RabbitMQ broker, and one JVM-hosted simulation adapter. All Spring services are non-web workers. Each queue is declared durable. Central in-memory aggregates are thread-aware through `ConcurrentHashMap`, `ConcurrentLinkedQueue`, and `CopyOnWriteArrayList`, enabling multiple listener containers to update state concurrently.

Scalability is queue-consumer based. Taxi and order request consumers can be replicated for stateless work, but central operator replicas would need partitioned ownership or external state because their maps, sequences, clock, and counters are process-local. Queue-level competing consumers distribute messages, so a single logical simulation should have one authoritative central state owner unless messages are partitioned by simulation id.

Fault tolerance relies on RabbitMQ durable queues, persistent demo-client publications, Spring listener containers, local exception boundaries, and request timeouts. **Inferred:** production delivery semantics depend on Spring AMQP acknowledgement defaults and broker durability; business handlers do not implement deduplication or transactional outbox behavior. Partial outages are naturally isolated by queue family: order queries, standalone taxi actions, and central orchestration can fail independently.

Deployment is manual Maven/JAR execution with RabbitMQ at `localhost:5672` by default. No Docker, Kubernetes, CI workflow, infrastructure-as-code, service discovery, or multi-region topology is present. Multi-region is not applicable to a latency-sensitive single simulation in v1; v2 should first introduce simulation identity, externalized state, contract versioning, and deterministic event partitioning.

The primary trade-off is modular algorithm integration versus distributed consistency. RabbitMQ decouples simulator timing from backend execution and lets algorithms be swapped behind stable DTOs, but cross-queue workflows are not atomic. In-memory state maximizes simulation-loop speed but makes central restart recovery an explicit reinitialization concern.

## 3. Complete Module Breakdown

### Central Operator Bootstrap and AMQP Configuration

`CoMicroserviceApplication` starts component scanning. `RabbitMQConfig` owns `co.exchange`, eight durable request/response queues, bindings whose routing keys equal queue names, and the Jackson message converter. `TaxiOperationConfig` injects `taxi.operation.algorithm`, defaulting to `FALLBACK`; the current application selects `BMDDPG`. These modules own wiring, not domain transformations.

### Central Request Dispatch

`CoListener` consumes `co.requests`, parses raw UTF-8 JSON, reads `requestType` and `payload`, and dispatches `TAXI_OP_DONE`, `BMDDPG_WINDOW_STARTS`, `CO_INIT`, and `SIMU_TIME_UPDATE`. Taxi completion delegates to `TaxiOperationService`; window start triggers `BMDDPGService`; initialization clears state and initializes monitoring; time update sets the platform clock. The listener preserves a request correlation id for initialization response. Errors are logged and contained within the delivery callback.

### Fleet and Demand State

`TaxiStateMap` owns the latest `TaxiState` per taxi id in a `ConcurrentHashMap`, supplies defensive map copies and an idling-taxi projection, and clears on initialization. `TaxiStateMapService` parses `{taxiId: TaxiState}` updates and applies them. `ActiveOrders` owns the current unmatched `TaxiOrder` list in a `CopyOnWriteArrayList`; `ActiveOrdersService` accepts `{ "orderList": [...] }`, updates matching-window counters, and appends orders. `ActiveTaxis` is a second list abstraction used by legacy/simple matching paths. These stores are volatile and scoped to one central JVM.

### Simulation Clock

`SimulationTimeService` parses `yyyy-MM-dd HH:mm:ss` payload values, owns the current `Date`, and exposes time-only and date-time strings. Monitoring services read it to label measurement windows. It does not schedule events itself; `ScheduledEvent` and `DemoEvent` are extension examples for time-triggered behavior.

### Matching and Algorithm Orchestration

`BMDDPGService` is the algorithm integration façade. On a window trigger it snapshots idling taxis and active orders, derives reposition commands, invokes a pluggable-shape matching method, enqueues reposition and service operations, updates matching/utility state through `CoMatchingService`, and starts each affected taxi's sequence. Its documented extension contracts are `Map<String, ReposOperationDTO>` for repositioning and a map containing parallel `matchedOrders` and `matchedTaxis` lists for matching.

`CoMatchingService` validates parallel match results, calculates per-pair utility, enqueues `ServiceOperationDTO`, updates match counters, and removes matched orders. `demoSimpleMatch` pairs taxis and orders by iterator order and demonstrates the algorithm boundary. The module owns match-result application, not physical taxi movement.

### Taxi Operation State Machine

`TaxiOperationSequence` owns a concurrent FIFO queue of `TaxiOperationDTO` per taxi. DTO variants are `IdlingOperationDTO(idleTime)`, `ReposOperationDTO(toLat,toLon)`, and `ServiceOperationDTO(orderId)`; `TaxiOperationType` also defines `CHARGING` and `OTHER`. `TaxiOperationService` routes completion events according to the configured algorithm. `BMDDPGService.handleTaxiOpDone` dequeues queued work first; when no work remains it transitions completed idling to another idling period, repositioning to idling, and service to idling.

`TaxiOperationPublisher` translates DTOs into the external envelope `{responseType:"NEW_TAXI_OPERATION", payload:{taxiId,operationType,operationData}}` and publishes to `co.responses`. `MessagePublisherService` owns RabbitTemplate calls, JSON content type, exchange/routing key selection, and optional correlation preservation.

### Monitoring and Data Processing

`MatchingRateMonitorService` tracks initial, added, and matched order counts per window, computes `matched/total`, and writes timestamped rows to an output workbook. `UtilityMonitorService` resolves taxi/order pairs, derives service utility from trip income/distance and taxi state, accumulates results, and writes its workbook. Both initialize output logging on `CO_INIT`. The notebooks under `data-processing/co` visualize matching-rate and utility outputs; they are downstream analytical assets rather than runtime dependencies.

### Order Microservice

`AllOrders` loads `2010-06-01+3_trips.xlsx` at `@PostConstruct`, converts each row into a `TaxiOrder`, and assigns the row index as order id. `FetchOrderService` parses `{curDatetime,timeWindow}`, compares time-of-day values, formats matching orders with `yyyy-MM-dd HH:mm:ss`, and returns a JSON array. `OrderInitListener` handles dataset reset/initialization semantics and responds on `order.init.responses`; `OrderFetchListener` handles correlated queries and publishes on `order.fetch.responses`. `RabbitMQConfig` owns `order.exchange` and four durable queues/bindings.

### Standalone Taxi Microservice

`TaxiMessageListener` consumes a compact `TaxiState(timeInterval,taxiZone,soc)` from `taxi.requests`, requires correlation metadata, calls `TaxiService`, and sends a correlated JSON response to `taxi.responses`. `TaxiService` selects charging with probability 0.1 or when SOC is below 20; charging destinations come from zones 1–5 and other repositioning targets from a configured popular-zone list. It is stateless apart from random generators and can therefore scale with competing consumers.

### Demo Client and Simulation Adapter Contracts

`RabbitMQSetup` owns one native RabbitMQ connection/channel. `CoMsgClient`, `OrderMsgClient`, and `TaxiMsgClient` publish persistent messages with UUID correlation ids, register consumers, acknowledge responses, and resolve `CompletableFuture` entries in concurrent maps. Blocking façade methods wait up to five seconds. `Main` exercises initialization, taxi updates, order updates, and an algorithm-window trigger.

The demo domain mirrors service contracts (`TaxiState`, `TaxiOrder`, request/response enums, operation DTO-like data). `TaxiOperationManager` sketches how decoded central operations map into AnyLogic taxi statecharts/MOLUs. This adapter boundary is intentionally simulator-specific; backend services do not import AnyLogic classes.

## 4. Inter-Module Relationships and Communication

All service-to-service communication uses AMQP 0-9-1 through RabbitMQ direct exchanges. Request routing keys and response routing keys equal queue names. Message bodies are UTF-8 JSON; content type is `application/json`. Demo-client request publishers set delivery mode 2 and correlation ids. Spring response publishers preserve correlation ids for RPC-style calls; asynchronous `NEW_TAXI_OPERATION` events can be published without correlation.

The central generic flow is simulator `CoMsgClient -> co.exchange/co.requests -> CoListener -> domain service`. `CO_INIT` produces a correlated `CO_INIT_COMPLETE`. `SIMU_TIME_UPDATE` changes the clock without a response. `BMDDPG_WINDOW_STARTS` synchronously calculates commands inside the listener thread, then publishes asynchronous operations. `TAXI_OP_DONE` advances the taxi's operation sequence and may emit the next operation.

Taxi-state and active-order updates use dedicated request queues. Each requires a correlation id, mutates central in-memory state, and emits `UPDATE_DONE` or `ORDER_UPDATE_DONE` on its paired response queue. The correlation id is the sole request/response join key. There is no distributed transaction between state mutation and acknowledgement publication; v1 consistency is therefore at-least "handler ran before ack response was emitted," not exactly-once business application.

Order fetch is synchronous from the caller's perspective and asynchronous on the wire. The client publishes a time-window query, stores a future keyed by correlation id, and waits five seconds. The service filters its in-memory catalog and replies. Late responses may arrive after the client removes a timed-out future; consumers acknowledge the message but no caller receives it.

Standalone taxi action follows the same RPC pattern. The service's transformation is deterministic with respect to SOC threshold but intentionally stochastic in charging probability and zone choice. Multiple consumers may process independent requests concurrently. Ordering between different taxi requests is not guaranteed or required.

Within central operator, listeners call services synchronously in-process. Concurrent listener containers can interleave taxi updates, order updates, triggers, and completions. Concurrent collections protect structural operations, while algorithm triggers use snapshots/copies. Per-taxi operation ordering is FIFO through `ConcurrentLinkedQueue`. Cross-taxi ordering and ordering across RabbitMQ queues are unspecified.

There is no application retry policy, dead-letter exchange, circuit breaker, or idempotency key store in code. Broker/client reconnection belongs to Spring AMQP or the native RabbitMQ client. Listener exceptions are caught and logged, so failure propagation generally terminates the current business path without producing a response. Client-side timeout bounds blocking RPC waits but does not cancel broker delivery.

Normal simulation execution is: initialize central state; publish simulation clock and demand/fleet updates; trigger a decision window; snapshot idling taxis and unmatched orders; calculate reposition/match results; enqueue per-taxi operations; publish first operations; simulator executes movement/service; simulator reports updated state and completion; central advances each sequence; metrics are written at match-window boundaries.

During a partial outage, queued commands can remain in RabbitMQ if their consumer is down, but process-local central state is unavailable after central failure. Order-service outage affects demand queries, standalone taxi-service outage affects only that alternate action endpoint, and analysis notebook outage has no runtime effect. RabbitMQ outage halts all integration paths and is the system-wide messaging dependency.

## 5. Domain Model and Behavior Design

`TaxiState` is the platform's current vehicle value object: taxi id, longitude, latitude, simulation interval, taxi zone, SOC, revenue, and current operation. Construction and setters clamp interval/zone to non-negative values and SOC to `[0,100]`. Identity is `taxiId`; `TaxiStateMap` owns the latest state. The standalone taxi service uses a smaller state schema and is a distinct contract.

`TaxiOrder` represents a trip request with id, pickup/dropoff coordinates, pickup time, distance, and income. The order service owns the full historical catalog; central `ActiveOrders` owns only currently eligible, unmatched work. Matching transfers logical ownership by removing matched order ids from the active set and placing service operations in taxi queues.

`TaxiOperationDTO` is a polymorphic command value. An idling command owns duration, a reposition command owns destination coordinates, and a service command owns order id. The external envelope supplies the discriminator `operationType`; consumers must use it to select the operation-data schema.

The per-taxi operational state machine is:

```text
known taxi state
  -> algorithm window: enqueue REPOSITIONING and optionally SERVICE
  -> publish queue head
  -> simulator executes operation
  -> TAXI_OP_DONE
       -> queued operation exists: publish next FIFO item
       -> no queued operation:
            IDLING complete -> IDLING(120)
            REPOSITIONING complete -> IDLING(120)
            SERVICE complete -> IDLING(120)
```

The simulation lifecycle is `uninitialized -> CO_INIT -> active`. Initialization clears central taxi state, active orders, operation queues, and metric counters/files before returning `CO_INIT_COMPLETE`. Time updates and state updates are meaningful in the active phase. **Inferred:** the simulator must not trigger an algorithm window before initialization and state population, although the code primarily guards only the absence of taxi state.

Matching invariants require both keys, non-empty parallel lists, and equal lengths. Pair position defines taxi-to-order ownership. A successful application increments matched count, calculates pair utility, enqueues one service operation per pair, and removes the orders. Repositioning applies to the idling-taxi snapshot. An operation completion must identify a taxi and an enum-compatible operation type.

Consistency boundaries are process-local method calls and individual AMQP messages. Concurrent collections protect collection integrity, but no lock spans snapshot selection, result calculation, enqueueing, and active-order removal. v2 algorithms requiring strict deterministic windows should introduce a simulation-window aggregate and serialize commands by simulation/window id.

## 6. Data Architecture

Runtime state uses in-memory Java collections rather than a database. The central operator stores taxi state in `ConcurrentHashMap<String,TaxiState>`, active orders in `CopyOnWriteArrayList<TaxiOrder>`, and per-taxi work in `ConcurrentHashMap<String,Queue<TaxiOperationDTO>>`. This is justified for low-latency simulation orchestration where the simulator can replay/init state, but durability is delegated to external inputs and RabbitMQ.

Historical orders are loaded from a bundled XLSX workbook through Apache POI. Column order is pickup time, pickup latitude/longitude, dropoff latitude/longitude, distance, and income. Row number becomes order id. The catalog is retained in memory and time-window reads compare local time-of-day, making the workbook the source of truth for demand replay.

Metrics are written to XLSX files under a local `output` directory. Matching-rate records derive from per-window counters and simulation timestamps. Utility records derive from matched taxi/order attributes. These files are analytical outputs, not transactional state. The notebooks read the resulting series for visualization.

RabbitMQ queues are durable, and demo publications request persistent delivery. Persistence is nevertheless end-to-end only when the broker is configured for durable storage and messages have not already been acknowledged. Response messages and Spring-published operation events inherit RabbitTemplate defaults plus explicit JSON metadata; business state is not reconstructed from queues.

There is no database schema, partitioning, migration runner, archive service, cache tier, or read replica. Logical partitioning is by queue family and taxi id. A v2 persistence schema should use `simulation_id`, `window_id`, `taxi_id`, `order_id`, event sequence, contract version, and idempotency key so multiple simulations and recovery can coexist.

The consistency model is hybrid: strong structural consistency for individual concurrent-map/list operations, FIFO for one taxi's local operation queue, eventual consistency between simulator state and central snapshots, and best-effort analytical file output. Initialization is the data-lifecycle reset. Retention of workbook inputs and outputs is manual.

## 7. API and Contract Design

There are no HTTP endpoints despite the Spring Web dependency. The public integration API is the AMQP topology.

| Exchange / routing key | Request body | Response |
| --- | --- | --- |
| `co.exchange / co.requests` | `{requestType,payload}` | `co.responses`, depending on type |
| `co.activeTaxis.update.requests` | map of taxi id to `TaxiState` | correlated `"UPDATE_DONE"` |
| `co.activeOrders.update.requests` | `{orderList:[TaxiOrder...]}` | correlated `"ORDER_UPDATE_DONE"` |
| `co.match.requests` | matching request JSON | correlated `co.match.responses` |
| `order.exchange / order.init.requests` | initialization message | correlated init response |
| `order.exchange / order.fetch.requests` | `{curDatetime,timeWindow}` | formatted order array |
| `taxi.exchange / taxi.requests` | compact standalone `TaxiState` | correlated encoded `TaxiAction` |

Central request types are `CO_INIT`, `SIMU_TIME_UPDATE`, `BMDDPG_WINDOW_STARTS`, and `TAXI_OP_DONE`. Central response types are `CO_INIT_COMPLETE` and `NEW_TAXI_OPERATION`. The operation payload contract is `{taxiId, operationType, operationData}` with DTO-specific data. Consumers must tolerate JSON enum strings and the service's date format.

The error model is operational rather than contractual. Invalid central request structure and deserialization errors are logged; most paths emit no error response. Order fetch converts processing failure to `[]`. Native clients return error strings for serialization, publication, timeout, interruption, or failed future completion. There is no error-code envelope shared across services.

Authentication and authorization are absent at the application layer. Rate limiting, quotas, tenant headers, and API versions are absent. Queue names act as topic names and are constants duplicated between producers and consumers. Schema evolution is convention-based; DTOs exist in multiple modules rather than a shared versioned artifact.

For v2, preserve queue/routing names during a compatibility window, add `schemaVersion`, `simulationId`, `eventId`, and `causationId` to envelopes, define JSON Schema or a shared contract module, and introduce versioned routing keys only for breaking changes. Consumer isolation should use separate queues per consumer role rather than multiple semantic consumers competing on one queue.

## 8. Security Architecture

The application has no authentication tokens, sessions, roles, or per-operation authorization. All clients able to connect to the RabbitMQ virtual host and publish to the exchanges can invoke simulation actions. The intended v1 security boundary is a trusted local development environment.

RabbitMQ username/password and host/port are application properties for the Spring services and hard-coded in the demo setup. Transport defaults to plaintext AMQP on port 5672. No TLS, certificate validation, secret manager, rotation workflow, or at-rest encryption is configured. Generated workbooks and the bundled demand dataset use normal filesystem permissions.

Multi-tenant isolation is absent. Queue names are global, and messages do not carry simulation or tenant identifiers. One broker topology can therefore mix runs unless each run uses a separate broker/vhost or executes sequentially. Central state is also one global simulation per process.

The attack surface includes deserialization of untrusted JSON, oversized messages, queue flooding, malicious enum/request values, replayed state updates, correlation-id collisions, spreadsheet input, and unauthorized operation publication. Jackson binding is to concrete DTOs rather than general polymorphic class names, which limits gadget-style type activation. Domain setters clamp some numeric state, listeners check required envelope fields/correlation ids, and RabbitMQ routing constrains paths, but broker access control remains the principal mitigation.

Logging uses SLF4J and console output and may include ids, JSON payloads, or metric values. There is no immutable audit trail. v2 should externalize secrets, require TLS, configure least-privilege broker users, limit body size/prefetch, validate coordinates/time ranges/ids, sign or authenticate simulator commands where crossing hosts, and retain structured audit events keyed by simulation/window/event.

## 9. Non-Functional Design

No formal latency, throughput, availability, RTO, or RPO targets are declared. The implemented behavior targets interactive simulation: native RPC calls wait at most five seconds; in-memory algorithm state avoids database latency; message consumers process one message body synchronously per delivery callback; metric writing occurs in the orchestration path.

Horizontal scale is strongest for stateless taxi actions and read-only order fetches. Central orchestration is vertically scaled and single-owner by design. Backpressure is delegated to RabbitMQ listener prefetch and queue depth; the repository sets neither explicit prefetch nor concurrency. Native clients maintain an unbounded response-future map bounded in practice by request completion/timeouts.

Resilience comes from durable queues, persistent client requests, correlation maps, concurrent collections, isolated listener catches, and five-second caller timeouts. There are no bulkheads, circuit breakers, health probes, dead-letter queues, poison-message policies, or application retries. Availability is therefore broker- and process-dependent.

Observability consists of SLF4J/console statements, matching-rate and utility workbooks, system screenshots, and plotting notebooks. There is no structured log schema, distributed tracing, Prometheus metrics, dashboards, alerting, or request-duration instrumentation. Correlation ids can support trace reconstruction for RPC paths but are not consistently logged end-to-end.

Likely bottlenecks are synchronous algorithm execution on the central listener, copying `CopyOnWriteArrayList` state under write-heavy demand, workbook writes, full scans of the demand catalog per fetch, JSON serialization, and a single RabbitMQ channel in the demo client. **Inferred:** deterministic simulation performance benefits from bounding algorithm-window work and moving analytical writes off the critical command path.

## 10. Configuration and Environment Design

Each Spring service uses `application.properties`. All are non-web applications and point to local RabbitMQ with host, port, username, and password. Central additionally selects `taxi.operation.algorithm=BMDDPG`. The demo client duplicates connection values in Java. Maven wrappers exist for the three Spring services; demo-client is a standalone Maven project.

Environment separation is manual. There are no Spring profiles, environment-variable placeholders, configuration server, feature-flag service, or runtime reload. Algorithm selection is the principal strategy flag and is read through `TaxiOperationConfig`; changing it requires configuration update and process restart.

Build and rollout are per-module Maven operations. There is no parent multi-module POM, container image, CI/CD workflow, deployment manifest, infrastructure-as-code, blue/green, or canary mechanism. RabbitMQ exchanges/queues are declaratively created by Spring services on startup; the demo assumes those declarations already exist.

For v2, consolidate broker/contract settings, externalize credentials, add local/test/production profiles, introduce reproducible container orchestration for broker plus services, and treat algorithm rollout as versioned strategy deployment. Canary behavior should partition by simulation id rather than split messages from one simulation between algorithm versions.

## 11. Dependency Graph and Technology Stack

The core language is Java 21. Spring services use Spring Boot parent `3.4.3`, `spring-boot-starter-amqp`, `spring-boot-starter-web` in non-web mode, and `spring-boot-starter-test`. Central and order services use Apache POI `5.2.3`. Jackson supplies JSON mapping. The demo client uses RabbitMQ Java client `5.18.0` and Jackson Databind `2.16.1`.

The internal dependency graph is:

```text
simulation/demo DTOs and clients
    -> RabbitMQ contracts
        -> listeners/config
            -> application services
                -> concurrent in-memory aggregates
                -> algorithm/matching policy
                -> operation publisher
                -> monitoring/workbook output

order listeners -> AllOrders + FetchOrderService -> bundled XLSX
taxi listener -> TaxiService -> TaxiAction
central listener/update listeners -> central domain graph
```

Layering rules should keep RabbitMQ `Message` and `RabbitTemplate` in listeners/publishers, JSON DTOs in contract/model packages, algorithm decisions in services, and simulator-specific statechart code in the adapter. Current code generally follows this separation, enabling algorithm replacement without AnyLogic imports.

Version constraints are recorded independently in POMs; there is no dependency-management parent across modules. Upgrade strategy should align Spring Boot and AMQP starter compatibility, keep Java/Jackson behavior consistent across duplicated DTOs, and contract-test RabbitMQ message conversion. Breaking changes to queue names, enums, property names, or JSON fields require coordinated producer/consumer rollout.

## 12. Failure Analysis

RabbitMQ is the principal system-wide dependency. Broker outage prevents state synchronization, queries, triggers, and commands. Durable/persistent messages reduce loss across broker restart but not when clients cannot publish. Recovery is broker restoration followed by service reconnection and simulation-state reconciliation.

Central operator is the authoritative volatile state owner. Process loss discards current taxi map, active orders, operation sequences, time, and metric counters. Recovery is `CO_INIT` plus simulator replay of time, taxis, and active orders; pending commands already acknowledged by the simulator require domain reconciliation. **Inferred RTO:** service restart plus replay time. **Inferred RPO:** last state retained by the simulator or upstream event source, because central itself has no durable checkpoint.

Order-service failure removes demand-query availability but the workbook remains a recoverable source. Restart reloads the complete classpath dataset. RTO is JVM startup and workbook parse time; RPO is zero for the immutable bundled dataset. A malformed/missing workbook prevents a populated catalog, and fetch calls then return no useful demand.

Taxi-service failure affects only the optional standalone decision endpoint. Requests may remain queued until a consumer returns; client-side calls time out after five seconds and late responses are acknowledged without completing an expired future. Its stateless design permits restart with no domain-state loss.

Central listener processing errors are caught and logged. Because handlers generally do not return structured errors, clients observe timeouts or missing asynchronous operations. Cross-step failure can leave updated in-memory state without a response or queued operations without all expected publications. There is no compensating transaction or replay ledger.

Monitoring failure can interrupt file output and reduce observability; depending on where an unchecked I/O error is handled, it can also affect the current orchestration path. Runtime simulation state remains in memory. Recovery is recreating output directories/files and restarting or reinitializing monitoring; RPO is the last flushed workbook row.

Native demo-client failure loses its in-memory future map and single channel. RabbitMQ may still deliver responses, but no restarted caller can correlate them. The client should close resources gracefully; a production adapter should use per-request reply queues or a durable correlation store if requests must survive simulator restart.

There are no automated backups. Backup sources are Git-tracked code/config, the bundled order workbook, simulator state, RabbitMQ persistence, and manually copied output workbooks. Disaster recovery should start RabbitMQ, start consumers, initialize central, replay authoritative simulator state, then resume at a well-defined window boundary.

## 13. Versioning and Evolution Strategy

All three Spring artifacts are `0.0.1-SNAPSHOT`; demo-client is `1.0-SNAPSHOT`. Semantic versioning is not formally enforced. For v2, treat queue names, routing keys, envelope fields, enums, date formats, correlation behavior, and operation DTOs as public contracts.

Additive JSON evolution should allow consumers to ignore unknown fields and producers to preserve required v1 fields. Breaking field/type/enum changes should introduce a new schema version and, where necessary, parallel `*.v2` routing keys/queues. Deploy consumers that accept both versions before switching producers, then deprecate v1 after replay queues drain.

There is no database migration governance in v1. If durable state is introduced, migrations must be owned centrally, keyed by simulation id, and coordinated with event schema versions. Workbook schema evolution should document column names/types rather than depend only on positional columns.

Contract testing should serialize every request/response with producer classes and deserialize it with consumer classes. Required suites include central dispatch types, state/order update acknowledgements, all operation DTO variants, correlation preservation, order date formatting/window boundaries, taxi SOC threshold behavior, initialization reset, per-taxi FIFO sequencing, match parallel-list invariants, and metric workbook schemas.

Compatibility enforcement should move duplicated constants/DTOs into a versioned contract artifact or generate them from schemas. Maven dependency convergence and integration tests against a real ephemeral RabbitMQ broker should gate releases. A practical deprecation timeline is at least one minor release or one complete simulation migration cycle.

## 14. Formal Consistency and Invariants

System-wide invariants are that the simulator and services agree on exchange/routing names; one central state owner coordinates a logical simulation; initialization precedes algorithm work; and simulator operation completion advances only the identified taxi's sequence. These are enforced through topology constants, control flow, and deployment convention.

Data invariants are that taxi identity is non-empty and stable per map entry; interval/zone are non-negative; SOC is clamped to `[0,100]`; active orders contain unmatched work; order ids identify catalog/active entries; and service operations refer to an order id. Matching results require equal positional taxi/order lists. Code enforces numeric clamping and most match-shape rules; v2 should validate identifiers and coordinate ranges explicitly.

Messaging invariants are that RPC requests carry a correlation id, responses preserve it, JSON content type is used, and request/response routing keys match declared durable queues. Native clients resolve at most one future per correlation id by removing it on completion. Cross-queue ordering is never assumed; per-queue broker order and per-taxi local FIFO are the only relevant ordering guarantees.

Transaction invariants are local: one concurrent collection mutation is atomic at its collection boundary, but state mutation and message publication are not one transaction. Applying a match must enqueue service work, update metrics, and remove active orders as one logical unit even though v1 lacks rollback. Initialization logically clears all central aggregates and resets metric outputs before acknowledgement.

Security invariants in v1 are deployment-level: only trusted broker clients should publish; broker credentials must be available to each process; and simulator messages must deserialize to known DTO/enums. v2 enforcement must add TLS, secret management, least-privilege permissions, simulation identity, payload bounds, and replay protection.

Operational invariants are that `CO_INIT_COMPLETE` is emitted only after reset actions; operation queues are FIFO per taxi; a taxi with queued work receives that work before fallback idling; order fetch returns pickups in `[start,end)` by time-of-day; matching-rate counters reset per measured window; and process restart requires explicit state rehydration. These invariants should become integration tests and monitoring assertions before any horizontally scaled v2 deployment.
