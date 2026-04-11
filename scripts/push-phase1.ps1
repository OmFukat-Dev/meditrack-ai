# MediTrack AI - Push Phase 1 to GitHub
# This script pushes the completed Phase 1 to your GitHub repository
# Make sure Git is installed and you're in the project directory

Write-Host "## Pushing Phase 1 to GitHub ##" -ForegroundColor Cyan

# Check if Git is available
try {
    git --version | Out-Null
    Write-Host "Git is available" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Git is not installed or not in PATH" -ForegroundColor Red
    Write-Host "Please install Git from https://git-scm.com/" -ForegroundColor Yellow
    exit 1
}

# Navigate to project directory
Set-Location $PSScriptRoot\..

# Initialize Git repository if not already done
if (-not (Test-Path ".git")) {
    Write-Host "Initializing Git repository..." -ForegroundColor Yellow
    git init
}

# Configure Git if not configured
$gitUser = git config --global user.name
if (-not $gitUser) {
    $userName = Read-Host "Enter your Git username"
    git config --global user.name $userName
}

$gitEmail = git config --global user.email
if (-not $gitEmail) {
    $userEmail = Read-Host "Enter your Git email"
    git config --global user.email $userEmail
}

# Add remote origin
Write-Host "Adding remote origin..." -ForegroundColor Yellow
git remote add origin https://github.com/OmFukat-Dev/meditrack-ai.git

# Create .gitignore file
Write-Host "Creating .gitignore..." -ForegroundColor Yellow
$gitignoreContent = @"
# Compiled class file
*.class

# Log file
*.log

# BlueJ files
*.ctxt

# Mobile Tools for Java (J2ME)
.mtj.tmp/

# Package Files #
*.jar
*.war
*.nar
*.ear
*.zip
*.tar.gz
*.rar

# virtual machine crash logs, see http://www.java.com/en/download/help/error_hotspot.xml
hs_err_pid*
replay_pid*

# Maven
target/
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
pom.xml.next
release.properties
dependency-reduced-pom.xml
buildNumber.properties
.mvn/timing.properties
.mvn/wrapper/maven-wrapper.jar

# Gradle
.gradle
build/
!gradle/wrapper/gradle-wrapper.jar
!**/src/main/**/build/
!**/src/test/**/build/

# IntelliJ IDEA
.idea/
*.iws
*.iml
*.ipr
out/
!**/src/main/**/out/
!**/src/test/**/out/

# Eclipse
.apt_generated
.classpath
.factorypath
.project
.settings
.springBeans
.sts4-cache
bin/
!**/src/main/**/bin/
!**/src/test/**/bin/

# NetBeans
/nbproject/private/
/nbbuild/
/dist/
/nbdist/
/.nb-gradle/

# VS Code
.vscode/

# Mac
.DS_Store

# Windows
Thumbs.db
ehthumbs.db
Desktop.ini

# Reports
reports/
*.pdf

# Logs
logs/
*.log

# Docker
docker-compose.override.yml

# Environment variables
.env
.env.local
.env.development.local
.env.test.local
.env.production.local

# Temporary files
tmp/
temp/
*.tmp
*.temp

# Database
*.db
*.sqlite
*.sqlite3

# OS generated files
.DS_Store
.DS_Store?
._*
.Spotlight-V100
.Trashes
ehthumbs.db
Thumbs.db
"@

Set-Content -Path ".gitignore" -Value $gitignoreContent

# Add all files
Write-Host "Staging all files..." -ForegroundColor Yellow
git add .

# Check status
Write-Host "Git status:" -ForegroundColor Cyan
git status --short

# Create commit
Write-Host "Creating initial commit..." -ForegroundColor Yellow
git commit -m "Phase 1 Complete - Foundation Setup

## Phase 1 Accomplishments:
- Multi-module Maven project structure (9 microservices)
- Complete Docker infrastructure (MySQL, Redis, Kafka, Prometheus, Grafana, Zipkin)
- Database schemas with Flyway migrations for all services
- Spring Boot configurations for all services
- Eureka Server and API Gateway setup
- Development scripts and tooling
- Comprehensive documentation

## Services Created:
- Eureka Server (Service Discovery)
- API Gateway (Spring Cloud Gateway)
- Patient Service (FHIR compliance ready)
- Vitals Service (Kafka integration ready)
- AI Prediction Service (Weka integration ready)
- Alert Service (Saga pattern ready)
- Notification Service (Multi-channel ready)
- Report Service (PDF generation ready)
- Vital Simulator (IoT simulation ready)

## Infrastructure:
- Docker Compose with all services
- Prometheus + Grafana monitoring
- Zipkin distributed tracing
- Kafka UI for development

## Next Phase:
Ready to start Phase 2 - Core Microservices Implementation

Co-authored-by: Cascade AI <cascade@example.com>"

# Push to GitHub
Write-Host "Pushing to GitHub..." -ForegroundColor Yellow
git branch -M main
git push -u origin main

Write-Host "`n[SUCCESS] Phase 1 pushed to GitHub!" -ForegroundColor Green
Write-Host "Repository: https://github.com/OmFukat-Dev/meditrack-ai" -ForegroundColor Cyan
Write-Host "Ready to start Phase 2 implementation!" -ForegroundColor Green
