package com.example.backend;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.kerberos.authentication.KerberosAuthenticationProvider;
import org.springframework.security.kerberos.authentication.KerberosServiceAuthenticationProvider;
import org.springframework.security.kerberos.authentication.sun.SunJaasKerberosClient;
import org.springframework.security.kerberos.authentication.sun.SunJaasKerberosTicketValidator;
import org.springframework.security.kerberos.web.authentication.SpnegoAuthenticationProcessingFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${kerberos.keytab}")
    private String keytab;

    @Value("${kerberos.service-principal}")
    private String servicePrincipal;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SpnegoAuthenticationProcessingFilter spnegoFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().authenticated()
                )
                .addFilterBefore(spnegoFilter, BasicAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public SpnegoAuthenticationProcessingFilter spnegoAuthenticationProcessingFilter(AuthenticationManager authenticationManager) {
        var filter = new SpnegoAuthenticationProcessingFilter();
        filter.setAuthenticationManager(authenticationManager);
        return filter;
    }

    @Bean
    public KerberosAuthenticationProvider kerberosAuthenticationProvider() {
        var provider = new KerberosAuthenticationProvider();
        var client = new SunJaasKerberosClient();
        provider.setKerberosClient(client);
        provider.setUserDetailsService(kerberosUserDetailsService());
        return provider;
    }

    @Bean
    public KerberosServiceAuthenticationProvider kerberosServiceAuthenticationProvider(
            SunJaasKerberosTicketValidator ticketValidator,
            UserDetailsService userDetailsService
    ){
        var provider = new KerberosServiceAuthenticationProvider();
        provider.setTicketValidator(ticketValidator);
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    @Bean
    public SunJaasKerberosTicketValidator sunJaasKerberosTicketValidator() {
        var tickerValidator = new SunJaasKerberosTicketValidator();
        tickerValidator.setServicePrincipal(servicePrincipal);
        tickerValidator.setKeyTabLocation(new FileSystemResource(keytab));
        tickerValidator.setDebug(true);
        return tickerValidator;
    }

    @Bean
    public UserDetailsService kerberosUserDetailsService() {
        return principalName -> {
            System.out.println("Kerberos principal name: " + principalName);

            if (principalName.startsWith("user")) {
                return new User(principalName, "notUsed", AuthorityUtils.createAuthorityList("USER"));
            }

            throw new UsernameNotFoundException("Kerberos user not found: " + principalName);
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(
            HttpSecurity http,
            KerberosServiceAuthenticationProvider kerberosServiceAuthenticationProvider,
            KerberosAuthenticationProvider kerberosAuthenticationProvider
    ) throws Exception {
        var authenticationManager = http.getSharedObject(AuthenticationManagerBuilder.class)
                .authenticationProvider(kerberosAuthenticationProvider)
                .authenticationProvider(kerberosServiceAuthenticationProvider);

        return authenticationManager.build();
    }
}
