package com.example.bankcards.service;

import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.response.AuthResponse;
import com.example.bankcards.dto.response.UserResponse;
import com.example.bankcards.security.JwtService;
import com.example.bankcards.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String accessToken = jwtService.generateToken(principal.username(), principal.role());

        UserResponse userResponse = new UserResponse(
                principal.id(),
                principal.username(),
                principal.role(),
                principal.enabled(),
                principal.createdAt()
        );
        return new AuthResponse(accessToken, "Bearer", userResponse);
    }
}
