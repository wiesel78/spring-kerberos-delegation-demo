package com.example.frontend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${kerberos.keytab}")
    private String keytab;

    @Value("${kerberos.service-principal}")
    private String servicePrincipal;

    @Value("${backend.spn}")
    private String backendSpn;

    @Value("${kerberos.krb5ConfigPath}")
    private String krb5ConfigPath;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DelegationFilter filter
    ) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/unauth/*").permitAll()
                    .anyRequest().authenticated())
            .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DelegationFilter delegationFilter() {
        return new DelegationFilter(servicePrincipal, keytab, backendSpn, krb5ConfigPath);
    }
}
