# Distributed Tatkal Railway Reservation System

An enterprise-grade, distributed 3-tier railway booking application built for **Distributed Computing (DC)**. This project demonstrates high-concurrency ticket reservation using **Java Remote Method Invocation (RMI)**, **Server Thread Pooling (`ThreadPoolExecutor`)**, **PostgreSQL Non-Blocking Pessimistic Locking (`FOR UPDATE OF sa SKIP LOCKED`)**, **Distributed Bully Leader Election**, **Distributed Clock Synchronization (Cristian's Algorithm & Lamport Logical Clocks)**, and a modern, sleek **Next.js Web Frontend** using the custom design color palette.

---

## 🎨 Design System & Color Palette

The frontend is styled using the specified design palette:
- **Primary / Action**: Warm Terracotta Amber (`#D9874C`)
- **Background**: Soft Warm Cream (`#FEFDF0`, `#F0EDD8`)
- **Secondary / Nature**: Sage Green (`#8FAB7E`)
- **Accent / Muted**: Light Slate Sage (`#A5BF96`, `#9DB08E`)
- **Text & Borders**: Rich Deep Bronze (`#3D2B1F`), Warm Sand Border (`#D5C9A8`)

---

## 🚀 Quick Start: Running the Frontend & Backend

### 1. Launch Next.js Web Frontend
Double-click [run_frontend.bat](file:///c:/Users/Atharv/OneDrive/Desktop/dc-tatkal-reservation/run_frontend.bat) or run:
```bash
cd tatkal-frontend
npx next start -p 3000
```
Open **[http://localhost:3000](http://localhost:3000)** in your browser.

**Demo Credentials**:
- **Email**: `devraj@example.com` / `rahul@example.com`
- **Password**: `password123`

### 2. Launch Java RMI Server (Backend)
Double-click [run_server.bat](file:///c:/Users/Atharv/OneDrive/Desktop/dc-tatkal-reservation/run_server.bat) or run:
```cmd
run_server.bat
```
*(Binds `TatkalService` & `BookingService` on port `1099`)*

### 3. Run Java CLI Client
Double-click [run_client.bat](file:///c:/Users/Atharv/OneDrive/Desktop/dc-tatkal-reservation/run_client.bat) or run:
```cmd
run_client.bat
```

---

## 📌 Features & Experiments Covered

### 🔹 1. Next.js Web Frontend & REST Bridge
- **Authentication**: User session management with fast credential auto-fill for testing.
- **Train Search**: Dynamic station lookup from database, date filtering, and schedule discovery.
- **Seat Availability**: Real-time breakdown of available vs total seats per coach class (`1A`, `2A`, `3A`, `SL`).
- **Tatkal Ticket Reservation**: Multi-passenger booking (up to 6 passengers) with pre-saved profile auto-fill, berth preferences, and row-level locking (`FOR UPDATE SKIP LOCKED`).
- **Ticket Cancellation**: Real-time cancellation charge deduction (₹100/seat) and instant waitlist passenger promotion.
- **Booking History**: Expandable history cards showing PNR, assigned coach/seat numbers, status badges, and booking timestamp.
- **Bully Leader Election**: Interactive election initiator, leader failure simulation, and database synchronization (`users.is_leader`).
- **Clock Algorithms (Exp 3)**: Cristian's algorithm physical sync (±100ms window) and Lamport logical clock message passing.

### 🔹 2. Remote Procedure Call (RPC) / Java RMI
- Strongly typed Data Transfer Objects (DTOs) implementing `java.io.Serializable`.
- Local/Remote PostgreSQL integration.
- Full parity between Java RMI functions and Next.js REST API routes.

### 🔹 3. Multithreading & Database Concurrency Control
- **Server Thread Pooling (`ThreadPoolExecutor`)**: Managed pool (8 core, 32 max threads, 100 bounded queue, `CallerRunsPolicy`).
- **Pessimistic Locking (`FOR UPDATE OF sa SKIP LOCKED`)**: Non-blocking row-level lock scoped exclusively to `seat_allocations` (`OF sa`), allowing concurrent transactions to dynamically claim available seats without waiting on locked rows.
- **Barrier Synchronization Test Suite (`CountDownLatch`)**: 50 concurrent client threads testing seat uniqueness.

### 🔹 4. Clock Synchronization & Logical Event Ordering
- **Physical Clock (Cristian's Algorithm)**:
  - $\text{Estimated Correct Time} = \text{ServerTime} + (\text{RTT} / 2)$.
  - Validates physical clock alignment against $\pm 100\text{ ms}$ window.
- **Logical Clock (Lamport Logical Clock)**:
  - Enforces strict Lamport causal ordering rules ($L = L + 1$, send, receive).
- **Dual-Timestamp Tatkal Booking**:
  - Reservation events carry both physical real-world timestamps and Lamport logical timestamps.

---

## 📁 Repository Structure Overview

```
dc-tatkal-reservation/
├── tatkal-frontend/              # Complete Next.js Web Frontend
│   ├── app/                      # App router pages & API routes
│   │   ├── api/                  # REST endpoints mirroring Java RMI functions
│   │   │   ├── availability/     # GET seat availability
│   │   │   ├── book/             # POST Tatkal ticket booking
│   │   │   ├── cancel/           # POST Ticket cancellation & waitlist promotion
│   │   │   ├── clock-sync/       # POST Cristian's algorithm & Lamport clocks
│   │   │   ├── history/          # GET User booking history
│   │   │   ├── leader/           # GET/POST Bully leader status & update
│   │   │   ├── login/            # POST User authentication
│   │   │   ├── passengers/       # GET Pre-saved user passenger profiles
│   │   │   ├── schedules/        # GET Active train schedules
│   │   │   ├── search-trains/    # GET Dynamic train search
│   │   │   ├── stations/         # GET Dynamic stations list
│   │   │   └── users/            # GET Participating nodes
│   │   ├── book/                 # Tatkal booking page
│   │   ├── cancel/               # Cancellation & refund page
│   │   ├── clock/                # Interactive clock synchronization page
│   │   ├── dashboard/            # User overview & quick actions
│   │   ├── election/             # Interactive Bully election page
│   │   ├── history/              # Booking history page
│   │   ├── search/               # Train search page
│   │   └── page.tsx              # Modern login page
│   ├── components/               # Navbar & UI primitive components (Card, Button, etc.)
│   ├── context/                  # AuthContext provider
│   ├── lib/                      # Database pool & TypeScript types
│   └── tailwind.config.ts        # Custom palette configuration
├── schema/
│   └── schema.sql                # Complete database schema and seed data
├── src/
│   ├── rmi/                      # Remote interface & Serializable DTOs
│   ├── server/                   # Server & RMI Implementation
│   ├── client/                   # Java Client applications & test harnesses
│   └── clock/                    # Cristian's & Lamport clock implementations
├── run_frontend.bat              # Script to start Next.js frontend
├── run_server.bat                # Script to start Java RMI server
├── run_client.bat                # Script to start Java CLI client
├── run_clock_demo.bat            # Script to run clock demo
├── run_election_demo.bat         # Script to run Bully election demo
└── run_concurrency_test.bat      # Script to run concurrency test harness
```

---

## 🔑 Default Seed Data Credentials
- **User Email**: `devraj@example.com` / `rahul@example.com`
- **Password**: `password123`
- **Trains Available**: `12951` (Mumbai-Delhi Rajdhani), `12002` (Delhi-Bhopal Shatabdi), `12301` (Howrah Rajdhani), `22691` (Bengaluru Rajdhani), `12123` (Deccan Queen).
