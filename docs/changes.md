# Changes Log

## Experiment 1: Remote Procedure Call (RPC) / Java RMI
**Date:** 2026-08-12

The following files were created and configured to implement the client-server architecture using Java RMI, connecting to a PostgreSQL database.

| File Path | Description |
|---|---|
| [db.properties](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/db.properties) | Configuration file configured to use Supabase IPv4 Connection Pooler with `prepareThreshold=0` parameter to disable server-side prepared statements and prevent PgBouncer transaction-mode conflict errors. |
| [schema.sql](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/schema.sql) | Merged schema combining previous design with existing.sql (ENUM types, indexes, and seed data). Cleaned up redundant seat_id column and duplicate ENUM values. |
| [existing.sql](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/existing.sql) | Input schema containing raw database table structures and indexes, merged into schema.sql. |
| [src/rmi/BookingService.java](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/src/rmi/BookingService.java) | Remote service interface exposing login, search, availability, booking, cancellation, and history methods. |
| [src/rmi/UserSession.java](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/src/rmi/UserSession.java) | DTO representing a logged-in user session. |
| [src/rmi/TrainSearchResult.java](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/src/rmi/TrainSearchResult.java) | DTO for matching trains from route search. |
| [src/rmi/CoachAvailability.java](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/src/rmi/CoachAvailability.java) | DTO details of available seats per coach type. |
| [src/rmi/PassengerInput.java](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/src/rmi/PassengerInput.java) | DTO to collect passenger information for booking. |
| [src/rmi/BookingResult.java](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/src/rmi/BookingResult.java) | DTO returning PNR, total fare, status, and seat assignments. |
| [src/rmi/CancellationResult.java](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/src/rmi/CancellationResult.java) | DTO containing cancellation charges and refund amounts. |
| [src/rmi/BookingHistoryItem.java](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/src/rmi/BookingHistoryItem.java) | DTO representing past booking transactions with passenger details. |
| [src/server/DBConnection.java](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/src/server/DBConnection.java) | Database connection manager reading from `db.properties`. |
| [src/server/BookingServiceImpl.java](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/src/server/BookingServiceImpl.java) | Service implementation containing transactional business logic and database concurrency locks. Added explicit ENUM casting (gender, berth preference, coach class, and payment mode parameters) and input uppercase normalization to resolve PostgreSQL type conversion mismatches. |
| [src/server/BookingServer.java](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/src/server/BookingServer.java) | Main class that initializes the local RMI registry and binds the BookingService. |
| [src/client/BookingClient.java](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/src/client/BookingClient.java) | Interactive CLI client application implementing the user dashboard. |
| [compile.bat](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/compile.bat) | Batch compile script downloading the PostgreSQL driver and compiling sources. |
| [run_server.bat](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/run_server.bat) | Execution script to start the Booking Server. |
| [run_client.bat](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/run_client.bat) | Execution script to run the Booking Client CLI. |
| [README.md](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/README.md) | Setup and run manual detailing compile and run commands for both Windows and Linux Ubuntu. |

---

## Experiment 2: Multithreading & Database Concurrency Control
**Date:** 2026-08-15

The following updates and files were created to implement server-side thread pooling (`ExecutorService`), PostgreSQL pessimistic concurrency control (`FOR UPDATE SKIP LOCKED`), and client-side barrier testing (`CountDownLatch`).

| File Path | Description |
|---|---|
| [src/server/BookingServer.java](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/src/server/BookingServer.java) | Updated to rebind both `BookingService` and `TatkalService` in the RMI registry port 1099. |
| [src/server/BookingServiceImpl.java](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/src/server/BookingServiceImpl.java) | Integrated server-side `ThreadPoolExecutor` (8 core, 32 max, 100 queue) with graceful rejection, shutdown hook, thread logging, and non-blocking PostgreSQL `FOR UPDATE OF sa SKIP LOCKED` seat allocation. Scoped lock target strictly to `seat_allocations` table (`OF sa`), preventing multi-table JOIN locking on `coaches` and `seats` that caused premature seat skips during high-concurrency bursts. |
| [src/client/TatkalConcurrencyTest.java](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/src/client/TatkalConcurrencyTest.java) | Enhanced multithreaded client experiment harness supporting high-concurrency client runs (50+ clients), multi-passenger allocations, waitlist tracking, and seat uniqueness assertions. |
| [src/com/tatkal/client/TatkalConcurrencyTest.java](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/src/com/tatkal/client/TatkalConcurrencyTest.java) | Package wrapper for `com.tatkal.client.TatkalConcurrencyTest` namespace compatibility. |
| [src/client/UnsafeBookingDemo.java](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/src/client/UnsafeBookingDemo.java) | Conceptual demonstration highlighting double-booking hazards when database pessimistic locks are omitted. |
| [schema/schema.sql](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/schema/schema.sql) | Expanded database schema with production-scale seed dataset (10 stations, 5 trains, 30 schedules, multi-coach compositions, and dynamic 1,000+ seat allocations via PostgreSQL `generate_series`). Fixed `berth_type_enum` type casts. |
| [compile.bat](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/compile.bat) | Updated compilation script to compile `src/com/tatkal/client/*.java`. |
| [run_concurrency_test.bat](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/run_concurrency_test.bat) | Execution script to run the concurrency test suite on Windows. |
| [README.md](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/README.md) | Comprehensive project README containing About section, Experiment 1 (RPC/RMI), Experiment 2 (Multithreading & Database Concurrency), setup guides, execution commands, and repository structure overview. |

---

## Experiment 3: Clock Algorithms (Cristian's Physical Sync & Lamport Logical Clock)
**Date:** 2026-08-18

The following files were created and configured to implement Cristian's Algorithm for physical clock synchronization and Lamport Logical Clock for event ordering across logical nodes (Mumbai, Delhi, Chennai).

| File Path | Description |
|---|---|
| [src/clock/PhysicalClock.java](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/src/clock/PhysicalClock.java) | Thread-safe simulated physical clock supporting initial offsets (+5000 ms Mumbai, +10000 ms Delhi, +1000 ms Chennai) and Cristian algorithm adjustments. |
| [src/clock/LogicalClock.java](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/src/clock/LogicalClock.java) | Thread-safe Lamport Logical Clock implementation supporting Rule 1/2 (`increment`) and Rule 3 (`updateFromRemote`: `max(L, R) + 1`). |
| [src/clock/DistributedNode.java](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/src/clock/DistributedNode.java) | Encapsulates logical node identity, physical/logical clock instances, Cristian's RTT calculation (`T0`, `ServerTime`, `T1`, `RTT`, `RTT/2`, `Adjustment`), ±100 ms window validation, and Lamport message handling. |
| [src/rmi/ClockMessage.java](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/src/rmi/ClockMessage.java) | Serializable DTO carrying sender node ID, logical timestamp, physical timestamp, and event description over RMI. |
| [src/rmi/BookingService.java](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/src/rmi/BookingService.java) | Remote service interface updated with `getServerPhysicalTime()` and `processClockSyncMessage()`. |
| [src/server/BookingServiceImpl.java](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/src/server/BookingServiceImpl.java) | Service implementation providing Mumbai time server timestamps and server logical clock updating. |
| [src/client/ClockAlgorithmsDemo.java](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/src/client/ClockAlgorithmsDemo.java) | Main experiment runner demonstrating Section A (Cristian physical sync), Section B (Lamport logical ordering), and Section C (Tatkal booking event with dual timestamps). |
| [src/client/BookingClient.java](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/src/client/BookingClient.java) | Updated interactive CLI dashboard with Option 6 (`Synchronize Clock & View Timestamps (Exp 3)`) and integrated dual physical/logical clock timestamp display on booking confirmation receipts. |
| [compile.bat](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/compile.bat) | Updated compilation script to include `src/clock/*.java`. |
| [run_clock_demo.bat](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/run_clock_demo.bat) | Batch execution script to run the Clock Algorithms experiment on Windows. |

---

## Experiment 4: Bully Election Algorithm for Distributed Process Leadership
**Date:** 2026-08-25

The following files were created and modified to implement the Bully Election Algorithm across distributed Tatkal user client processes using Java RMI and PostgreSQL transaction control (`users.is_leader`), featuring a single complete lab demonstration with failed request resend and satisfaction workflows using formal `REQUEST` and `ACK` message terminology.

| File Path | Description |
|---|---|
| [src/rmi/ClientElectionService.java](file:///c:/Users/Atharv/OneDrive/Desktop/dc-tatkal-reservation/src/rmi/ClientElectionService.java) | Remote RMI interface enabling process-to-process election calls (`receiveElection`, `receiveCoordinator`, `processLeaderRequest`, `isAlive`). |
| [src/client/ClientElectionNode.java](file:///c:/Users/Atharv/OneDrive/Desktop/dc-tatkal-reservation/src/client/ClientElectionNode.java) | Distributed client process node implementation managing RMI bindings (`UserClient_<userId>`), failure simulation, `sendRequestToLeader` workflow (detecting missing leader ACK, launching election, and re-sending initially failed request to new leader), DB transactional updates, and formatted logs (`ELECTION REQUEST`, `ALIVE ACK`, `COORDINATOR ANNOUNCEMENT`, `COORDINATOR ACK`, `REQUEST ACK`). |
| [src/client/BullyElectionDemo.java](file:///c:/Users/Atharv/OneDrive/Desktop/dc-tatkal-reservation/src/client/BullyElectionDemo.java) | Single complete evaluation runner executing the lab demonstration scenario (Initial booking request to Leader User 4 fails due to missing ACK -> User 2 initiates Bully Election -> User 3 elected as winner -> DB updated -> COORDINATOR broadcast -> initially failed request re-sent to new leader User 3 and successfully satisfied with `REQUEST ACK`). |
| [run_election_demo.bat](file:///c:/Users/Atharv/OneDrive/Desktop/dc-tatkal-reservation/run_election_demo.bat) | Batch execution script to run the Bully Election demonstration on Windows (`java -cp "lib/*;bin" client.BullyElectionDemo`). |
| [src/rmi/BookingService.java](file:///c:/Users/Atharv/OneDrive/Desktop/dc-tatkal-reservation/src/rmi/BookingService.java) | Updated remote interface exposing `getCurrentLeader()`, `updateLeaderInDatabase()`, and `getAllUsers()`. |
| [src/server/BookingServiceImpl.java](file:///c:/Users/Atharv/OneDrive/Desktop/dc-tatkal-reservation/src/server/BookingServiceImpl.java) | Service implementation providing transaction-safe database leader queries and updates (`UPDATE users SET is_leader = FALSE; UPDATE users SET is_leader = TRUE WHERE user_id = ?`). |
| [src/client/BookingClient.java](file:///c:/Users/Atharv/OneDrive/Desktop/dc-tatkal-reservation/src/client/BookingClient.java) | Updated interactive CLI dashboard displaying current leader in the header and adding CLI options for leader checking, initiating election, simulating leader failure, and database record verification. |


