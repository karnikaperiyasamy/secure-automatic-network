# SANTMS — Secure Automatic Network Topology & Management System
### Enterprise-Grade Network Management | Spring Boot 3 + PostgreSQL + Vanilla JS

---

## 🚀 Quick Start (5 Steps)

### Prerequisites
| Tool | Version | Download |
|------|---------|----------|
| Java JDK | 17+ | https://adoptium.net |
| Maven | 3.9+ | https://maven.apache.org (or use `./mvnw`) |
| PostgreSQL | 14+ | https://www.postgresql.org |

---

### Step 1 — Create the Database

Open pgAdmin or psql and run:
```sql
CREATE DATABASE santms_db;
```

---

### Step 2 — Configure Database Credentials

Edit `backend/src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/santms_db
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD
```

> ⚠️ Change `YOUR_PASSWORD` to your PostgreSQL password.

---

### Step 3 — Run the Backend

```bash
cd backend
./mvnw spring-boot:run
```

**Windows:**
```cmd
cd backend
mvnw.cmd spring-boot:run
```

The backend starts on **http://localhost:8080**

You'll see:
```
╔══════════════════════════════════════════════════════════╗
║   SANTMS - Network Management System                    ║
║   Version 1.0.0  |  Running on http://localhost:8080    ║
╚══════════════════════════════════════════════════════════╝
```

Spring Boot will:
- ✅ Create all database tables automatically (`ddl-auto=create-drop`)
- ✅ Seed default users, organization, and sample devices
- ✅ Start background network simulation

---

### Step 4 — Open the Frontend

Open `frontend/login.html` in your browser, **or** serve it with any static server:

```bash
# Option A: Python (simplest)
cd frontend
python3 -m http.server 3000
# Then open http://localhost:3000/login.html

# Option B: Node.js live-server
npx live-server frontend --port=3000

# Option C: VS Code Live Server extension
# Right-click login.html → Open with Live Server
```

---

### Step 5 — Login

| Role | Username | Password | Access |
|------|----------|----------|--------|
| 🔴 Super Admin | `superadmin` | `Admin@1234` | Full system control |
| 🟡 Network Admin | `netadmin` | `Admin@1234` | Device & topology management |
| 🔵 Security Analyst | `secanalyst` | `Admin@1234` | Security monitoring |
| ⚪ Read Only | `readonly` | `Admin@1234` | View dashboards only |

---

## 📁 Project Structure

```
santms/
├── backend/                        ← Spring Boot application
│   ├── mvnw                        ← Maven wrapper (Linux/Mac)
│   ├── mvnw.cmd                    ← Maven wrapper (Windows)
│   ├── pom.xml                     ← Dependencies
│   └── src/main/
│       ├── java/com/santms/
│       │   ├── SantmsApplication.java
│       │   ├── config/
│       │   │   ├── AppConfig.java
│       │   │   ├── DataSeeder.java      ← Seeds default data
│       │   │   └── SecurityConfig.java  ← JWT + CORS + RBAC
│       │   ├── controller/
│       │   │   ├── AuthController.java
│       │   │   ├── DeviceController.java
│       │   │   ├── ApiControllers.java  ← Dashboard, Topology, Alerts, Scans
│       │   │   └── UserAndAuditControllers.java
│       │   ├── dto/
│       │   │   ├── request/             ← Input DTOs with validation
│       │   │   └── response/            ← Output DTOs
│       │   ├── entity/                  ← JPA entities
│       │   ├── exception/               ← Global exception handling
│       │   ├── repository/              ← Spring Data JPA repositories
│       │   ├── security/                ← JWT filter + UserDetailsService
│       │   └── service/impl/            ← Business logic
│       └── resources/
│           └── application.properties
│
├── frontend/                       ← Static HTML/CSS/JS
│   ├── login.html                  ← Entry point
│   ├── css/
│   │   └── main.css                ← Global dark theme styles
│   ├── js/
│   │   └── app.js                  ← API client + shared utilities
│   └── pages/
│       ├── dashboard.html          ← Main dashboard with charts
│       ├── topology.html           ← Interactive network map (vis.js)
│       ├── devices.html            ← Device CRUD + search
│       ├── alerts.html             ← Alert management
│       ├── monitoring.html         ← Live device metrics
│       ├── security.html           ← Threat detection + AI recs
│       ├── scans.html              ← Network discovery scans
│       ├── ipmanager.html          ← IP address management
│       ├── reports.html            ← Reports + analytics
│       ├── users.html              ← User management
│       ├── auditlogs.html          ← Audit trail
│       └── settings.html           ← System settings
│
└── schema.sql                      ← Manual SQL schema (optional)
```

---

## 🔌 REST API Reference

All APIs require `Authorization: Bearer <JWT_TOKEN>` header (except auth endpoints).

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | Login, returns JWT |
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/refresh` | Refresh access token |
| POST | `/api/auth/forgot-password` | Send reset email |
| POST | `/api/auth/reset-password` | Reset with token |
| POST | `/api/auth/logout` | Invalidate token |
| GET  | `/api/auth/validate` | Validate token |

### Devices
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/devices?orgId=1` | List all devices (paginated) |
| GET | `/api/devices/search?orgId=1&q=cisco` | Search devices |
| GET | `/api/devices/{id}` | Get device by ID |
| POST | `/api/devices?orgId=1` | Create device |
| PUT | `/api/devices/{id}` | Update device |
| DELETE | `/api/devices/{id}` | Delete device |
| PATCH | `/api/devices/{id}/maintenance` | Toggle maintenance |
| GET | `/api/devices/stats?orgId=1` | Device statistics |

### Dashboard
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/dashboard?orgId=1` | Full dashboard data |

### Topology
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/topology?orgId=1` | Get topology graph |
| POST | `/api/topology/regenerate?orgId=1` | Regenerate topology |
| PATCH | `/api/topology/nodes/{id}/position?x=100&y=200` | Update node position |

### Alerts
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/alerts?orgId=1` | List alerts (paginated) |
| GET | `/api/alerts/open?orgId=1` | Open alerts only |
| GET | `/api/alerts/count?orgId=1` | Alert counts |
| PATCH | `/api/alerts/{id}/acknowledge` | Acknowledge alert |
| PATCH | `/api/alerts/{id}/resolve?note=fixed` | Resolve alert |

### Network Scans
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/network/scans/start?orgId=1&network=192.168.1.0/24` | Start scan |
| GET | `/api/network/scans/{id}/status` | Poll scan progress |
| GET | `/api/network/scans?orgId=1` | Scan history |

### Users
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users?orgId=1` | List users |
| GET | `/api/users/me` | Current user profile |
| POST | `/api/users?orgId=1` | Create user |
| DELETE | `/api/users/{id}` | Delete user |

### Audit Logs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/audit-logs?orgId=1` | Paginated audit log |

---

## 🔒 Security Features

- **JWT Authentication** — Stateless, signed HS256 tokens
- **BCrypt Password Hashing** — Strength 12
- **Role-Based Access Control** — 4 roles, method-level security
- **Account Lockout** — After 5 failed logins (30 min lock)
- **CORS** — Configurable allowed origins
- **Input Validation** — Jakarta Bean Validation on all DTOs
- **SQL Injection Protection** — JPA parameterized queries
- **Global Exception Handler** — Consistent error responses

---

## ⚙️ Configuration

### Switch to `validate` (production-safe) schema mode
```properties
# Don't drop/recreate tables on restart
spring.jpa.hibernate.ddl-auto=validate
```

### Email Setup (for alerts/OTP)
```properties
spring.mail.username=your@gmail.com
spring.mail.password=your-app-password   # Gmail App Password
```

### Change default JWT secret
```properties
app.jwt.secret=YourVeryLongSecretKey256BitsMinimumForHMACSHA256Algorithm
```

---

## 🧪 Testing the API with curl

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"superadmin","password":"Admin@1234"}'

# Use the returned token for subsequent requests
TOKEN="eyJ..."

# Get dashboard
curl http://localhost:8080/api/dashboard?orgId=1 \
  -H "Authorization: Bearer $TOKEN"

# List devices
curl http://localhost:8080/api/devices?orgId=1 \
  -H "Authorization: Bearer $TOKEN"

# Start a network scan
curl -X POST "http://localhost:8080/api/network/scans/start?orgId=1&network=192.168.1.0/24" \
  -H "Authorization: Bearer $TOKEN"
```

---

## 🐛 Troubleshooting

### Port 8080 already in use
```properties
# application.properties
server.port=8090
```
Also update `API_BASE` in `frontend/js/app.js`:
```js
const API_BASE = 'http://localhost:8090';
```

### Database connection refused
- Ensure PostgreSQL is running: `sudo service postgresql start`
- Verify credentials in `application.properties`
- Check database exists: `psql -U postgres -c "\l"`

### Maven not found / `./mvnw` fails
```bash
# Install Maven directly
# Ubuntu/Debian:
sudo apt install maven

# macOS:
brew install maven

# Then run:
mvn spring-boot:run
```

### CORS errors in browser
The backend allows all origins by default. If issues persist, check:
```properties
app.cors.allowed-origins=http://localhost:3000,http://localhost:8080
```

### Frontend shows "Connection failed"
- Ensure backend is running on port 8080
- Open browser console (F12) for specific errors
- Try: `curl http://localhost:8080/api/auth/validate`

---

## 📊 Technology Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 17, Spring Boot 3.2, Spring Security, Spring Data JPA |
| **Database** | PostgreSQL 14+, Hibernate ORM, HikariCP connection pool |
| **Security** | JWT (jjwt 0.11.5), BCrypt, RBAC, CSRF disabled for REST |
| **Frontend** | HTML5, CSS3, Vanilla JavaScript ES6+ |
| **UI Libraries** | Bootstrap 5.3, Font Awesome 6.5 |
| **Charts** | Chart.js 4.4 |
| **Network Graph** | vis.js 9.1 (Network) |
| **Build** | Maven 3.9 with Maven Wrapper |

---

## 🌟 Features Summary

| Module | Features |
|--------|---------|
| **Auth** | JWT login, refresh tokens, forgot password, account lockout, RBAC |
| **Dashboard** | 8 stat cards, 4 charts, real-time metrics, activity feed |
| **Topology** | vis.js graph, drag/drop, 3 layouts, PNG export, auto-refresh |
| **Devices** | Full CRUD, search, filter, CSV import/export, maintenance mode |
| **Alerts** | Severity levels, ack/resolve, bulk operations, auto-generation |
| **Monitoring** | Live metrics grid, 10s auto-refresh, per-device sparklines |
| **Security** | Threat detection, AI recommendations, risk scoring, checklist |
| **Scans** | Async network discovery, progress polling, device auto-creation |
| **IP Manager** | Visual subnet map, pool management, conflict detection |
| **Reports** | 6 report types, chart analytics, CSV/PDF/Excel export |
| **Users** | Full CRUD, role assignment, profile management |
| **Audit Logs** | Complete action trail, filtering, CSV export |
| **Settings** | SMTP, security policy, scan schedule, backup, API config |

---

*Built with ❤️ as an enterprise network management solution.*
