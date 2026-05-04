# B2B Platform Module

Multi-tenant organization management for B2B clients (health management companies, rehabilitation hospitals, fitness equipment vendors).

## Overview

The B2B platform extends the intervention-engine FastAPI service (port 4001) to provide multi-tenant organization management capabilities.

## Features

### 1. Multi-Tenant Architecture
- Tenant registration and management
- Data isolation per tenant
- JWT-based authentication

### 2. Role-Based Access Control
| Role | Scope | Permissions |
|------|-------|-------------|
| org_admin | Full tenant | All features + user management |
| health_coach | Assigned users | View user data + manage prescriptions |
| viewer | Read-only | View dashboard overview |

### 3. Dashboard KPIs
- Active users count
- Device online rate
- Exercise session count
- Sleep compliance rate

### 4. Device Management
- Device listing with status
- Last seen tracking
- Device data retrieval

### 5. Health Coach Workstation
- User list management
- Health profile viewing
- Prescription management

## API Endpoints

### Authentication
```
POST /api/auth/login          - Login with username/password
POST /api/auth/refresh        - Refresh access token
```

### Tenant Management
```
POST   /api/tenants           - Create tenant
GET    /api/tenants           - List tenants
GET    /api/tenants/{id}      - Get tenant details
PUT    /api/tenants/{id}      - Update tenant
DELETE /api/tenants/{id}      - Disable tenant
```

### User Management
```
POST   /api/users             - Create B2B user
GET    /api/users             - List users
GET    /api/users/{id}        - Get user details
PUT    /api/users/{id}        - Update user
DELETE /api/users/{id}        - Delete user
```

### Dashboard
```
GET /api/dashboard/kpi        - Current KPI snapshot
GET /api/dashboard/trends      - KPI trends (7d/30d)
```

### Devices
```
GET /api/devices              - List devices
GET /api/devices/{id}         - Get device details
GET /api/devices/{id}/data    - Get device data
```

### Health Coach
```
GET  /api/coach/users                    - List managed users
GET  /api/coach/users/{id}/profile       - Get user profile
GET  /api/coach/users/{id}/prescriptions - Get prescriptions
PUT  /api/coach/prescriptions/{id}      - Update prescription
```

## Demo Credentials

On startup, the following demo users are created:
- Username: `admin`, Password: `admin123` (org_admin)
- Username: `coach1`, Password: `coach123` (health_coach)
- Username: `viewer`, Password: `viewer123` (viewer)

## Authentication Flow

1. Login with username/password to get JWT token
2. Include token in Authorization header: `Bearer <token>`
3. Token contains `tenant_id` and `role` for multi-tenant isolation

## Running

The B2B module is automatically loaded when starting the intervention-engine:
```bash
python -m uvicorn src.api.fastapi_server:app --host 0.0.0.0 --port 4001
```

## Testing

```bash
python -m pytest tests/b2b/ -v
```
