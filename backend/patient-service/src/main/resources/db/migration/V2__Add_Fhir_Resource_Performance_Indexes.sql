-- MediTrack AI - FHIR Resource Performance Indexes

CREATE INDEX idx_fhir_patient_type_resource_version
    ON fhir_resources (patient_id, resource_type, resource_id, resource_version);

CREATE INDEX idx_fhir_patient_type_resource_created
    ON fhir_resources (patient_id, resource_type, resource_id, created_at);
