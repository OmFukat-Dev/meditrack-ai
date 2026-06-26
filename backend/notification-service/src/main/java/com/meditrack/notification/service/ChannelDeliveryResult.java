package com.meditrack.notification.service;

import com.meditrack.notification.domain.NotificationChannel;

import java.util.LinkedHashMap;
import java.util.Map;

public class ChannelDeliveryResult {

    private boolean success;
    private NotificationChannel channel;
    private String provider;
    private String responseCode;
    private String responseMessage;
    private String errorMessage;
    private Map<String, Object> metadata = new LinkedHashMap<>();

    public static ChannelDeliveryResult success(
        NotificationChannel channel,
        String provider,
        String responseCode,
        String responseMessage
    ) {
        ChannelDeliveryResult result = new ChannelDeliveryResult();
        result.setSuccess(true);
        result.setChannel(channel);
        result.setProvider(provider);
        result.setResponseCode(responseCode);
        result.setResponseMessage(responseMessage);
        return result;
    }

    public static ChannelDeliveryResult failure(
        NotificationChannel channel,
        String provider,
        String responseCode,
        String responseMessage,
        String errorMessage
    ) {
        ChannelDeliveryResult result = new ChannelDeliveryResult();
        result.setSuccess(false);
        result.setChannel(channel);
        result.setProvider(provider);
        result.setResponseCode(responseCode);
        result.setResponseMessage(responseMessage);
        result.setErrorMessage(errorMessage);
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
