package com.meditrack.alert.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmsGatewayService {

    private static final Logger logger = LoggerFactory.getLogger(SmsGatewayService.class);

    public boolean sendSms(String recipient, String message) {
        logger.info("Simulated SMS gateway send to {}: {}", recipient, message);
        return true;
    }
}
