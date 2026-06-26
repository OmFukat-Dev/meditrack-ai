package com.meditrack.vitals.kafka;

public final class KafkaEventOwnershipValidator {

    private KafkaEventOwnershipValidator() {
    }

    public static boolean isValid(String patientId, String department, String createdBy, String role) {
        return patientId != null && !patientId.isBlank()
            && department != null && !department.isBlank()
            && createdBy != null && !createdBy.isBlank()
            && role != null && !role.isBlank();
    }
}
