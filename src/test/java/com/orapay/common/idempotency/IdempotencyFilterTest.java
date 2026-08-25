package com.orapay.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IdempotencyFilterTest {

    private IdempotencyRepository idempotencyRepository;
    private ObjectMapper objectMapper;
    private IdempotencyFilter idempotencyFilter;

    @BeforeEach
    void setUp() {
        idempotencyRepository = mock(IdempotencyRepository.class);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        idempotencyFilter = new IdempotencyFilter(idempotencyRepository, objectMapper);
    }

    @Test
    @DisplayName("Should serve cached response payload on replay request with matching Idempotency-Key")
    void testReplayDuplicateRequestServesCachedPayload() throws Exception {
        String key = "test-key-12345";
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(key);
        record.setRequestPath("/api/transfers");
        record.setResponseStatus(200);
        record.setResponseBody("{\"status\":\"SUCCESS\",\"data\":{\"transactionId\":\"tx-123\"}}");

        when(idempotencyRepository.findById(key)).thenReturn(Optional.of(record));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/transfers");
        request.addHeader("Idempotency-Key", key);

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        idempotencyFilter.doFilterInternal(request, response, filterChain);

        // Verify downstream filter chain was NOT called again (no double-processing)
        verify(filterChain, never()).doFilter(any(), any());

        // Verify response payload is returned from cache
        assertEquals(200, response.getStatus());
        assertEquals("HIT", response.getHeader("X-Cache-Lookup"));
        assertTrue(response.getContentAsString().contains("tx-123"));
    }

    @Test
    @DisplayName("Should reject transfer request without Idempotency-Key header with 400 Bad Request")
    void testMissingIdempotencyHeaderRejection() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/transfers");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        idempotencyFilter.doFilterInternal(request, response, filterChain);

        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().contains("Header 'Idempotency-Key' is mandatory"));
        verify(filterChain, never()).doFilter(any(), any());
    }
}
