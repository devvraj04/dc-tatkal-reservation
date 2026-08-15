# Distributed Tatkal Railway Reservation System

An enterprise-grade, distributed 3-tier railway booking application built for **Sem-5 Distributed Computing (DC)**. This project demonstrates high-concurrency ticket reservation using **Java Remote Method Invocation (RMI)**, **Server Thread Pooling (`ThreadPoolExecutor`)**, and **PostgreSQL Non-Blocking Pessimistic Locking (`FOR UPDATE OF sa SKIP LOCKED`)**, connected to a cloud PostgreSQL database hosted on **Supabase**.

---

## 📌 Experiments Covered

### 🔹 Experiment 1: Remote Procedure Call (RPC) / Java RMI
- **Objective**: Enable multi-tier client-server communication using Java RMI.
- **Architecture**: The client CLI application never touches the database directly. It invokes remote procedures (`login`, `searchTrain`, `checkAvailability`, `bookTatkalTicket`, `cancelTicket`, `getBookingHistory`) on the RMI Booking Server.
- **Features**:
  - Strongly typed Data Transfer Objects (DTOs) implementing `java.io.Serializable`.
  - Cloud PostgreSQL integration via Supabase IPv4 connection pooler (`prepareThreshold=0`).
  - Interactive CLI dashboard with pre-saved passenger profile loading.

### 🔹 Experiment 2: Multithreading & Database Concurrency Control
- **Objective**: Handle simultaneous high-volume Tatkal ticket booking requests safely without duplicate seat allocations or system deadlocks.
- **Architecture**:
  1. **Server-Side Thread Pooling (`ThreadPoolExecutor`)**: Managed pool (8 core, 32 max threads, 100 bounded queue, `CallerRunsPolicy`) executing concurrent booking transactions via `Callable<BookingResult>`.
  2. **PostgreSQL Pessimistic Locking (`FOR UPDATE OF sa SKIP LOCKED`)**: Non-blocking row-level lock scoped exclusively to `seat_allocations` (`OF sa`), allowing concurrent transactions to dynamically claim available seats without waiting on locked rows or locking multi-table `JOIN` dependencies (`coaches`/`seats`).
  3. **Barrier Synchronization Test Suite (`CountDownLatch`)**: Launching 50 concurrent client threads simultaneously over RMI to assert seat uniqueness and zero duplicate allocations (`CONCURRENCY TEST PASSED`).

---

## ⚙️ Prerequisites & Setup

### Requirements
- **Java Development Kit (JDK)**: JDK 8 or above (JDK 17, 21, or 23 recommended).
- **Internet Connection**: Required for connecting to cloud PostgreSQL on Supabase and downloading the JDBC Driver.

### 1. Database Setup
1. Execute the SQL script [schema/schema.sql](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/schema/schema.sql) in your Supabase SQL Editor or PGAdmin.
   - Creates 15 normalized tables, indexes, ENUM types, and populates realistic seed data (10 stations, 5 trains, 30 journey schedules, multi-coach compositions, and 1,000+ dynamic seat allocations).
2. Configure credentials in [db.properties](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/db.properties):
   ```properties
   db.url=jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:6543/postgres?prepareThreshold=0
   db.user=postgres.<your-project-id>
   db.password=<your_password>
   ```
   > [!IMPORTANT]
   > The `prepareThreshold=0` parameter is mandatory to disable server-side prepared statements and prevent PgBouncer transaction-mode conflicts.

---

## 🚀 Compilation & Execution Guide

### Option A: Windows OS

#### 1. Compile the Project
Double-click [compile.bat](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/compile.bat) or run in Command Prompt:
```cmd
compile.bat
```
*(Automatically downloads `postgresql-42.7.3.jar` into `lib/` and compiles all sources into `bin/`)*

#### 2. Run the Booking Server
Double-click [run_server.bat](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/run_server.bat) or run:
```cmd
run_server.bat
```
*(Binds `TatkalService` and `BookingService` on RMI port `1099`)*

#### 3. Run the Interactive Client CLI (Experiment 1)
Double-click [run_client.bat](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/run_client.bat) or run:
```cmd
run_client.bat
```

#### 4. Run the Multithreading Concurrency Test Suite (Experiment 2)
Double-click [run_concurrency_test.bat](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/run_concurrency_test.bat) or run:
```cmd
run_concurrency_test.bat
```
- **Custom Environment Variables**:
  ```cmd
  set TEST_CLIENTS=50
  set TEST_SCHEDULE_ID=1
  set TEST_COACH_TYPE=SL
  run_concurrency_test.bat
  ```

#### 5. Run the Unsafe Race Condition Demo
```cmd
java -cp "lib/*;bin" client.UnsafeBookingDemo
```

---

### Option B: Linux Ubuntu OS

#### 1. Compile the Project
```bash
mkdir -p lib bin
if [ ! -f "lib/postgresql-42.7.3.jar" ]; then
    curl -L -o lib/postgresql-42.7.3.jar https://jdbc.postgresql.org/download/postgresql-42.7.3.jar
fi
javac -d bin -cp "lib/*:src" src/rmi/*.java src/server/*.java src/client/*.java src/com/tatkal/client/*.java
```

#### 2. Run the Server
```bash
java -cp "lib/*:bin" server.BookingServer
```

#### 3. Run the Client CLI
```bash
java -cp "lib/*:bin" client.BookingClient
```

#### 4. Run the Concurrency Test Suite
```bash
TEST_CLIENTS=50 TEST_SCHEDULE_ID=1 java -cp "lib/*:bin" client.TatkalConcurrencyTest
```

---

## 🌐 Distributed Setup (Testing Across Different Machines)

To connect remote clients to the Booking Server across a Local Area Network (LAN):

1. **Start Server with Hostname IP**:
   ```cmd
   java -Djava.rmi.server.hostname=<SERVER_IP_ADDRESS> -cp "lib/*;bin" server.BookingServer
   ```
2. **Run Remote Client**:
   ```cmd
   java -cp "lib/*;bin" client.BookingClient <SERVER_IP_ADDRESS>
   ```

---

## 📁 Repository Structure Overview

```
tatkal-reservation-system/
├── schema/
│   └── schema.sql                # Complete database schema and seed data
├── src/
│   ├── rmi/                      # Remote interface & Serializable DTOs
│   │   ├── BookingService.java
│   │   ├── BookingResult.java
│   │   ├── CoachAvailability.java
│   │   ├── PassengerInput.java
│   │   └── UserSession.java
│   ├── server/                   # Server & RMI Implementation
│   │   ├── BookingServer.java     # RMI Registry setup & binding
│   │   ├── BookingServiceImpl.java# ThreadPoolExecutor & FOR UPDATE OF sa SKIP LOCKED
│   │   └── DBConnection.java     # JDBC connection manager
│   ├── client/                   # Client applications
│   │   ├── BookingClient.java    # Interactive CLI User Dashboard
│   │   ├── TatkalConcurrencyTest.java # 50-client barrier test harness
│   │   └── UnsafeBookingDemo.java # Race condition hazard demo
│   └── com/tatkal/client/
│       └── TatkalConcurrencyTest.java # Package alias wrapper
├── docs/
│   ├── changes.md                # Dated change logs
│   └── logs.md                   # Technical design & architecture log
├── db.properties                 # PostgreSQL database configuration
├── compile.bat                   # Automated compilation batch script
├── run_server.bat                # Server execution script
├── run_client.bat                # Client CLI execution script
└── run_concurrency_test.bat      # Concurrency test execution script
```

---

## 🔑 Default Seed Data Credentials
- **User Email**: `devraj@example.com`
- **Password**: `password123`
- **Trains Available**: `12951` (Mumbai-Delhi Rajdhani), `12002` (Delhi-Bhopal Shatabdi), `12301` (Howrah Rajdhani), `22691` (Bengaluru Rajdhani), `12123` (Deccan Queen).
- **Target Schedule**: Schedule ID `1` (Train `12951` on `2026-08-15`).
