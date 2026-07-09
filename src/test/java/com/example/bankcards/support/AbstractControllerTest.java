package com.example.bankcards.support;

import com.example.bankcards.security.ErrorResponseWriter;
import com.example.bankcards.security.JwtService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Shared mocks required because security {@code @Component} beans are picked up in {@code @WebMvcTest}.
 */
public abstract class AbstractControllerTest {

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private ErrorResponseWriter errorResponseWriter;
}
