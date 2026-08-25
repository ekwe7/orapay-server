package com.orapay.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orapay.common.dto.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    public static final String ALT_IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        // Filter only mutating requests to /api/transfers or /api/payments
        boolean isMutationMethod = "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method);
        boolean isTargetEndpoint = path.startsWith("/api/transfers") || path.startsWith("/api/payments");
        return !isMutationMethod || !isTargetEndpoint;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = request.getHeader(ALT_IDEMPOTENCY_KEY_HEADER);
        }

        // If target endpoint is /api/transfers and header is missing, require Idempotency-Key
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            if (request.getRequestURI().startsWith("/api/transfers")) {
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                ApiResponse<Object> errorResp = ApiResponse.error("Header 'Idempotency-Key' is mandatory for transfer operations");
                response.getWriter().write(objectMapper.writeValueAsString(errorResp));
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        // Replay Protection: Check if key was already processed
        Optional<IdempotencyRecord> existingRecordOpt = idempotencyRepository.findById(idempotencyKey);
        if (existingRecordOpt.isPresent()) {
            IdempotencyRecord record = existingRecordOpt.get();
            log.info("Replay detected for Idempotency-Key: [{}]. Serving cached response payload.", idempotencyKey);
            response.setStatus(record.getResponseStatus());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("X-Cache-Lookup", "HIT");
            response.getWriter().write(record.getResponseBody());
            return;
        }

        // Cache new request execution response
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(request, responseWrapper);
            int status = responseWrapper.getStatus();

            if (status >= 200 && status < 300) {
                byte[] responseArray = responseWrapper.getContentAsByteArray();
                String responseBody = new String(responseArray, StandardCharsets.UTF_8);

                IdempotencyRecord record = new IdempotencyRecord();
                record.setIdempotencyKey(idempotencyKey);
                record.setRequestPath(request.getRequestURI());
                record.setResponseStatus(status);
                record.setResponseBody(responseBody);

                idempotencyRepository.save(record);
                log.info("Successfully cached response payload for new Idempotency-Key: [{}]", idempotencyKey);
            }
        } finally {
            responseWrapper.copyBodyToResponse();
        }
    }
}
