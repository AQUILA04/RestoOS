package com.resto.core.idempotency;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

    private static final int MAX_ENTRIES = 1000;
    private final Map<String, String> processedKeys = Collections.synchronizedMap(
            new LinkedHashMap<String, String>(MAX_ENTRIES, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > MAX_ENTRIES;
                }
            }
    );

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            String idempotencyKey = request.getHeader("Idempotency-Key");
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                if (processedKeys.containsKey(idempotencyKey)) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json");
                    response.getWriter().write(processedKeys.get(idempotencyKey));
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) {
        if ("POST".equalsIgnoreCase(request.getMethod()) && response.getStatus() >= 200 && response.getStatus() < 300) {
            String idempotencyKey = request.getHeader("Idempotency-Key");
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                processedKeys.put(idempotencyKey, "{\"status\":\"PROCESSED\",\"idempotencyKey\":\"" + idempotencyKey + "\"}");
            }
        }
    }
}
