-- ============================================================
-- DISTRIBUTED TATKAL RAILWAY RESERVATION SYSTEM
-- FINAL FUNCTIONAL DATABASE
-- PostgreSQL
-- ============================================================

BEGIN;

-- ============================================================
-- 1. CLEAN EXISTING TABLES
-- ============================================================

DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS cancellations CASCADE;
DROP TABLE IF EXISTS waitlists CASCADE;
DROP TABLE IF EXISTS payments CASCADE;
DROP TABLE IF EXISTS booking_passengers CASCADE;
DROP TABLE IF EXISTS seat_allocations CASCADE;
DROP TABLE IF EXISTS bookings CASCADE;
DROP TABLE IF EXISTS seats CASCADE;
DROP TABLE IF EXISTS coaches CASCADE;
DROP TABLE IF EXISTS train_schedules CASCADE;
DROP TABLE IF EXISTS routes CASCADE;
DROP TABLE IF EXISTS passengers CASCADE;
DROP TABLE IF EXISTS trains CASCADE;
DROP TABLE IF EXISTS stations CASCADE;
DROP TABLE IF EXISTS users CASCADE;


-- ============================================================
-- 2. DROP ENUM TYPES IF THEY ALREADY EXIST
-- ============================================================

DROP TYPE IF EXISTS account_status_enum CASCADE;
DROP TYPE IF EXISTS gender_enum CASCADE;
DROP TYPE IF EXISTS berth_preference_enum CASCADE;
DROP TYPE IF EXISTS train_type_enum CASCADE;
DROP TYPE IF EXISTS train_status_enum CASCADE;
DROP TYPE IF EXISTS schedule_status_enum CASCADE;
DROP TYPE IF EXISTS coach_type_enum CASCADE;
DROP TYPE IF EXISTS coach_status_enum CASCADE;
DROP TYPE IF EXISTS berth_type_enum CASCADE;
DROP TYPE IF EXISTS seat_status_enum CASCADE;
DROP TYPE IF EXISTS booking_type_enum CASCADE;
DROP TYPE IF EXISTS quota_enum CASCADE;
DROP TYPE IF EXISTS booking_status_enum CASCADE;
DROP TYPE IF EXISTS allocation_status_enum CASCADE;
DROP TYPE IF EXISTS allocation_booking_type_enum CASCADE;
DROP TYPE IF EXISTS passenger_status_enum CASCADE;
DROP TYPE IF EXISTS payment_mode_enum CASCADE;
DROP TYPE IF EXISTS payment_status_enum CASCADE;
DROP TYPE IF EXISTS waitlist_type_enum CASCADE;
DROP TYPE IF EXISTS waitlist_status_enum CASCADE;
DROP TYPE IF EXISTS cancellation_status_enum CASCADE;
DROP TYPE IF EXISTS notification_type_enum CASCADE;
DROP TYPE IF EXISTS delivery_status_enum CASCADE;


-- ============================================================
-- 3. ENUM TYPES
-- ============================================================

CREATE TYPE account_status_enum AS ENUM (
    'ACTIVE',
    'SUSPENDED',
    'LOCKED',
    'DEACTIVATED'
);

CREATE TYPE gender_enum AS ENUM (
    'MALE',
    'FEMALE',
    'OTHER'
);

CREATE TYPE berth_preference_enum AS ENUM (
    'LOWER',
    'MIDDLE',
    'UPPER',
    'SIDE_LOWER',
    'SIDE_UPPER',
    'NO_PREFERENCE'
);

CREATE TYPE train_type_enum AS ENUM (
    'EXPRESS',
    'SUPERFAST',
    'RAJDHANI',
    'SHATABDI',
    'DURONTO',
    'VANDE_BHARAT',
    'MAIL',
    'PASSENGER'
);

CREATE TYPE train_status_enum AS ENUM (
    'ACTIVE',
    'CANCELLED',
    'SUSPENDED'
);

CREATE TYPE schedule_status_enum AS ENUM (
    'SCHEDULED',
    'BOARDING',
    'RUNNING',
    'COMPLETED',
    'CANCELLED'
);

CREATE TYPE coach_type_enum AS ENUM (
    'SL',
    '3A',
    '2A',
    '1A',
    'CC',
    'EC',
    '2S'
);

CREATE TYPE coach_status_enum AS ENUM (
    'ACTIVE',
    'MAINTENANCE',
    'REMOVED'
);

CREATE TYPE berth_type_enum AS ENUM (
    'LOWER',
    'MIDDLE',
    'UPPER',
    'SIDE_LOWER',
    'SIDE_UPPER',
    'CHAIR'
);

CREATE TYPE seat_status_enum AS ENUM (
    'AVAILABLE',
    'MAINTENANCE',
    'BLOCKED'
);

CREATE TYPE booking_type_enum AS ENUM (
    'GENERAL',
    'TATKAL',
    'PREMIUM_TATKAL'
);

CREATE TYPE quota_enum AS ENUM (
    'GENERAL',
    'TATKAL',
    'PREMIUM_TATKAL',
    'LADIES',
    'SENIOR_CITIZEN',
    'DIVYANG',
    'OTHER'
);

CREATE TYPE booking_status_enum AS ENUM (
    'PENDING',
    'CONFIRMED',
    'RAC',
    'WAITING',
    'CANCELLED',
    'PAYMENT_FAILED'
);

CREATE TYPE allocation_status_enum AS ENUM (
    'AVAILABLE',
    'HELD',
    'BOOKED',
    'RAC',
    'BLOCKED'
);

CREATE TYPE allocation_booking_type_enum AS ENUM (
    'NONE',
    'GENERAL',
    'TATKAL',
    'PREMIUM_TATKAL'
);

CREATE TYPE passenger_status_enum AS ENUM (
    'CONFIRMED',
    'RAC',
    'WAITING',
    'CANCELLED'
);

CREATE TYPE payment_mode_enum AS ENUM (
    'UPI',
    'CREDIT_CARD',
    'DEBIT_CARD',
    'NET_BANKING',
    'WALLET'
);

CREATE TYPE payment_status_enum AS ENUM (
    'INITIATED',
    'SUCCESS',
    'FAILED',
    'REFUNDED',
    'PARTIALLY_REFUNDED'
);

CREATE TYPE waitlist_type_enum AS ENUM (
    'GNWL',
    'RLWL',
    'PQWL',
    'TQWL'
);

CREATE TYPE waitlist_status_enum AS ENUM (
    'WAITING',
    'CONFIRMED',
    'CANCELLED'
);

CREATE TYPE cancellation_status_enum AS ENUM (
    'REQUESTED',
    'PROCESSED',
    'FAILED'
);

CREATE TYPE notification_type_enum AS ENUM (
    'BOOKING_CONFIRMATION',
    'PAYMENT_SUCCESS',
    'PAYMENT_FAILED',
    'CANCELLATION',
    'WAITLIST_UPDATE',
    'TATKAL_OPENING',
    'GENERAL'
);

CREATE TYPE delivery_status_enum AS ENUM (
    'PENDING',
    'SENT',
    'FAILED',
    'READ'
);


-- ============================================================
-- 4. USERS
-- ============================================================

CREATE TABLE users (
    user_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    full_name VARCHAR(100) NOT NULL,

    email VARCHAR(100) NOT NULL UNIQUE,

    mobile_no VARCHAR(15) NOT NULL UNIQUE,

    password_hash VARCHAR(255) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    last_login TIMESTAMP,

    account_status account_status_enum NOT NULL DEFAULT 'ACTIVE'
);


-- ============================================================
-- 5. STATIONS
-- ============================================================

CREATE TABLE stations (
    station_code VARCHAR(10) PRIMARY KEY,

    station_name VARCHAR(100) NOT NULL,

    city VARCHAR(100) NOT NULL,

    state VARCHAR(100) NOT NULL,

    railway_zone VARCHAR(100) NOT NULL
);


-- ============================================================
-- 6. TRAINS
-- ============================================================

CREATE TABLE trains (
    train_no INTEGER PRIMARY KEY,

    train_name VARCHAR(100) NOT NULL,

    train_type train_type_enum NOT NULL,

    source_station_code VARCHAR(10) NOT NULL,

    destination_station_code VARCHAR(10) NOT NULL,

    total_distance INTEGER NOT NULL,

    running_days VARCHAR(20) NOT NULL,

    train_status train_status_enum NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT fk_train_source
        FOREIGN KEY (source_station_code)
        REFERENCES stations(station_code)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_train_destination
        FOREIGN KEY (destination_station_code)
        REFERENCES stations(station_code)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT chk_train_distance
        CHECK (total_distance > 0),

    CONSTRAINT chk_train_different_stations
        CHECK (source_station_code <> destination_station_code)
);


-- ============================================================
-- 7. PASSENGERS
-- ============================================================

CREATE TABLE passengers (
    passenger_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    user_id BIGINT NOT NULL,

    passenger_name VARCHAR(100) NOT NULL,

    age INTEGER NOT NULL,

    gender gender_enum NOT NULL,

    berth_preference berth_preference_enum
        NOT NULL DEFAULT 'NO_PREFERENCE',

    nationality VARCHAR(50) NOT NULL DEFAULT 'Indian',

    id_proof_type VARCHAR(30),

    id_proof_number VARCHAR(50),

    CONSTRAINT fk_passenger_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT chk_passenger_age
        CHECK (age BETWEEN 0 AND 120)
);


-- ============================================================
-- 8. ROUTES
-- ============================================================

CREATE TABLE routes (
    route_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    train_no INTEGER NOT NULL,

    station_code VARCHAR(10) NOT NULL,

    stop_number INTEGER NOT NULL,

    arrival_time TIME,

    departure_time TIME,

    day_number INTEGER NOT NULL DEFAULT 1,

    platform_number VARCHAR(20),

    CONSTRAINT fk_route_train
        FOREIGN KEY (train_no)
        REFERENCES trains(train_no)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_route_station
        FOREIGN KEY (station_code)
        REFERENCES stations(station_code)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT uq_route_train_stop
        UNIQUE (train_no, stop_number),

    CONSTRAINT uq_route_train_station
        UNIQUE (train_no, station_code),

    CONSTRAINT chk_route_stop
        CHECK (stop_number > 0),

    CONSTRAINT chk_route_day
        CHECK (day_number > 0)
);


-- ============================================================
-- 9. TRAIN SCHEDULES
-- ============================================================

CREATE TABLE train_schedules (
    schedule_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    train_no INTEGER NOT NULL,

    journey_date DATE NOT NULL,

    departure_datetime TIMESTAMP NOT NULL,

    arrival_datetime TIMESTAMP NOT NULL,

    schedule_status schedule_status_enum
        NOT NULL DEFAULT 'SCHEDULED',

    CONSTRAINT fk_schedule_train
        FOREIGN KEY (train_no)
        REFERENCES trains(train_no)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT uq_schedule_train_date
        UNIQUE (train_no, journey_date),

    CONSTRAINT chk_schedule_time
        CHECK (arrival_datetime > departure_datetime)
);


-- ============================================================
-- 10. COACHES
-- ============================================================

CREATE TABLE coaches (
    coach_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    train_no INTEGER NOT NULL,

    coach_number VARCHAR(10) NOT NULL,

    coach_type coach_type_enum NOT NULL,

    total_seats INTEGER NOT NULL,

    coach_status coach_status_enum
        NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT fk_coach_train
        FOREIGN KEY (train_no)
        REFERENCES trains(train_no)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT uq_coach_train_number
        UNIQUE (train_no, coach_number),

    CONSTRAINT chk_coach_seats
        CHECK (total_seats > 0)
);


-- ============================================================
-- 11. SEATS
-- ============================================================

CREATE TABLE seats (
    seat_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    coach_id BIGINT NOT NULL,

    seat_number INTEGER NOT NULL,

    berth_type berth_type_enum NOT NULL,

    seat_status seat_status_enum
        NOT NULL DEFAULT 'AVAILABLE',

    CONSTRAINT fk_seat_coach
        FOREIGN KEY (coach_id)
        REFERENCES coaches(coach_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT uq_seat_coach_number
        UNIQUE (coach_id, seat_number),

    CONSTRAINT chk_seat_number
        CHECK (seat_number > 0)
);


-- ============================================================
-- 12. BOOKINGS
-- ============================================================

CREATE TABLE bookings (
    pnr VARCHAR(20) PRIMARY KEY,

    user_id BIGINT NOT NULL,

    schedule_id BIGINT NOT NULL,

    source_station_code VARCHAR(10) NOT NULL,

    destination_station_code VARCHAR(10) NOT NULL,

    booking_type booking_type_enum NOT NULL,

    quota quota_enum NOT NULL DEFAULT 'GENERAL',

    booking_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    booking_status booking_status_enum
        NOT NULL DEFAULT 'PENDING',

    total_fare NUMERIC(10,2) NOT NULL,

    cancellation_time TIMESTAMP,

    CONSTRAINT fk_booking_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_booking_schedule
        FOREIGN KEY (schedule_id)
        REFERENCES train_schedules(schedule_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_booking_source
        FOREIGN KEY (source_station_code)
        REFERENCES stations(station_code)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_booking_destination
        FOREIGN KEY (destination_station_code)
        REFERENCES stations(station_code)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT chk_booking_fare
        CHECK (total_fare >= 0),

    CONSTRAINT chk_booking_stations
        CHECK (source_station_code <> destination_station_code)
);


-- ============================================================
-- 13. SEAT ALLOCATIONS
-- ============================================================
-- One physical seat can have a different state for each
-- journey date.
--
-- Example:
--
-- Seat B1-25
-- Schedule 101 -> BOOKED
-- Schedule 102 -> AVAILABLE
-- Schedule 103 -> RAC
--
-- ============================================================

CREATE TABLE seat_allocations (
    allocation_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    schedule_id BIGINT NOT NULL,

    seat_id BIGINT NOT NULL,

    booking_status allocation_status_enum
        NOT NULL DEFAULT 'AVAILABLE',

    booking_type allocation_booking_type_enum
        NOT NULL DEFAULT 'NONE',

    pnr VARCHAR(20),

    allocated_at TIMESTAMP,

    CONSTRAINT fk_allocation_schedule
        FOREIGN KEY (schedule_id)
        REFERENCES train_schedules(schedule_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_allocation_seat
        FOREIGN KEY (seat_id)
        REFERENCES seats(seat_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_allocation_booking
        FOREIGN KEY (pnr)
        REFERENCES bookings(pnr)
        ON UPDATE CASCADE
        ON DELETE SET NULL,

    CONSTRAINT uq_allocation_schedule_seat
        UNIQUE (schedule_id, seat_id)
);


-- ============================================================
-- 14. BOOKING PASSENGERS
-- ============================================================
-- A single PNR can contain multiple passengers.
--
-- Booking
--    |
--    +---- Passenger 1
--    +---- Passenger 2
--    +---- Passenger 3
--
-- ============================================================

CREATE TABLE booking_passengers (
    booking_passenger_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    pnr VARCHAR(20) NOT NULL,

    passenger_id BIGINT NOT NULL,

    allocation_id BIGINT,

    passenger_status passenger_status_enum
        NOT NULL DEFAULT 'WAITING',

    fare NUMERIC(10,2) NOT NULL,

    CONSTRAINT fk_booking_passenger_booking
        FOREIGN KEY (pnr)
        REFERENCES bookings(pnr)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_booking_passenger_passenger
        FOREIGN KEY (passenger_id)
        REFERENCES passengers(passenger_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_booking_passenger_allocation
        FOREIGN KEY (allocation_id)
        REFERENCES seat_allocations(allocation_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL,

    CONSTRAINT uq_booking_passenger
        UNIQUE (pnr, passenger_id),

    CONSTRAINT chk_booking_passenger_fare
        CHECK (fare >= 0)
);


-- ============================================================
-- 15. PAYMENTS
-- ============================================================

CREATE TABLE payments (
    payment_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    pnr VARCHAR(20) NOT NULL,

    amount NUMERIC(10,2) NOT NULL,

    payment_mode payment_mode_enum NOT NULL,

    transaction_id VARCHAR(100) NOT NULL UNIQUE,

    payment_status payment_status_enum
        NOT NULL DEFAULT 'INITIATED',

    payment_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payment_booking
        FOREIGN KEY (pnr)
        REFERENCES bookings(pnr)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT chk_payment_amount
        CHECK (amount >= 0)
);


-- ============================================================
-- 16. WAITLISTS
-- ============================================================

CREATE TABLE waitlists (
    waitlist_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    pnr VARCHAR(20) NOT NULL,

    schedule_id BIGINT NOT NULL,

    waitlist_number INTEGER NOT NULL,

    current_position INTEGER NOT NULL,

    waitlist_type waitlist_type_enum
        NOT NULL DEFAULT 'TQWL',

    status waitlist_status_enum
        NOT NULL DEFAULT 'WAITING',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    confirmed_at TIMESTAMP,

    CONSTRAINT fk_waitlist_booking
        FOREIGN KEY (pnr)
        REFERENCES bookings(pnr)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_waitlist_schedule
        FOREIGN KEY (schedule_id)
        REFERENCES train_schedules(schedule_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT chk_waitlist_number
        CHECK (waitlist_number > 0),

    CONSTRAINT chk_waitlist_position
        CHECK (current_position > 0)
);


-- ============================================================
-- 17. CANCELLATIONS
-- ============================================================

CREATE TABLE cancellations (
    cancellation_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    pnr VARCHAR(20) NOT NULL,

    cancelled_by BIGINT NOT NULL,

    cancellation_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    refund_amount NUMERIC(10,2) NOT NULL DEFAULT 0.00,

    cancellation_charge NUMERIC(10,2) NOT NULL DEFAULT 0.00,

    cancellation_status cancellation_status_enum
        NOT NULL DEFAULT 'REQUESTED',

    CONSTRAINT fk_cancellation_booking
        FOREIGN KEY (pnr)
        REFERENCES bookings(pnr)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_cancellation_user
        FOREIGN KEY (cancelled_by)
        REFERENCES users(user_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT chk_refund_amount
        CHECK (refund_amount >= 0),

    CONSTRAINT chk_cancellation_charge
        CHECK (cancellation_charge >= 0)
);


-- ============================================================
-- 18. NOTIFICATIONS
-- ============================================================

CREATE TABLE notifications (
    notification_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    user_id BIGINT NOT NULL,

    pnr VARCHAR(20),

    notification_type notification_type_enum NOT NULL,

    message TEXT NOT NULL,

    sent_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    delivery_status delivery_status_enum
        NOT NULL DEFAULT 'PENDING',

    CONSTRAINT fk_notification_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_notification_booking
        FOREIGN KEY (pnr)
        REFERENCES bookings(pnr)
        ON UPDATE CASCADE
        ON DELETE SET NULL
);


-- ============================================================
-- 19. INDEXES
-- ============================================================

CREATE INDEX idx_passengers_user_id
    ON passengers(user_id);

CREATE INDEX idx_trains_source
    ON trains(source_station_code);

CREATE INDEX idx_trains_destination
    ON trains(destination_station_code);

CREATE INDEX idx_routes_train
    ON routes(train_no);

CREATE INDEX idx_routes_station
    ON routes(station_code);

CREATE INDEX idx_schedules_train
    ON train_schedules(train_no);

CREATE INDEX idx_schedules_date
    ON train_schedules(journey_date);

CREATE INDEX idx_coaches_train
    ON coaches(train_no);

CREATE INDEX idx_seats_coach
    ON seats(coach_id);

CREATE INDEX idx_bookings_user
    ON bookings(user_id);

CREATE INDEX idx_bookings_schedule
    ON bookings(schedule_id);

CREATE INDEX idx_bookings_status
    ON bookings(booking_status);

CREATE INDEX idx_bookings_timestamp
    ON bookings(booking_timestamp);

CREATE INDEX idx_allocations_schedule
    ON seat_allocations(schedule_id);

CREATE INDEX idx_allocations_status
    ON seat_allocations(booking_status);

CREATE INDEX idx_allocations_pnr
    ON seat_allocations(pnr);

CREATE INDEX idx_booking_passengers_pnr
    ON booking_passengers(pnr);

CREATE INDEX idx_booking_passengers_passenger
    ON booking_passengers(passenger_id);

CREATE INDEX idx_payments_pnr
    ON payments(pnr);

CREATE INDEX idx_payments_status
    ON payments(payment_status);

CREATE INDEX idx_waitlists_schedule
    ON waitlists(schedule_id);

CREATE INDEX idx_waitlists_status
    ON waitlists(status);

CREATE INDEX idx_cancellations_pnr
    ON cancellations(pnr);

CREATE INDEX idx_cancellations_user
    ON cancellations(cancelled_by);

CREATE INDEX idx_notifications_user
    ON notifications(user_id);

CREATE INDEX idx_notifications_pnr
    ON notifications(pnr);

CREATE INDEX idx_notifications_status
    ON notifications(delivery_status);


-- ============================================================
-- 20. FINISH TRANSACTION
-- ============================================================

COMMIT;

-- ============================================================
-- DATABASE CREATION COMPLETE
-- ============================================================