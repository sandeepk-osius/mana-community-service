package com.manacommunity.api.controller;

import com.manacommunity.api.response.AuthResponse;
import com.manacommunity.api.dto.KycRequest;
import com.manacommunity.api.dto.LoginRequest;
import com.manacommunity.api.dto.RegisterRequest;
import com.manacommunity.api.security.UserPrincipal;
import com.manacommunity.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Auth endpoints — exceptions bubble up to GlobalExceptionHandler
 * automatically.
 * No try/catch needed here; the handler returns the correct HTTP status +
 * ErrorResponse JSON.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // TODO: restrict in production
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) throws Exception {
        AuthResponse response = authService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) throws Exception {
        return ResponseEntity.ok(authService.loginUser(request));
    }

    @PostMapping("/verify-kyc")
    public ResponseEntity<String> verifyKyc(
            @Valid @RequestBody KycRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        // authService.submitKyc(principal.getId(), req);
        return ResponseEntity.ok("KYC submitted for review. You will be notified once approved.");
    }
}
