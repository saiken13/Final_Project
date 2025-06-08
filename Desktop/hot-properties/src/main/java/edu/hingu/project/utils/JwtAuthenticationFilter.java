package edu.hingu.project.utils;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Autowired
    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        log.debug("intercepting request path: {}", path);

        if (path.equals("/") || path.equals("/index")
                || path.equals("/login") || path.equals("/register")
                || path.equals("/browse") || path.equals("/error")
                || path.startsWith("/css") || path.startsWith("/js")
                || path.startsWith("/webjars")
                || path.startsWith("/profile-pictures")
                || path.startsWith("/uploads")
                || path.matches("^/images/.*\\.(png|jpg|jpeg|webp|svg)$")) {
            log.debug("Skipping JWT auth for public path: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        String token = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                log.debug("Cookie found: {} = {}", cookie.getName(), cookie.getValue());
                if ("jwt".equals(cookie.getName())) {
                    token = cookie.getValue();
                    log.debug("JWT token extracted from cookie");
                    break;
                }
            }
        } else {
            log.debug("No cookies found in request");
        }

        if (token == null || token.trim().isEmpty()) {
            log.warn("No JWT token found - skipping authentication");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String username = jwtUtil.extractUsername(token);
            log.debug("Extracted username from JWT: {}", username);

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            log.debug("Loaded user details for: {}", userDetails.getUsername());

            if (jwtUtil.validateToken(token, userDetails)) {
                log.debug("JWT token is valid");
            
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                request.getSession(true); 
            
                log.info("Authenticated user: {}", username);
            }
             else {
                log.warn("JWT token is invalid");
            }

        } catch (Exception ex) {
            log.error("Exception during JWT validation: {}", ex.getMessage(), ex);
        }

        filterChain.doFilter(request, response);
    }
}
