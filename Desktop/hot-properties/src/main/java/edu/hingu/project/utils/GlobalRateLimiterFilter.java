package edu.hingu.project.utils;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class GlobalRateLimiterFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 500; 
    private static final long WINDOW_MS = 60_000; 

    private final Map<String, RequestWindow> ipRequests = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String ip = request.getRemoteAddr();

        if ("127.0.0.1".equals(ip) || "::1".equals(ip)) {
            filterChain.doFilter(request, response);
            return;
        }

        Instant now = Instant.now();
        RequestWindow window = ipRequests.computeIfAbsent(ip, k -> new RequestWindow());

        synchronized (window) {
            if (now.isAfter(window.windowStart.plusMillis(WINDOW_MS))) {
                window.windowStart = now;
                window.requestCount = 1;
            } else {
                window.requestCount++;
            }

            if (window.requestCount > MAX_REQUESTS) {
                response.setStatus(429);
                response.setContentType("application/json");
                String s = String.format("""
                    {
                      "error": "Rate Limit Exceeded",
                      "message": "Exceeded %d calls per %d seconds. Please slow down."
                    }
                """, MAX_REQUESTS, WINDOW_MS / 1000);
                response.getWriter().write(s);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private static class RequestWindow {
        Instant windowStart = Instant.now();
        int requestCount = 0;
    }
}
