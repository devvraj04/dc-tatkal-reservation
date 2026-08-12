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
