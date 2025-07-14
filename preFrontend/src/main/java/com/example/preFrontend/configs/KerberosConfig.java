package com.example.preFrontend.configs;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "kerberos")
@Data
public class KerberosConfig {
    private String keytab;
    private String servicePrincipal;
    private String krb5ConfigPath;
    private boolean debug;
}
