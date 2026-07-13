# ZyroTrade

> A secure stock trading simulation backend built with Spring Boot, featuring JWT authentication, real-time market data integration, and transactional order execution.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-brightgreen)
![Spring Security](https://img.shields.io/badge/Spring_Security-JWT-success)
![MySQL](https://img.shields.io/badge/MySQL-Database-blue)
![License](https://img.shields.io/badge/License-MIT-green)

---

# Overview

ZyroTrade is a backend application that simulates stock trading by allowing users to buy and sell stocks using live market prices fetched from the Alpha Vantage API.

The platform focuses on secure order execution, portfolio management, and transactional consistency while demonstrating real-world backend development practices using Spring Boot.

---

# Features

## Secure Authentication

- JWT-based authentication
- Role-based access control
- Protected trading endpoints
- Secure password encryption

---

## Trading Engine

Supports

- Buy Orders
- Sell Orders
- Portfolio Updates
- Balance Validation
- Transaction History

The order execution engine ensures transactional consistency throughout every trade.

---

## Live Market Data

Integrated with the **Alpha Vantage API** to fetch live stock prices.

Provides

- Current stock prices
- Automated market data synchronization
- Scheduled background updates

---

## Portfolio Management

Track

- Current Holdings
- Quantity Owned
- Average Purchase Price
- Portfolio Value
- Available Balance

---

## Transaction Management

Maintains a complete record of

- Buy Transactions
- Sell Transactions
- Trade History
- Portfolio Changes

All operations are executed using ACID-compliant database transactions.

---

# System Architecture

```
              Client
                 │
                 ▼
          REST Controllers
                 │
                 ▼
        Authentication Layer
         (Spring Security)
                 │
                 ▼
          Business Services
                 │
      ┌──────────┴──────────┐
      ▼                     ▼
 Trading Engine      Market Data Service
      │                     │
      ▼                     ▼
 Portfolio Manager   Alpha Vantage API
      │
      ▼
 Spring Data JPA
      │
      ▼
     MySQL
```

---

# Tech Stack

### Backend

- Java
- Spring Boot
- Spring Security
- Spring Data JPA

### Database

- MySQL

### Authentication

- JWT Authentication

### External APIs

- Alpha Vantage API

### Build Tool

- Maven

---

# Core Modules

## Authentication

Responsible for

- User Registration
- Login
- JWT Generation
- Request Authorization

---

## Trading Engine

Handles

- BUY Orders
- SELL Orders
- Portfolio Updates
- Trade Validation

---

## Portfolio Service

Responsible for

- Holdings Management
- Profit/Loss Tracking
- Portfolio Valuation

---

## Market Data Service

Fetches stock prices from Alpha Vantage using scheduled jobs to keep market data up to date.

---

## Transaction Service

Maintains complete trading history while ensuring transactional consistency.

---

# Database Design

Main entities

- User
- Portfolio
- Holding
- Transaction
- Stock

The schema is normalized to maintain data integrity and support efficient portfolio operations.

---

# Execution Flow

```
Login
   │
   ▼
Authenticate User
   │
   ▼
Fetch Live Stock Price
   │
   ▼
Validate Funds/Holdings
   │
   ▼
Execute Trade
   │
   ▼
Update Portfolio
   │
   ▼
Store Transaction
   │
   ▼
Return Updated Portfolio
```

---

# REST APIs

## Authentication

```
POST /api/auth/register

POST /api/auth/login
```

---

## Trading

```
POST /api/trades/buy

POST /api/trades/sell
```

---

## Portfolio

```
GET /api/portfolio

GET /api/portfolio/holdings
```

---

## Transactions

```
GET /api/transactions
```

---

## Market Data

```
GET /api/stocks/{symbol}
```

---

# Performance Highlights

- Processes **500+ simulated trades**
- Average trade execution latency below **200 ms**
- JWT-secured REST APIs
- Live market data integration
- ACID-compliant transaction management

---

# Future Enhancements

- Real-time WebSocket price streaming
- Limit & Stop-Loss Orders
- Watchlists
- Stock Search
- Multi-currency Support
- Redis Caching
- Docker Deployment
- Kubernetes Support
- Portfolio Analytics Dashboard

---

# Getting Started

Clone the repository

```bash
git clone https://github.com/AryanGupta833/ZyroTrade.git
```

Navigate to the project

```bash
cd ZyroTrade
```

Run the application

```bash
mvn spring-boot:run
```

---

# Future Vision

ZyroTrade aims to evolve into a feature-rich stock trading backend that demonstrates scalable backend architecture, secure financial transaction processing, and seamless integration with external financial APIs.

---

# Author

**Aryan Gupta**

GitHub  
https://github.com/AryanGupta833

LinkedIn  
https://linkedin.com/in/aryan-gupta-316a35366
