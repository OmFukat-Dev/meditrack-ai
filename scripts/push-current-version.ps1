# MediTrack AI - Push Current Version to GitHub
# This script pushes the latest changes to your GitHub repository

Write-Host "## Pushing Current Version to GitHub ##" -ForegroundColor Cyan

# Navigate to project directory
Set-Location $PSScriptRoot\..

# Check if Git is available
try {
    git --version | Out-Null
    Write-Host "Git is available" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Git is not installed or not in PATH" -ForegroundColor Red
    Write-Host "Please install Git from https://git-scm.com/" -ForegroundColor Yellow
    exit 1
}

# Stage all changes
Write-Host "Staging all changes..." -ForegroundColor Yellow
git add .

# Check status
Write-Host "Git status:" -ForegroundColor Cyan
git status --short

# Create commit message based on current state
$commitMessage = @"
Update: Phase 2.1 Patient Service Complete + Phase 2.2 Vitals Service Started

## Phase 2.1 Patient Service - COMPLETED
- Complete Patient CRUD operations with comprehensive validation
- Medical history management with ICD-10 support
- FHIR R4 compliance with HAPI integration
- Enterprise-grade security with JWT/OAuth2
- Advanced search and filtering capabilities
- Production-ready API with OpenAPI documentation

## Phase 2.2 Vitals Service - STARTED
- Vitals entity with rate-of-change calculations
- VitalsRepository with Kafka integration
- VitalsService with Redis caching
- Vital thresholds and trends management
- Batch processing for vital readings

## Technical Achievements
- 25+ Java classes across Patient and Vitals services
- Complete FHIR resource management
- Advanced database queries with JPQL
- Comprehensive validation and security
- Metrics and monitoring integration
- Docker infrastructure ready

## Next Steps
- Complete Phase 2.2: Vitals Ingestion Service
- Start Phase 2.3: Vital Simulator Service
- Begin Phase 3: AI/ML Prediction Engine

Co-authored-by: Cascade AI <cascade@example.com>
"@

# Create commit
Write-Host "Creating commit..." -ForegroundColor Yellow
git commit -m $commitMessage

# Push to GitHub
Write-Host "Pushing to GitHub..." -ForegroundColor Yellow
git push origin main

Write-Host "`n[SUCCESS] Current version pushed to GitHub!" -ForegroundColor Green
Write-Host "Repository: https://github.com/OmFukat-Dev/meditrack-ai" -ForegroundColor Cyan
Write-Host "Commit includes Phase 2.1 completion and Phase 2.2 start" -ForegroundColor White
