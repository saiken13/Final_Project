package edu.hingu.project.services;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import edu.hingu.project.dtos.JwtResponse;
import edu.hingu.project.entities.User;
import edu.hingu.project.repositories.UserRepository;
import edu.hingu.project.utils.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           JwtUtil jwtUtil, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    public JwtResponse authenticateAndGenerateToken(User user) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            String token = jwtUtil.generateToken(userDetails);

            return new JwtResponse(token);
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    @Override
    public Cookie loginAndCreateJwtCookie(User user) throws BadCredentialsException {
        return loginAndCreateJwtCookieByEmail(user);
    }

    @Override
    public Cookie loginAndCreateJwtCookieByEmail(User userInput) throws BadCredentialsException {
        User user = userRepository.findByEmail(userInput.getEmail())
            .orElseThrow(() -> new BadCredentialsException("Email not found"));
    
        if (!passwordEncoder.matches(userInput.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }
    
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            user.getPassword(),
            user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList())
        );
    
        String token = jwtUtil.generateToken(userDetails);
    
        Cookie jwtCookie = new Cookie("jwt", token);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(60 * 60); // 1 hour
    
        return jwtCookie;
    }
    


    @Override
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public void clearJwtCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        cookie.setSecure(true); // only for HTTPS
        response.addCookie(cookie);
    }

    @Override
        public User getCurrentUser() {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        }


    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
} 
