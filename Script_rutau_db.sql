-- =========================================================
-- Script de creación de tablas - PostgreSQL
-- RutaU - Carpooling Universitario
-- (users, vehicles, trips, trip_stops, seat_requests,
--  ratings, reports, notifications)
-- Base de datos: RutaU_DB
-- =========================================================

-- Eliminar tablas si existen (orden inverso por dependencias)
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS reports CASCADE;
DROP TABLE IF EXISTS ratings CASCADE;
DROP TABLE IF EXISTS seat_requests CASCADE;
DROP TABLE IF EXISTS trip_stops CASCADE;
DROP TABLE IF EXISTS trips CASCADE;
DROP TABLE IF EXISTS vehicles CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- =========================================================
-- Tabla: users (cuenta + perfil del estudiante)
-- role: USER (estudiante) o ADMIN (administrador)
-- =========================================================
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(150) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    full_name   VARCHAR(150) NOT NULL,
    university  VARCHAR(150) NOT NULL,
    district    VARCHAR(100),
    phone       VARCHAR(30),
    avatar_url  VARCHAR(500),
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    role        VARCHAR(30)  NOT NULL DEFAULT 'USER',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================
-- Tabla: vehicles (1:1 con users, el dueño del auto)
-- =========================================================
CREATE TABLE vehicles (
    id        BIGSERIAL PRIMARY KEY,
    owner_id  BIGINT      NOT NULL UNIQUE,
    brand     VARCHAR(50) NOT NULL,
    model     VARCHAR(50) NOT NULL,
    color     VARCHAR(30),
    plate     VARCHAR(10) NOT NULL UNIQUE,
    capacity  INT         NOT NULL,
    CONSTRAINT fk_vehicles_owner
        FOREIGN KEY (owner_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_vehicle_capacity
        CHECK (capacity BETWEEN 1 AND 8)
);

-- =========================================================
-- Tabla: trips (N:1 con users vía driver_id, N:1 con vehicles)
-- status: SCHEDULED, FULL, COMPLETED, CANCELLED
-- =========================================================
CREATE TABLE trips (
    id               BIGSERIAL PRIMARY KEY,
    driver_id        BIGINT        NOT NULL,
    vehicle_id       BIGINT        NOT NULL,
    origin           VARCHAR(200)  NOT NULL,
    destination      VARCHAR(200)  NOT NULL,
    zone             VARCHAR(100)  NOT NULL,
    departure_time   TIMESTAMP     NOT NULL,
    total_seats      INT           NOT NULL,
    available_seats  INT           NOT NULL,
    price_per_seat   NUMERIC(8,2)  NOT NULL DEFAULT 0,
    status           VARCHAR(20)   NOT NULL DEFAULT 'SCHEDULED',
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trips_driver
        FOREIGN KEY (driver_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_trips_vehicle
        FOREIGN KEY (vehicle_id) REFERENCES vehicles (id),
    CONSTRAINT chk_trip_seats
        CHECK (total_seats > 0 AND available_seats BETWEEN 0 AND total_seats),
    CONSTRAINT chk_trip_price
        CHECK (price_per_seat >= 0)
);

-- =========================================================
-- Tabla: trip_stops (N:1 con trips) - puntos intermedios
-- =========================================================
CREATE TABLE trip_stops (
    id          BIGSERIAL PRIMARY KEY,
    trip_id     BIGINT       NOT NULL,
    address     VARCHAR(200) NOT NULL,
    zone        VARCHAR(100),
    stop_order  INT          NOT NULL,
    CONSTRAINT fk_stops_trip
        FOREIGN KEY (trip_id) REFERENCES trips (id)
        ON DELETE CASCADE,
    CONSTRAINT uq_stop_order
        UNIQUE (trip_id, stop_order)
);

-- =========================================================
-- Tabla: seat_requests (N:1 con trips, N:1 con users vía passenger_id)
-- pickup_stop_id NULL = recojo en el origen del conductor (US11)
-- status: PENDING, ACCEPTED, REJECTED, CANCELLED, COMPLETED
-- =========================================================
CREATE TABLE seat_requests (
    id              BIGSERIAL PRIMARY KEY,
    trip_id         BIGINT      NOT NULL,
    passenger_id    BIGINT      NOT NULL,
    pickup_stop_id  BIGINT,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    cancel_reason   VARCHAR(255),
    requested_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_requests_trip
        FOREIGN KEY (trip_id) REFERENCES trips (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_requests_passenger
        FOREIGN KEY (passenger_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_requests_stop
        FOREIGN KEY (pickup_stop_id) REFERENCES trip_stops (id)
        ON DELETE SET NULL,
    CONSTRAINT uq_request_trip_passenger
        UNIQUE (trip_id, passenger_id)
);

-- =========================================================
-- Tabla: ratings (N:1 con trips, rater_id y rated_id hacia users)
-- rated_role: DRIVER o PASSENGER
-- =========================================================
CREATE TABLE ratings (
    id          BIGSERIAL PRIMARY KEY,
    trip_id     BIGINT      NOT NULL,
    rater_id    BIGINT      NOT NULL,
    rated_id    BIGINT      NOT NULL,
    rated_role  VARCHAR(20) NOT NULL,
    score       INT         NOT NULL,
    comment     VARCHAR(500),
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ratings_trip
        FOREIGN KEY (trip_id) REFERENCES trips (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_ratings_rater
        FOREIGN KEY (rater_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_ratings_rated
        FOREIGN KEY (rated_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_rating_score
        CHECK (score BETWEEN 1 AND 5),
    CONSTRAINT chk_rating_not_self
        CHECK (rater_id <> rated_id),
    CONSTRAINT uq_rating_once
        UNIQUE (trip_id, rater_id, rated_id)
);

-- =========================================================
-- Tabla: reports (moderación - US19)
-- status: PENDING, RESOLVED, DISMISSED
-- =========================================================
CREATE TABLE reports (
    id                BIGSERIAL PRIMARY KEY,
    reporter_id       BIGINT       NOT NULL,
    reported_user_id  BIGINT,
    reported_trip_id  BIGINT,
    reason            VARCHAR(500) NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    action_taken      VARCHAR(255),
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at       TIMESTAMP,
    CONSTRAINT fk_reports_reporter
        FOREIGN KEY (reporter_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_reports_user
        FOREIGN KEY (reported_user_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_reports_trip
        FOREIGN KEY (reported_trip_id) REFERENCES trips (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_report_target
        CHECK (reported_user_id IS NOT NULL OR reported_trip_id IS NOT NULL)
);

-- =========================================================
-- Tabla: notifications (N:1 con users - US20)
-- =========================================================
CREATE TABLE notifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    type        VARCHAR(30)  NOT NULL,
    message     VARCHAR(255) NOT NULL,
    is_read     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
);

-- =========================================================
-- Índices útiles adicionales (búsquedas y reportes)
-- =========================================================
CREATE INDEX idx_trips_driver_id       ON trips (driver_id);
CREATE INDEX idx_trips_search          ON trips (origin, destination, departure_time);
CREATE INDEX idx_trips_zone            ON trips (zone);
CREATE INDEX idx_requests_trip_id      ON seat_requests (trip_id);
CREATE INDEX idx_requests_passenger_id ON seat_requests (passenger_id);
CREATE INDEX idx_ratings_rated_id      ON ratings (rated_id);
CREATE INDEX idx_notifications_user_id ON notifications (user_id);
