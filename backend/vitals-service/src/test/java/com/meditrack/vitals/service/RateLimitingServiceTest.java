package com.meditrack.vitals.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitingServiceTest {

    @Mock
    private RedisCacheService redisCacheService;

    @InjectMocks
    private RateLimitingService rateLimitingService;

    @Test
    void checkPatientRateLimitAllowsWithinQuota() {
        String key = "rate_limit:patient:123:patient_minute";
        when(redisCacheService.isRateLimited(eq(key), eq(200), eq(60))).thenReturn(false);
        when(redisCacheService.getRateLimitCount(eq(key))).thenReturn(0L);

        RateLimitingService.RateLimitResult result = rateLimitingService.checkPatientRateLimit("123");

        assertTrue(result.isAllowed());
        assertEquals(200, result.getRemainingRequests());
        assertEquals("patient_minute", result.getLimitType());
        assertEquals("Request allowed", result.getUserMessage());
    }

    @Test
    void checkPatientRateLimitBlocksWhenRedisDenies() {
        String key = "rate_limit:patient:123:patient_minute";
        when(redisCacheService.isRateLimited(eq(key), eq(200), eq(60))).thenReturn(true);
        when(redisCacheService.getRateLimitCount(eq(key))).thenReturn(201L);

        RateLimitingService.RateLimitResult result = rateLimitingService.checkPatientRateLimit("123");

        assertFalse(result.isAllowed());
        assertEquals(0, result.getRemainingRequests());
        assertEquals("patient_minute", result.getLimitType());
        assertEquals("Rate limit exceeded", result.getUserMessage());
    }
}
