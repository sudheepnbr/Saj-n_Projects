package com.oih.idp.web;

import com.oih.idp.config.IdpProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class UiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(UiExceptionHandler.class);

    private final Environment environment;
    private final IdpProperties properties;

    public UiExceptionHandler(Environment environment, IdpProperties properties) {
        this.environment = environment;
        this.properties = properties;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String unexpected(Exception ex, HttpServletRequest request, Model model) {
        if (ex instanceof AccessDeniedException || ex instanceof AuthenticationException) {
            throw (RuntimeException) ex;
        }
        log.error("Request failed {} {}", request.getMethod(), request.getRequestURI(), ex);
        model.addAttribute("status", 500);
        model.addAttribute("displayName", properties.getDisplayName());
        boolean show = !environment.acceptsProfiles(Profiles.of("prod", "production"));
        model.addAttribute("message", show ? ex.getClass().getSimpleName() + ": " + ex.getMessage() : null);
        return "error";
    }
}
