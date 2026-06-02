package com.manacommunity.api.service;

import com.manacommunity.api.response.AuthResponse;
import com.manacommunity.api.dto.KycRequest;
import com.manacommunity.api.dto.LoginRequest;
import com.manacommunity.api.dto.RegisterRequest;

public interface AuthService {
    AuthResponse registerUser(RegisterRequest request) throws Exception;
    AuthResponse loginUser(LoginRequest request) throws Exception;

    boolean submitKyc(Long userId, KycRequest req);
}
