package edu.hingu.project.utils;

import org.springframework.security.core.Authentication;

import edu.hingu.project.entities.User;

public record CurrentUserContext(User user, Authentication auth) {}
