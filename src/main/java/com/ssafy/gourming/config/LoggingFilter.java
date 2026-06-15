package com.ssafy.gourming.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getContentType() != null && request.getContentType().contains("multipart/form-data")) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper cachingRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper cachingResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(cachingRequest, cachingResponse);
        } finally {
            long timeTaken = System.currentTimeMillis() - startTime;

            String requestBody = new String(cachingRequest.getContentAsByteArray(), StandardCharsets.UTF_8);
            String responseBody = new String(cachingResponse.getContentAsByteArray(), StandardCharsets.UTF_8);

            int status = cachingResponse.getStatus();
            String method = request.getMethod();
            String uri = request.getRequestURI();
            String clientIp = getClientIp(request);
            String userEmail = extractUserFromJwt(request);

            StringBuilder logMessage = new StringBuilder();
            logMessage.append("\n===== [HTTP ").append(method).append(" ").append(uri).append("] ")
                      .append(status).append(" - ").append(timeTaken).append("ms =====")
                      .append("\n  Client IP   : ").append(clientIp)
                      .append("\n  User (JWT)  : ").append(userEmail);

            String queryString = request.getQueryString();
            if (queryString != null && !queryString.isEmpty()) {
                logMessage.append("\n  Query Params:");
                String[] pairs = queryString.split("&");
                for (String pair : pairs) {
                    String[] kv = pair.split("=", 2);
                    String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                    String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
                    logMessage.append("\n    - ").append(key).append(": ").append(value);
                }
            }

            if (!requestBody.isEmpty()) {
                String maskedRequestBody = maskSensitiveData(requestBody);
                logMessage.append("\n  Request Body : ").append(maskedRequestBody);
            }

            if (!responseBody.isEmpty() && cachingResponse.getContentType() != null && cachingResponse.getContentType().contains("application/json")) {
                String maskedResponseBody = maskSensitiveData(responseBody);
                logMessage.append("\n  Response Body: ").append(maskedResponseBody);
            }
            logMessage.append("\n===================================================================");

            log.info(logMessage.toString());

            cachingResponse.copyBodyToResponse();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String extractUserFromJwt(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                String[] parts = token.split("\\.");
                if (parts.length >= 2) {
                    String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                    Matcher m = Pattern.compile("\"sub\":\"([^\"]+)\"").matcher(payload);
                    if (m.find()) {
                        return m.group(1); // sub 에 저장된 이메일 반환
                    }
                }
            } catch (Exception e) {
                return "Invalid/Unparsable Token";
            }
        }
        return "Anonymous";
    }

    private String maskSensitiveData(String body) {
        if (body == null || body.isEmpty()) return body;
        // JSON 형식 마스킹 (예: "password": "secret", "token": "eyJ...")
        body = body.replaceAll("(?i)(\"(?:password|token|accessToken|refreshToken)\"\\s*:\\s*\")[^\"]+(\")", "$1***$2");
        // x-www-form-urlencoded 형식 마스킹 (예: password=secret&...)
        body = body.replaceAll("(?i)((?:password|token|accessToken|refreshToken)=)([^&]+)", "$1***");
        return body;
    }
}
