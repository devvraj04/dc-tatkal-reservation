# System Architecture & Technical Design Logs

## Experiment 1: Java RMI / RPC Distributed Reservation Booking System
**Date:** 2026-08-12

---

### 1. Objective and Architectural Need
In a distributed railway reservation system, client applications (passengers) should never directly connect to the database. Directly exposing the database port online presents severe security risks (SQL injections, credential leakage) and makes it impossible to manage complex multi-step transactions, caching, or rate limiting. 
To solve this, we implemented a **3-Tier Architecture**:
1. **Client Tier**: Interactive CLI providing menus, input sanitization, and calling remote procedures.
2. **Application Server Tier (Booking Server)**: Exposes a Remote Procedure Call (RPC) interface via Java RMI, handles business logic, and manages transactions.
3. **Database Tier**: Relational PostgreSQL database (hosted on Supabase) storing static master data and dynamic seat allocations.

---

### 2. Java RMI Mechanism & Serialization
Java RMI (Remote Method Invocation) allows an application on one JVM to invoke methods on an object running in another JVM. 
- **Registry**: The Booking Server starts a registry on port `1099`. It creates an instance of `BookingServiceImpl` and binds it to the lookup name `"BookingService"`.
- **Stubs & Skeletons**: The Client contacts the RMI Registry, retrieves a Stub (proxy) for `BookingService`, and calls methods on it. Java RMI serializes the parameters, transmits them over TCP/IP, and the server executes the SQL queries before returning the serialized DTOs back.
- **DTOs (Data Transfer Objects)**: All return values (like `BookingResult`, `UserSession`) and input lists (like `PassengerInput`) implement `java.io.Serializable` with a unique `serialVersionUID` to enable network transmission.

---

### 3. Concurrency Control and Database Locking (Double Booking Prevention)
A primary challenge in high-frequency booking systems (like Tatkal) is preventing **double-booking** (two clients booking the same seat simultaneously). 
- **Technique: Pessimistic Locking with Transactions**:
  1. We turn off automatic commits on the database connection: `connection.setAutoCommit(false)`.
  2. We select available seats matching the requested coach type (e.g. SL or 3A) using the `FOR UPDATE` clause:
     ```sql
     SELECT sa.allocation_id, sa.seat_id, s.seat_number, c.coach_number, s.berth_type
     FROM seat_allocations sa
     JOIN seats s ON sa.seat_id = s.seat_id
     JOIN coaches c ON s.coach_id = c.coach_id
     WHERE sa.schedule_id = ? AND c.coach_type = ? AND sa.booking_status = 'AVAILABLE'
     ORDER BY s.seat_number
     LIMIT ?
     FOR UPDATE;
     ```
  3. The `FOR UPDATE` modifier places an exclusive row-level lock on the returned seat rows in PostgreSQL. Any concurrent transaction attempting to select the same seats will block/wait until our transaction finishes (either `commit` or `rollback`).
  4. If seats are available, we update their status to `'BOOKED'`, insert the bookings/passengers/payments, and invoke `conn.commit()`.
  5. If an error occurs (such as connection drops or database faults), `conn.rollback()` is executed, releasing all locks and restoring the seats to `'AVAILABLE'`.

---

### 4. Waitlist Progression & Automated Promotion
To mirror the behavior of Indian Railways, when a passenger cancels a confirmed ticket, the seat shouldn't simply become vacant if other passengers are waitlisted.
- **Logic**:
  1. When a passenger cancels a confirmed ticket, the transaction fetches active waitlist entries for that specific train schedule and coach class:
     ```sql
     SELECT w.waitlist_id, w.pnr, bp.booking_passenger_id, b.user_id
     FROM waitlists w
     JOIN bookings b ON w.pnr = b.pnr
     JOIN booking_passengers bp ON (w.pnr = bp.pnr AND bp.passenger_status = 'WAITLIST')
     WHERE w.schedule_id = ? AND w.coach_type = ? AND w.status = 'WAITING'
     ORDER BY w.waitlist_number ASC
     LIMIT 1
     FOR UPDATE;
     ```
  2. If a waitlisted passenger is found, their status is updated to `'CONFIRMED'`, and the seat allocation is reassigned directly to their PNR inside the same transaction.
  3. The waitlist entry status is updated to `'CONFIRMED'`, and a system notification is created to alert the passenger of their promotion.
  4. If no waitlist exists, the seat status simply reverts to `'AVAILABLE'`.

---

### 5. Architectural Assumptions
- **User Authentication**: For simplicity in college demos, passwords are checked directly. In a production system, standard BCrypt hashing would be used.
- **Coach Class Fares**: Fares are flat rates configured in the Booking Server based on the class selected (SL = Rs. 250, 3A = Rs. 650, 2A = Rs. 1100, 1A = Rs. 1800).
- **Seat Allocation**: When searching, seat allocations for schedules are pre-allocated in bulk during train schedule creation using the cartesian product insert in `schema.sql`.

---

### 6. Database Schema Merging & Custom Optimization
To combine the user's existing database design with the distributed booking application, [existing.sql](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/existing.sql) was merged into [schema.sql](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/schema.sql) and normalized:
- **ENUM Type Adaptation & Cleanup**: Cleaned up all redundant/overlapping ENUM states. Aligned the system on the single state `'WAITING'` for waitlists and passenger statuses (eliminating `'WAITLIST'`). Cleaned up the payment ENUMs by retaining standard terms (like `'CREDIT_CARD'` and `'NET_BANKING'`) and handling user-friendly translations inside `BookingClient.java` (mapping `'CARD'` and `'NETBANKING'` inputs to their respective DB enums). Additionally, added explicit type casts (`CAST(? AS coach_type_enum)`, `CAST(? AS gender_enum)`, `CAST(? AS berth_preference_enum)`, `CAST(? AS booking_status_enum)`, `CAST(? AS payment_mode_enum)`) and uppercase normalization in Java SQL queries to prevent PostgreSQL operator comparison and insertion type-mismatch failures (e.g. `column "gender" is of type gender_enum but expression is of type character varying`).
- **Seat Allocation Normalization**: Completely removed the redundant `seat_id` column from the `booking_passengers` table. Instead, the server application queries seat details by joining `booking_passengers` with `seat_allocations` and `seats`, guaranteeing a fully normalized (3NF) relational design.
- **RMI Application Support**: Maintained critical columns needed for transaction processing and waitlist class tracking: `bookings.coach_type` and `waitlists.coach_type`.
- **Primary Key Standard**: Replaced old serials with standard Postgres identity columns (`GENERATED BY DEFAULT AS IDENTITY`), allowing explicit ID insertion for seed data.
- **Index Definitions**: Merged all query indexes from [existing.sql](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/existing.sql) (e.g., routes, bookings, allocations, waitlists) to optimize DB search performance.
- **IPv4 Connection Pooler Integration & preparedThreshold Fix**: Resolved the direct connection RMI thread timeout issue by switching from the direct IPv6-only hostname `db.tigpgunzlteutfykaeqd.supabase.co` to the Supabase IPv4 connection pooler `aws-0-ap-south-1.pooler.supabase.com` on port `6543`. Additionally, added the `prepareThreshold=0` parameter to the JDBC connection string to disable server-side prepared statements, avoiding PgBouncer transaction-mode conflicts (`prepared statement "S_1" already exists`).
- **Setup Manuals**: Created [README.md](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/README.md) compiling detailed, step-by-step compilation, configuration, execution, and distributed network testing commands for both Windows batch environments and Linux Ubuntu command-line terminals.

---

## Experiment 2: Multithreading & Database Concurrency Control
**Date:** 2026-08-15

### 1. Server-Side Multithreading Architecture (`ThreadPoolExecutor`)
In high-concurrency reservation scenarios, serial execution of incoming client requests leads to extreme latency bottlenecks.
- **Thread Pool Design**:
  - `corePoolSize`: 8 threads
  - `maximumPoolSize`: 32 threads
  - `keepAliveTime`: 60 seconds
  - `workQueue`: `ArrayBlockingQueue<Runnable>(100)` (bounded to prevent out-of-memory errors under sudden traffic spikes)
  - `rejectionHandler`: `CallerRunsPolicy` to gracefully slow down callers without dropping requests.
- **RMI Integration**:
  - The remote procedure `bookTatkalTicket` wraps booking transactions in a `Callable<BookingResult>` and submits it to `bookingExecutor`.
  - Future completion is awaited via `future.get()`, and all exceptions (`InterruptedException`, `ExecutionException`, `RejectedExecutionException`) are properly unwrapped and re-thrown as `RemoteException` without swallowing.
  - Server thread names (`[pool-1-thread-03]`) are logged for all stage transitions (request receipt, seat allocation, and commit/rollback).
  - A JVM shutdown hook is registered to call `executor.shutdown()` and `awaitTermination(5, TimeUnit.SECONDS)`.

---

### 2. PostgreSQL Non-Blocking Pessimistic Locking (`FOR UPDATE SKIP LOCKED`)
Java synchronized blocks (`public synchronized BookingResult...`) fail in distributed multi-node environments because JVM locks cannot synchronize across separate processes or machines. The database remains the authoritative single source of truth.
- **Query Optimization**:
  ```sql
  SELECT sa.allocation_id, sa.seat_id, s.seat_number, c.coach_number, s.berth_type
  FROM seat_allocations sa
  JOIN seats s ON sa.seat_id = s.seat_id
  JOIN coaches c ON s.coach_id = c.coach_id
  WHERE sa.schedule_id = ?
    AND c.coach_type = CAST(? AS coach_type_enum)
    AND sa.booking_status = 'AVAILABLE'
  ORDER BY s.seat_number
  LIMIT ?
  FOR UPDATE SKIP LOCKED;
  ```
- **Why `SKIP LOCKED` is Critical**:
  - Standard `FOR UPDATE` causes concurrent transactions seeking available seats to block and wait on locked rows, producing lock-wait timeouts and row contention deadlocks.
  - `FOR UPDATE SKIP LOCKED` instructs PostgreSQL to instantly bypass any rows currently locked by another active transaction. Threads immediately lock remaining available seats or discover that 0 seats remain, transitioning smoothly to waitlisting or returning without blocking contention delays.

---

### 3. Barrier Synchronization Test Harness (`CountDownLatch`)
To simulate realistic "Tatkal Opening" race conditions where many users click "Book" at the exact same millisecond:
- **Test Harness (`TatkalConcurrencyTest.java`)**:
  - Utilizes two `CountDownLatch` instances: `readyLatch` (count = N) and `startLatch` (count = 1).
  - N worker threads signal `readyLatch.countDown()` and block on `startLatch.await()`.
  - Once all N workers are ready, `startLatch.countDown()` releases all 20 threads simultaneously, creating an authentic concurrent race over RMI.
- **Assertions & Edge-Case Validation**:
  - Validates `total_requests == confirmed_bookings + waitlisted_bookings + failed_requests`.
  - Asserts `unique_allocated_seats == total_confirmed_seats` across single/multi-passenger requests (`CONCURRENCY TEST PASSED`).
- **Expanded Seed Dataset**:
  - Population of 10 stations, 5 trains, 30 journey schedules (2026-08-15 to 2026-08-20), and dynamic seat generation (72-seat SL/3A coaches, 54-seat 2A coaches, 48-seat Chair Exec, 24-seat 1A) using PostgreSQL `generate_series`.
  - Generates 1,000+ physical seats and 10,000+ date-specific `seat_allocations` to stress-test high concurrency and edge cases.

---

## Experiment 3: Clock Synchronization & Logical Event Ordering
**Date:** 2026-08-18

### 1. Physical Clock Synchronization — Cristian's Algorithm
- **Nodes**: Mumbai (Time Server, +5000 ms offset), Delhi (+10000 ms offset), Chennai (+1000 ms offset).
- **Formula**:
  - $T_0$ = local physical time immediately before RMI request
  - $ServerTime$ = physical time returned by Mumbai server
  - $T_1$ = local physical time immediately after receiving response
  - $RTT = T_1 - T_0$
  - $Estimated\ Network\ Delay = RTT / 2$
  - $Estimated\ Correct\ Time = ServerTime + (RTT / 2)$
  - $Adjustment = Estimated\ Correct\ Time - T_1$
- **Synchronization Window Validation**:
  - Evaluates $|clientPhysicalTime - serverPhysicalTime| \le 100\text{ ms}$.
  - Adjusts physical clock offsets dynamically and asserts `WITHIN ±100 ms WINDOW`.

---

### 2. Logical Event Ordering — Lamport Logical Clock
- **Rules Enforced**:
  - **Rule 1 (Local Event)**: $L = L + 1$
  - **Rule 2 (Send Event)**: $L = L + 1$, attach $L$ to `ClockMessage`
  - **Rule 3 (Receive Event)**: $L = \max(L, R) + 1$ when receiving remote timestamp $R$.
- **Independence**: Maintained complete separation between physical real-world time (`PhysicalClock`) and logical causality sequence (`LogicalClock`).

---

### 3. Dual-Timestamp Tatkal Booking Event Integration
Every reservation event carries both independent timestamps:
- `Node`: Node ID (e.g. `Delhi`)
- `PNR`: Unique 10-character reservation record
- `Physical Timestamp`: `2026-08-18 15:38:03.280` (synchronized via Cristian's algorithm)
- `Lamport Logical Timestamp`: `6` (ordered via Lamport rules)

---

### 4. Interactive CLI Dashboard Integration (`BookingClient.java`)
- Added Option 6 (`Synchronize Clock & View Timestamps (Exp 3)`) to the logged-in user dashboard menu.
- Executes Cristian's Algorithm synchronization directly over RMI and outputs calculations ($T_0$, $ServerTime$, $T_1$, $RTT$, $RTT/2$, $Adjustment$, $Window\ Status$).
- Displays the **DISTRIBUTED CLOCK TIMESTAMPS (EXP 3)** block on Tatkal booking confirmation receipts.

