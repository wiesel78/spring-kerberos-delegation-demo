package com.example.preFrontend.configs;

import com.example.preFrontend.security.DelegationAuthProvider;
import com.example.preFrontend.security.DelegationFilter;
import com.example.preFrontend.security.NegotiateAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DelegationFilter filter
    ) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/unauth/*").permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .anyRequest().authenticated())
            .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(new NegotiateAuthenticationEntryPoint()));

        return http.build();
    }

    @Bean
    public DelegationFilter delegationFilter(AuthenticationManager authenticationManager) {
        return new DelegationFilter(authenticationManager);
    }
    
    @Bean
    public AuthenticationManager authenticationManager(DelegationAuthProvider delegationAuthProvider) {
        return new ProviderManager(List.of(delegationAuthProvider));
    }
}
