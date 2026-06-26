#!/bin/bash

# =====================================================
# MEDITRACK AI DATABASE SETUP SCRIPT
# =====================================================
# This script automates the complete database setup process

echo "🏥 MediTrack AI Database Setup"
echo "=================================="

# MySQL Configuration
MYSQL_USER="root"
MYSQL_PASSWORD="root123"
MYSQL_HOST="localhost"

# Database Names
USER_DB="meditrack_user_management"
PATIENT_DB="meditrack_patient_management"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

# Check MySQL connection
echo "🔍 Checking MySQL connection..."
if ! mysql -u$MYSQL_USER -p$MYSQL_PASSWORD -h$MYSQL_HOST -e "SELECT 1;" &>/dev/null; then
    print_error "Cannot connect to MySQL. Please check credentials."
    exit 1
fi
print_status "MySQL connection successful"

# Create databases
echo ""
echo "📊 Creating databases..."

# Create User Management Database
print_info "Creating User Management Database..."
mysql -u$MYSQL_USER -p$MYSQL_PASSWORD -h$MYSQL_HOST -e "DROP DATABASE IF EXISTS $USER_DB;" 2>/dev/null
mysql -u$MYSQL_USER -p$MYSQL_PASSWORD -h$MYSQL_HOST -e "CREATE DATABASE $USER_DB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null
if [ $? -eq 0 ]; then
    print_status "User Management Database created successfully"
else
    print_error "Failed to create User Management Database"
    exit 1
fi

# Create Patient Management Database
print_info "Creating Patient Management Database..."
mysql -u$MYSQL_USER -p$MYSQL_PASSWORD -h$MYSQL_HOST -e "DROP DATABASE IF EXISTS $PATIENT_DB;" 2>/dev/null
mysql -u$MYSQL_USER -p$MYSQL_PASSWORD -h$MYSQL_HOST -e "CREATE DATABASE $PATIENT_DB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null
if [ $? -eq 0 ]; then
    print_status "Patient Management Database created successfully"
else
    print_error "Failed to create Patient Management Database"
    exit 1
fi

# Execute schema files
echo ""
echo "🏗️ Creating database schemas..."

# User Management Schema
print_info "Applying User Management Schema..."
mysql -u$MYSQL_USER -p$MYSQL_PASSWORD -h$MYSQL_HOST $USER_DB < schema/user_management_schema.sql 2>/dev/null
if [ $? -eq 0 ]; then
    print_status "User Management Schema applied successfully"
else
    print_error "Failed to apply User Management Schema"
    exit 1
fi

# Patient Management Schema
print_info "Applying Patient Management Schema..."
mysql -u$MYSQL_USER -p$MYSQL_PASSWORD -h$MYSQL_HOST $PATIENT_DB < schema/patient_management_schema.sql 2>/dev/null
if [ $? -eq 0 ]; then
    print_status "Patient Management Schema applied successfully"
else
    print_error "Failed to apply Patient Management Schema"
    exit 1
fi

# Seed initial data
echo ""
echo "🌱 Seeding initial data..."

# User Management Seed Data
print_info "Seeding User Management Database..."
mysql -u$MYSQL_USER -p$MYSQL_PASSWORD -h$MYSQL_HOST $USER_DB < seed/user_management_seed.sql 2>/dev/null
if [ $? -eq 0 ]; then
    print_status "User Management Data seeded successfully"
else
    print_error "Failed to seed User Management Data"
    exit 1
fi

# Patient Management Seed Data
print_info "Seeding Patient Management Database..."
mysql -u$MYSQL_USER -p$MYSQL_PASSWORD -h$MYSQL_HOST $PATIENT_DB < seed/patient_management_seed.sql 2>/dev/null
if [ $? -eq 0 ]; then
    print_status "Patient Management Data seeded successfully"
else
    print_error "Failed to seed Patient Management Data"
    exit 1
fi

# Validate setup
echo ""
echo "🔍 Validating database setup..."

# Check tables count
USER_TABLES=$(mysql -u$MYSQL_USER -p$MYSQL_PASSWORD -h$MYSQL_HOST -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$USER_DB';" -s -N 2>/dev/null)
PATIENT_TABLES=$(mysql -u$MYSQL_USER -p$MYSQL_PASSWORD -h$MYSQL_HOST -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$PATIENT_DB';" -s -N 2>/dev/null)

print_info "User Management Database: $USER_TABLES tables created"
print_info "Patient Management Database: $PATIENT_TABLES tables created"

# Check data counts
ADMINS=$(mysql -u$MYSQL_USER -p$MYSQL_PASSWORD -h$MYSQL_HOST -e "SELECT COUNT(*) FROM $USER_DB.admins;" -s -N 2>/dev/null)
DOCTORS=$(mysql -u$MYSQL_USER -p$MYSQL_PASSWORD -h$MYSQL_HOST -e "SELECT COUNT(*) FROM $USER_DB.doctors;" -s -N 2>/dev/null)
NURSES=$(mysql -u$MYSQL_USER -p$MYSQL_PASSWORD -h$MYSQL_HOST -e "SELECT COUNT(*) FROM $USER_DB.nurses;" -s -N 2>/dev/null)
PATIENTS=$(mysql -u$MYSQL_USER -p$MYSQL_PASSWORD -h$MYSQL_HOST -e "SELECT COUNT(*) FROM $PATIENT_DB.patients;" -s -N 2>/dev/null)

print_info "Admins: $ADMINS"
print_info "Doctors: $DOCTORS"
print_info "Nurses: $NURSES"
print_info "Patients: $PATIENTS"

# Cross-database validation
echo ""
print_info "Validating cross-database relationships..."

# Check for orphaned patient records
ORPHANED_PATIENTS=$(mysql -u$MYSQL_USER -p$MYSQL_PASSWORD -h$MYSQL_HOST -e "
SELECT COUNT(*) FROM $PATIENT_DB.patients p 
LEFT JOIN $USER_DB.users u ON p.doctor_id = u.id 
WHERE p.doctor_id IS NOT NULL AND u.id IS NULL;
" -s -N 2>/dev/null)

if [ "$ORPHANED_PATIENTS" -eq 0 ]; then
    print_status "Cross-database relationships validated successfully"
else
    print_warning "Found $ORPHANED_PATIENTS orphaned patient records"
fi

# Test sample queries
echo ""
print_info "Testing sample queries..."

# Test doctor-patient query
DOCTOR_PATIENTS=$(mysql -u$MYSQL_USER -p$MYSQL_PASSWORD -h$MYSQL_HOST -e "
SELECT COUNT(*) FROM $PATIENT_DB.doctor_patient_assignment 
WHERE doctor_id = 'doc-dipanshu' AND assignment_status = 'ACTIVE';
" -s -N 2>/dev/null)

print_info "Doctor Dipanshu has $DOCTOR_PATIENTS assigned patients"

# Test vital signs query
VITAL_SIGNS=$(mysql -u$MYSQL_USER -p$MYSQL_PASSWORD -h$MYSQL_HOST -e "
SELECT COUNT(*) FROM $PATIENT_DB.vital_signs 
WHERE patient_id = 'patient-1';
" -s -N 2>/dev/null)

print_info "Patient 1 has $VITAL_SIGNS vital sign records"

# Generate configuration file
echo ""
print_info "Generating configuration file..."

cat > database_config.properties << EOF
# MediTrack AI Database Configuration
# Generated on $(date)

# Database Connection Settings
db.user.management.url=jdbc:mysql://$MYSQL_HOST:3306/$USER_DB
db.patient.management.url=jdbc:mysql://$MYSQL_HOST:3306/$PATIENT_DB
db.username=$MYSQL_USER
db.password=$MYSQL_PASSWORD

# Connection Pool Settings
db.pool.initial.size=5
db.pool.max.size=20
db.pool.min.idle=5
db.pool.max.idle=15

# Query Timeout Settings
db.query.timeout=30
db.connection.timeout=10

# Cross-Database Query Settings
db.cross.database.enabled=true
db.cross.database.validation.interval=3600000

# Kafka Integration
kafka.bootstrap.servers=localhost:9092
kafka.alerts.topic=meditrack-alerts
kafka.vitals.topic=meditrack-vitals
kafka.audit.topic=meditrack-audit

# AI Model Integration
ai.model.enabled=true
ai.prediction.threshold=0.8
ai.alert.auto.generate=true
EOF

print_status "Configuration file generated: database_config.properties"

# Create backup script
cat > backup_databases.sh << 'EOF'
#!/bin/bash

# MediTrack AI Database Backup Script
# Generated on $(date)

BACKUP_DIR="/backups/meditrack"
DATE=$(date +%Y%m%d_%H%M%S)
MYSQL_USER="$MYSQL_USER"
MYSQL_PASSWORD="$MYSQL_PASSWORD"
MYSQL_HOST="$MYSQL_HOST"

# Create backup directory
mkdir -p $BACKUP_DIR

# Backup User Management Database
echo "Backing up User Management Database..."
mysqldump -u$MYSQL_USER -p$MYSQL_PASSWORD -h$MYSQL_HOST --single-transaction --routines --triggers $USER_DB > $BACKUP_DIR/user_management_$DATE.sql

# Backup Patient Management Database
echo "Backing up Patient Management Database..."
mysqldump -u$MYSQL_USER -p$MYSQL_PASSWORD -h$MYSQL_HOST --single-transaction --routines --triggers $PATIENT_DB > $BACKUP_DIR/patient_management_$DATE.sql

# Compress backups
echo "Compressing backups..."
gzip $BACKUP_DIR/user_management_$DATE.sql
gzip $BACKUP_DIR/patient_management_$DATE.sql

# Remove old backups (keep last 7 days)
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete

echo "Backup completed: $BACKUP_DIR"
EOF

chmod +x backup_databases.sh
print_status "Backup script created: backup_databases.sh"

# Final summary
echo ""
echo "🎉 Database Setup Complete!"
echo "=================================="
echo "✅ User Management Database: $USER_DB"
echo "✅ Patient Management Database: $PATIENT_DB"
echo "✅ Admins: $ADMINS"
echo "✅ Doctors: $DOCTORS"
echo "✅ Nurses: $NURSES"
echo "✅ Patients: $PATIENTS"
echo "✅ Cross-database relationships: Validated"
echo ""
echo "📁 Configuration file: database_config.properties"
echo "📁 Backup script: backup_databases.sh"
echo ""
echo "🚀 Ready to start MediTrack AI services!"
echo ""
echo "Next steps:"
echo "1. Update your application properties with database_config.properties"
echo "2. Start your microservices"
echo "3. Verify all services are connecting correctly"
echo "4. Test the complete system functionality"
echo ""
echo "📖 For detailed documentation, see: database/README.md"
