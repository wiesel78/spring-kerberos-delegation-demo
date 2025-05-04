package com.example.frontend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
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
import org.springframework.security.kerberos.client.KerberosRestTemplate;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

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
    public KerberosRestTemplate kerberosRestTemplate() {
        return new org.springframework.security.kerberos.client.KerberosRestTemplate(keytab,servicePrincipal);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            KerberosFilter filter
    ) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public KerberosFilter kerberosFilter(AuthenticationManager authManager) {
        var filter = new KerberosFilter(servicePrincipal, keytab, backendSpn, krb5ConfigPath);
        filter.setAuthManager(authManager);
        return filter;
    }

    @Primary
    @Bean
    public AuthenticationManager kerberosAuthManager(
            AuthenticationProvider kerberosAuthProvider,
            KerberosServiceAuthenticationProvider kerberosServiceAuthProvider
    ) {
        return new ProviderManager(List.of(
                kerberosAuthProvider,
                kerberosServiceAuthProvider
        ));
    }

    @Primary
    @Bean
    public AuthenticationProvider kerberosAuthProvider(
            UserDetailsService userDetailsService
    ) {
        var provider = new KerberosAuthenticationProvider();
        var client = new SunJaasKerberosClient();
        provider.setKerberosClient(client);
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    @Bean
    public KerberosServiceAuthenticationProvider kerberosServiceAuthProvider(
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

    @Primary
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
}
