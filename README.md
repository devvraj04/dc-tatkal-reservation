# Distributed Tatkal Railway Reservation System

This is a distributed 3-tier railway booking application built for sem-5 Distributed Computing (DC). It utilizes **Java Remote Method Invocation (RMI)** for Remote Procedure Calls (RPC) between clients and the booking server, and connects securely to an online PostgreSQL database hosted on Supabase (using an IPv4 connection pooler).

---

## Prerequisites
- **Java Development Kit (JDK)**: JDK 8 or above installed (JDK 17, 21, or 23 recommended).
- **Internet Connection**: Required to connect to the online Supabase database and to download the PostgreSQL JDBC Driver JAR during compilation.

---

## 1. Database Setup
1. Execute the SQL script [schema.sql](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/schema.sql) on your PostgreSQL database (Supabase SQL Editor or PGAdmin). This creates the 15 normalized tables, indexes, and populates the database with seed data (default stations, trains, routes, schedules, coaches, seats, and a test user).
2. Configure your connection credentials in [db.properties](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/db.properties):
   ```properties
   db.url=jdbc:postgresql://<pooler-host>:6543/postgres?prepareThreshold=0
   db.user=postgres.<project-id>
   db.password=<your_database_password>
   ```
   > [!IMPORTANT]
   > The `prepareThreshold=0` parameter is **required** to disable server-side prepared statements, preventing conflicts on Supabase's transaction pooler (PgBouncer).

---

## 2. Compilation and Run Guide

### Option A: Windows OS

#### 1. Compile the Project
Double-click [compile.bat](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/compile.bat).
This script automatically:
- Creates `lib/` and `bin/` directories.
- Downloads the PostgreSQL JDBC driver via PowerShell.
- Compiles all Java files into `bin/`.

#### 2. Run the Server
Double-click [run_server.bat](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/run_server.bat).
This registers the RMI service programmatically on port `1099` and connects to the database.

#### 3. Run the Client
Double-click [run_client.bat](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/run_client.bat).
This launches the interactive command-line interface.

---

### Option B: Linux Ubuntu OS

#### 1. Compile the Project
Open a terminal in the project directory and run:
```bash
# Create target folders
mkdir -p lib bin

# Download PostgreSQL JDBC driver (if not already downloaded)
if [ ! -f "lib/postgresql-42.7.3.jar" ]; then
    echo "Downloading JDBC driver..."
    curl -L -o lib/postgresql-42.7.3.jar https://jdbc.postgresql.org/download/postgresql-42.7.3.jar
fi

# Compile all source files
javac -d bin -cp "lib/*:src" src/rmi/*.java src/server/*.java src/client/*.java
echo "Compilation complete."
```

#### 2. Run the Server
Execute the server using:
```bash
java -cp "lib/*:bin" server.BookingServer
```

#### 3. Run the Client
Execute the client using:
```bash
# Connects to localhost registry by default
java -cp "lib/*:bin" client.BookingClient
```

---

## 3. Distributed Setup (Testing Across Different Machines)

To test client-server interaction between two separate computers on the same local network:

### Step 1: Start Server with Bind Hostname Property
Java RMI requires the server to broadcast its actual network IP address (instead of localhost `127.0.0.1`), otherwise remote clients won't be able to communicate back.
- **Windows**: Edit [run_server.bat](file:///c:/Users/Devraj/Desktop/Sem5/DC/tatkal-reservation-system/run_server.bat) and modify the run line:
  ```batch
  java -Djava.rmi.server.hostname=<SERVER_IP_ADDRESS> -cp "lib/*;bin" server.BookingServer
  ```
- **Linux**: Start the server passing the VM system property:
  ```bash
  java -Djava.rmi.server.hostname=<SERVER_IP_ADDRESS> -cp "lib/*:bin" server.BookingServer
  ```
*(Replace `<SERVER_IP_ADDRESS>` with the server computer's actual LAN IP, e.g. `192.168.1.10`)*

### Step 2: Run Client with Server IP
Start the client application by passing the server's IP address:
- **Windows**: Run `run_client.bat <SERVER_IP_ADDRESS>` in Command Prompt.
- **Linux**: Run:
  ```bash
  java -cp "lib/*:bin" client.BookingClient <SERVER_IP_ADDRESS>
  ```

---

## 4. Test Credentials and Seeding Verification
- **Default Login User**: `devraj@example.com`
- **Default Password**: `password123`
- **Stations Available**: `CSMT` (Mumbai), `BRC` (Vadodara), `KOTA` (Kota), `NDLS` (New Delhi)
- **Train Schedule**: Train `12951` runs on `2026-08-15` and `2026-08-16` with 6 pre-allocated seats per class (SL, 3A, 2A).
