package edu.hingu.project.services;

import org.springframework.security.authentication.BadCredentialsException;

import edu.hingu.project.dtos.JwtResponse;
import edu.hingu.project.entities.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    JwtResponse authenticateAndGenerateToken(User user);

    public Cookie loginAndCreateJwtCookie(User user) throws BadCredentialsException;

    void clearJwtCookie(HttpServletResponse response);

}
