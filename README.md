# CQURShift
Guard Scheduling and Management System — CIS096-1 Assessment 2
README
# C-QURShift — Guard Scheduling and Management System

CIS096-1 Principles of Programming and Data Structures — Assessment 2 (2025–2026)

## Overview
C-QURShift is a desktop application for security guard scheduling built with
Java 21, JavaFX 21, JDBC, and MySQL 8.0.

## Features
- User authentication with SHA-256 password hashing and role-based access (Admin / Manager)
- Full CRUD management of Guards, Sites, and Shifts
- Automatic nearest-guard assignment using the Haversine algorithm
- Offline HTML5 Canvas London map with guard deployment visualisation
- In-app toast notifications and simulated SMS alerts (Twilio-ready)
- PDF and CSV data export
- 4-tab Simulation Centre (Admin only)

## Tech Stack
| Component | Technology |
|-----------|------------|
| Language | Java 21 LTS |
| GUI   | JavaFX 21 |
| Database | MySQL 8.0 |
| Build  | Apache Maven 3.9 |
| Testing | JUnit 5.10 + Mockito 5.10 |

## Prerequisites
- Java 21+
- Maven 3.9+
- MySQL 8.0 running locally

## Database Setup
sql
CREATE DATABASE IF NOT EXISTS secureshift_db;
CREATE USER IF NOT EXISTS 'secureshift_user'@'localhost' IDENTIFIED BY 'SecureShift2025!';
GRANT ALL PRIVILEGES ON secureshift_db.* TO 'secureshift_user'@'localhost';

## Run
bash
mvn clean javafx:run


## Test
bash
mvn clean test


## Default Credentials
| Username | Password  | Role  |
|----------|-------------|---------|
| admin  | admin123  | ADMIN |
| manager | manager123 | MANAGER |

## Project Structure
src/main/java/secureshift/
├── domain/     # Guard, Site, Shift, AssignmentStrategy, NearestGuardStrategy
├── data/      # JDBC repositories, DatabaseConfig, UserRepository
├── service/    # GuardService, ShiftService, SmsService, ExportService
├── application/  # Scheduler, SimulationEngine, RouteService
├── presentation/  # JavaFX controllers (LoginController, MainController, etc.)
└── util/      # GeoUtils, TimeUtils
