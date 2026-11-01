package com.marketplace.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@Component
public class SecurityProblemWriter {
    private final ObjectMapper objectMapper;

    public SecurityProblemWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void unauthenticated(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        write(request, response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication is required");
    }

    public void forbidden(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) throws IOException {
        if (exception instanceof CsrfException) {
            write(request, response, HttpStatus.FORBIDDEN, "INVALID_CSRF_TOKEN", "Fetch a new CSRF token and retry the request");
        } else {
            write(request, response, HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to perform this operation");
        }
    }

    private void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String code, String message) throws IOException {
        var problem = ProblemDetail.forStatusAndDetail(status, message);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        problem.setProperty("errors", List.of());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
