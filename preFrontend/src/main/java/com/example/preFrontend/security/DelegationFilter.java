package com.example.preFrontend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ietf.jgss.*;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.security.auth.login.Configuration;
import java.io.IOException;

public class DelegationFilter extends OncePerRequestFilter {

    private final AuthenticationManager authenticationManager;
    
    public DelegationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;    
    }
    
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication != null && authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        var authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // if no auth header is present, initiate the negotiate authentication
        if (authHeader == null || !authHeader.trim().startsWith("Negotiate ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            var authToken = new DelegationAuthToken(authHeader);
            authenticationManager.authenticate(authToken);
            
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
