# MediTrack AI - Health Monitoring & Predictive Alerts

## 🏥 Project Overview
MediTrack AI is a comprehensive healthcare monitoring system that provides real-time vital monitoring and predictive health alerts using microservices architecture and AI/ML models.

## 🚀 Key Features
- **Real-time Vital Monitoring** - Continuous tracking of patient vitals via IoT streams
- **AI-Powered Predictions** - Machine learning models for early health risk detection
- **NEWS Scoring** - Clinically validated National Early Warning Score system
- **FHIR Compliance** - Healthcare data interoperability standards
- **Alert Escalation** - Multi-level notification system with Saga pattern
- **Explainability** - Feature importance for AI predictions
- **Performance Dashboard** - Model drift detection and retraining pipeline

## 🏗️ Architecture
- **Microservices** - Spring Boot 3.2 with Java 17/21
- **API Gateway** - Spring Cloud Gateway
- **Service Discovery** - Eureka Server
- **Message Streaming** - Apache Kafka
- **Databases** - MySQL (primary) + Redis (caching)
- **AI/ML** - Weka 3.8 with J48 Decision Trees
- **Frontend** - React 18 with Material-UI
- **Monitoring** - Prometheus + Grafana + Loki + Zipkin

## 📁 Project Structure
```
meditrack-ai/
├── backend/                 # Spring Boot microservices
│   ├── eureka-server/       # Service discovery
│   ├── api-gateway/         # API Gateway
│   ├── patient-service/     # Patient management
│   ├── vitals-service/      # Vitals ingestion
│   ├── ai-prediction/       # AI/ML predictions
│   ├── alert-service/       # Alert management
│   ├── notification-service/ # Notifications
│   └── vital-simulator/     # Test data generator
├── frontend/                # React dashboard
├── docker/                  # Docker configurations
├── docs/                    # Documentation
├── scripts/                 # Utility scripts
└── docker-compose.yml       # Local development setup
```

## 🛠️ Tech Stack (100% Free & Open Source)

### Backend
- **Java 17/21** - OpenJDK
- **Spring Boot 3.2** - Application framework
- **Spring Cloud** - Microservices infrastructure
- **Apache Kafka** - Event streaming
- **MySQL 15** - Primary database
- **Redis 7** - Caching layer
- **Weka 3.8** - Machine learning library
- **HAPI FHIR** - Healthcare data standards

### Frontend
- **React 18** - UI framework
- **Redux Toolkit** - State management
- **Material-UI** - Component library
- **Recharts** - Data visualization
- **Native WebSocket** - Real-time communication

### DevOps & Monitoring
- **Docker** - Containerization
- **Prometheus** - Metrics collection
- **Grafana** - Visualization
- **Loki** - Log aggregation
- **Promtail** - Log shipping
- **Zipkin** - Distributed tracing
- **Maven** - Build tool
- **JUnit 5** - Testing framework

## 🚀 Quick Start

### Prerequisites
- Java 17 or 21
- Maven 3.9+
- Docker Desktop
- Node.js 18+
- MySQL

### Local Development
```bash
# Clone the repository
git clone <your-repo-url>
cd meditrack-ai

# Start infrastructure services
docker-compose up -d

# This boots MySQL, Redis, Kafka, Prometheus, Grafana, Loki, Promtail, and Zipkin.
# The MySQL init script creates the service databases, and Kafka topics are bootstrapped automatically.

# Build all microservices
mvn clean install

# Start services (order matters)
# 1. Eureka Server
# 2. API Gateway
# 3. Other microservices

# Start frontend
cd frontend
npm install
npm run dev
```

Windows launchers:
- `run-project.bat` opens Docker, backend, and frontend in separate windows.
- `run-docker.bat` opens only the Docker infrastructure stack.
- `run-backend.bat` starts the backend services; add `--skip-docker` when Docker is already being launched separately.

Optional production build:
```bash
npm run build
```

### Frontend Access
- The dashboard opens to a role-aware sign-in screen.
- Demo access:
  - `alex@admin.meditrack.ai` / `Admin@123`
  - `dr.isha@clinician.meditrack.ai` / `Clinician@123`
  - `family.member@viewer.meditrack.ai` / `Viewer@123`
- The supported email pattern is `name@admin.meditrack.ai`, `name@clinician.meditrack.ai`, or `name@viewer.meditrack.ai`.
- The sign-in card also includes a create-account tab for local clinician and viewer accounts.
- Optional frontend env vars:
  - `VITE_AUTH_API_URL` for a backend login service
  - `VITE_DASHBOARD_WS_URL` for a websocket endpoint

## 📊 Implementation Phases

1. **Phase 1** - Foundation Setup (Week 1-2)
2. **Phase 2** - Core Microservices (Week 3-5)
3. **Phase 3** - AI/ML Prediction Engine (Week 6-8)
4. **Phase 4** - Alert & Notification System (Week 9-10)
5. **Phase 5** - Frontend Dashboard (Week 11-12)
6. **Phase 6** - Testing & QA (Week 13-14)
7. **Phase 7** - Monitoring & Observability (Week 15)

## 🧪 Testing
- **Unit Tests** - JUnit 5 + Mockito
- **Integration Tests** - Testcontainers
- **API Tests** - REST Assured
- **Coverage Target** - 80%+ (JaCoCo)

## Performance Profiling
Use the Phase 6.2 profiling script after the backend services are running:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\profile-phase6-2.ps1
```

The script captures:
- JFR CPU profiles
- Heap snapshots and class histograms
- Native memory snapshots when available
- A latency summary for the patient and vitals read paths

Outputs are written to `profiling/phase-6-2/<timestamp>/`.

## 📈 Monitoring
- **Health Checks** - Spring Boot Actuator with custom service-specific readiness checks
- **Metrics** - Prometheus scrapes the backend `/actuator/prometheus` endpoints and Grafana reads the same datasource
- **Dashboards** - `docker/grafana/dashboards/meditrack-overview.json`
- **Alerts** - Prometheus alert rules in `docker/prometheus/rules/meditrack-alerts.yml`
- **Tracing** - Zipkin with trace IDs propagated through the services
- **Logging** - Structured trace-aware logs written to `logs/<service>.log` and shipped to Loki
- **Log Explorer** - Grafana Loki datasource with trace-to-log links

## 🔒 Security
- **Authentication** - JWT tokens
- **Authorization** - Role-based access control (RBAC)
- **Data Encryption** - HTTPS + database encryption
- **HIPAA Compliance** - Healthcare data protection

## 📝 Documentation
- [Implementation Plan](./MEDITRACK_IMPLEMENTATION_PLAN.md)
- [API Documentation](./docs/api/)
- [Architecture Guide](./docs/architecture/)
- [Deployment Guide](./docs/deployment/)

## 🤝 Contributing
1. Fork the repository
2. Create feature branch
3. Commit your changes
4. Push to branch
5. Create Pull Request

## 📄 License
This project is licensed under the MIT License - see the LICENSE file for details.

## 🏆 Success Metrics
- **Real-time Processing** - <100ms vital ingestion
- **Prediction Accuracy** - >90% on test dataset
- **System Availability** - 99.9% uptime
- **Alert Response Time** - <5 seconds
- **Test Coverage** - 80%+ across all services

---

**Note**: This project uses only free and open-source technologies. No paid services or trial periods are required.
