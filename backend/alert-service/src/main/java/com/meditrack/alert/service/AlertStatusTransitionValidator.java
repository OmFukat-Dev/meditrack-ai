package com.meditrack.alert.service;

import com.meditrack.alert.entity.Alert;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class AlertStatusTransitionValidator {

    private static final Map<Alert.AlertStatus, Set<Alert.AlertStatus>> ALLOWED = Map.of(
        Alert.AlertStatus.ACTIVE, EnumSet.of(Alert.AlertStatus.ACKNOWLEDGED, Alert.AlertStatus.IN_PROGRESS, Alert.AlertStatus.ESCALATED, Alert.AlertStatus.RESOLVED),
        Alert.AlertStatus.ACKNOWLEDGED, EnumSet.of(Alert.AlertStatus.IN_PROGRESS, Alert.AlertStatus.ESCALATED, Alert.AlertStatus.RESOLVED),
        Alert.AlertStatus.IN_PROGRESS, EnumSet.of(Alert.AlertStatus.RESOLVED, Alert.AlertStatus.ESCALATED),
        Alert.AlertStatus.ESCALATED, EnumSet.of(Alert.AlertStatus.IN_PROGRESS, Alert.AlertStatus.RESOLVED),
        Alert.AlertStatus.RESOLVED, EnumSet.noneOf(Alert.AlertStatus.class)
    );

    private AlertStatusTransitionValidator() {
    }

    public static boolean isAllowed(Alert.AlertStatus current, Alert.AlertStatus next) {
        if (current == null || next == null) {
            return false;
        }
        if (current == next) {
            return true;
        }
        return ALLOWED.getOrDefault(current, EnumSet.noneOf(Alert.AlertStatus.class)).contains(next);
    }
}
