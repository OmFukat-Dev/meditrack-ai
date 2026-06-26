# MediTrack AI Database Architecture

## Overview
This database architecture provides a scalable, production-ready solution for the Hospital Management System with two separate databases to ensure data isolation and performance.

## Database Structure

### 1. User Management Database (`meditrack_user_management`)
Stores all system users, roles, and access control data.

**Tables:**
- `roles` - User roles and permissions
- `departments` - Hospital departments
- `users` - Base user information
- `admins` - Admin-specific data
- `doctors` - Doctor-specific data
- `nurses` - Nurse-specific data
- `audit_logs` - System audit trail
- `user_sessions` - Active user sessions

### 2. Patient Management Database (`meditrack_patient_management`)
Stores all patient-related data and medical records.

**Tables:**
- `patients` - Patient demographic and medical information
- `doctor_patient_assignment` - Doctor-patient relationships
- `nurse_patient_assignment` - Nurse-patient relationships
- `vital_signs` - Patient vital signs history
- `alerts` - Medical alerts and notifications
- `medical_records` - Patient medical history
- `medications` - Patient medications
- `appointments` - Patient appointments

## Cross-Database Relationships

### Strategy
Since patients and users are in separate databases, we maintain relationships through:
1. **Application-level validation** - Service layer checks
2. **Consistent IDs** - Same user IDs across databases
3. **Referential integrity** - API-level validation

### Key Relationships
- `patients.doctor_id` → `users.id` (User Management DB)
- `patients.primary_nurse_id` → `users.id` (User Management DB)
- `doctor_patient_assignment.doctor_id` → `users.id`
- `nurse_patient_assignment.nurse_id` → `users.id`
- `vital_signs.recorded_by` → `users.id`
- `alerts.assigned_to` → `users.id`

## Initial Data

### Users
**Admins:**
- Om (om@meditrackadmin.ai)
- Sakshi (sakshi@meditrackadmin.ai)

**Doctors:**
- Dipanshu (Interventional Cardiology)
- Tanmay (General Cardiology)
- Ayush (Neurology)
- Chetan (Pediatric Cardiology)
- Monir (Oncology)

**Nurses:**
- Sarah (Cardiology)
- Emily (Neurology)
- Jessica (Oncology)
- Monalisa (Cardiology)
- Lana (Emergency)

### Patients
20 patients with complete demographic information, assigned to doctors and nurses.

## Setup Instructions

### 1. Create Databases
```sql
-- Execute schema files in order:
mysql -u root -p < schema/user_management_schema.sql
mysql -u root -p < schema/patient_management_schema.sql
```

### 2. Seed Initial Data
```sql
-- Execute seed files:
mysql -u root -p meditrack_user_management < seed/user_management_seed.sql
mysql -u root -p meditrack_patient_management < seed/patient_management_seed.sql
```

### 3. Validate Setup
```sql
-- Run validation queries:
mysql -u root -p < queries/sample_queries.sql
```

## Features Implemented

### ✅ User Management
- Role-based access control (Admin, Doctor, Nurse, Viewer)
- Department assignment
- Password management
- User activation/deactivation
- Audit logging

### ✅ Patient Management
- Complete patient profiles with guardian information
- Doctor-patient assignment
- Nurse-patient assignment
- Room and bed management
- Insurance information

### ✅ Medical Data
- Vital signs tracking with automatic alert generation
- Medical records with confidentiality levels
- Medication management
- Appointment scheduling
- Alert system with priority levels

### ✅ Cross-Database Integrity
- Consistent ID management
- Application-level validation
- Comprehensive audit trail
- Data consistency checks

## Security Features

### Authentication
- Password hashing (bcrypt)
- Session management
- Role-based permissions
- IP and user agent tracking

### Data Protection
- Audit logging for all changes
- Confidential medical records
- Data encryption in transit
- Access control by role

## Performance Optimizations

### Indexing
- Primary keys on all tables
- Foreign key indexes
- Search indexes on frequently queried fields
- Composite indexes for complex queries

### Query Optimization
- Efficient joins for cross-database queries
- Pagination for large datasets
- Caching strategies for frequently accessed data

## Monitoring & Maintenance

### Automated Alerts
- Vital sign threshold monitoring
- Database consistency checks
- Performance metrics
- Security event logging

### Data Validation
- Cross-database relationship validation
- Data integrity checks
- Duplicate detection
- Orphaned record cleanup

## Integration Points

### Kafka Integration
- Alert publishing to Kafka topics
- Event-driven architecture support
- Microservices communication
- Real-time updates

### AI Model Integration
- Vital sign analysis
- Prediction accuracy tracking
- Model performance metrics
- Automated alert generation

## Backup & Recovery

### Backup Strategy
- Daily automated backups
- Point-in-time recovery
- Cross-database consistency
- Disaster recovery planning

### Data Export
- Patient data export (CSV/PDF)
- Medical record exports
- Audit log exports
- Statistical reports

## Compliance

### HIPAA Compliance
- Protected health information (PHI) handling
- Access logging and audit trails
- Data encryption standards
- Patient privacy controls

### Data Retention
- Configurable retention policies
- Automated data archiving
- Secure data disposal
- Compliance reporting

## Scalability Considerations

### Horizontal Scaling
- Database sharding support
- Read replica configuration
- Load balancing ready
- Microservices architecture

### Future Enhancements
- Multi-tenant support
- Advanced analytics
- Machine learning integration
- Mobile API support

## Troubleshooting

### Common Issues
1. **Cross-database connections** - Use application-level joins
2. **ID consistency** - Ensure same IDs across databases
3. **Performance** - Check indexes and query optimization
4. **Data integrity** - Run validation queries regularly

### Monitoring Queries
```sql
-- Check for orphaned records
SELECT * FROM queries/sample_queries.sql WHERE query_type = 'VALIDATION';

-- Monitor performance
SHOW PROCESSLIST;
SHOW ENGINE INNODB STATUS;
```

## Support

For database issues:
1. Check validation queries
2. Review audit logs
3. Monitor performance metrics
4. Verify cross-database relationships
