package com.meditrack.notification.dto;

import java.util.ArrayList;
import java.util.List;

public class NotificationResponse {

    private String requestId;
    private String overallStatus;
    private int successCount;
    private int failureCount;
    private int scheduledCount;
    private String message;
    private List<NotificationChannelResult> results = new ArrayList<>();

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public int getScheduledCount() {
        return scheduledCount;
    }

    public void setScheduledCount(int scheduledCount) {
        this.scheduledCount = scheduledCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<NotificationChannelResult> getResults() {
        return results;
    }

    public void setResults(List<NotificationChannelResult> results) {
        this.results = results;
    }
}
