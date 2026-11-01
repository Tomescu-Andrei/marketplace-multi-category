package com.marketplace.controller;

import com.marketplace.dto.*;
import com.marketplace.service.AccountService;
import com.marketplace.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Session-based authentication. Fetch a CSRF token before POST requests and again after login/logout.")
public class AuthController {
    private final AccountService accounts;
    private final CurrentUserService currentUser;
    private final AuthenticationManager authenticationManager;
    private final SessionAuthenticationStrategy sessionStrategy;
    private final SecurityContextRepository contexts;

    public AuthController(AccountService accounts, CurrentUserService currentUser, AuthenticationManager authenticationManager,
                          SessionAuthenticationStrategy sessionStrategy, SecurityContextRepository contexts) {
        this.accounts = accounts;
        this.currentUser = currentUser;
        this.authenticationManager = authenticationManager;
        this.sessionStrategy = sessionStrategy;
        this.contexts = contexts;
    }

    @GetMapping("/csrf")
    @Operation(summary = "Get a CSRF token", description = "Send the returned token in the named header on POST requests. The same browser/session must be used.")
    public CsrfResponse csrf(@Parameter(hidden = true) CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register an account", description = "Creates an account without automatically logging in. Emails are stored in lowercase.")
    public UserResponse register(@RequestBody @Valid RegisterRequest request) {
        return accounts.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in", description = "Sets an HttpOnly session cookie. The session ID and CSRF token rotate; fetch a new CSRF token after success.")
    public UserResponse login(@RequestBody @Valid LoginRequest login, HttpServletRequest request, HttpServletResponse response) {
        if (login.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadCredentialsException("Invalid credentials");
        }
        var authentication = authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
                login.email().toLowerCase(Locale.ROOT), login.password()));
        sessionStrategy.onAuthentication(authentication, request, response);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        return currentUser.requireUser();
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user")
    public UserResponse me() {
        return currentUser.requireUser();
    }

    // The Security logout filter handles this route before MVC; the method documents the API.
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Log out", description = "Invalidates the session and clears its cookie. Requires a current CSRF token.")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
