package com.example.preFrontend.security;

import com.example.preFrontend.configs.KerberosConfig;
import org.ietf.jgss.GSSException;
import org.springframework.stereotype.Component;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * manage the kerberos session of this service that
 * can be cached until expiration.
 */
@Component
public class SubjectManager {
    private volatile Subject serviceSubject;
    private final Object lock = new Object();
    private final Configuration jaasConfig;
    
    public SubjectManager(KerberosConfig kerberosConfig) {
        setSystemProperties(kerberosConfig.getKrb5ConfigPath(), kerberosConfig.getKeytab());
        this.jaasConfig = getJaasConfig(kerberosConfig.getKeytab(), kerberosConfig.getServicePrincipal());
    }

    /**
     * check the subject for expiration
     * 
     * @param subject subject to validate
     * @return true, if subject is not expired, false, otherwise
     */
    public boolean isValid(Subject subject){
        if(subject == null) {
            return false;
        }
        
        return subject.getPrivateCredentials(KerberosTicket.class).stream()
                .anyMatch(KerberosTicket::isCurrent);
    }

    /**
     * Login as the service principal using JAAS.
     *
     * @return the Subject representing the service principal
     * @throws LoginException if the keytab or principal is invalid
     */
    public Subject loginAsService() throws LoginException {
        LoginContext lc = createLoginContext();
        lc.login();
        return lc.getSubject();
    }
    
    /**
     * get the current available and valid subject if exist or login
     * and create so a new subject.
     * 
     * @return a valid subject
     * @throws LoginException if the service login failed
     */
    public Subject getOrCreate() throws LoginException {
        if (isValid(serviceSubject)) {
            return serviceSubject;
        }
        
        synchronized (lock) {
            if (isValid(serviceSubject)) {
                return serviceSubject;
            }
            
            serviceSubject = loginAsService();
            return serviceSubject;
        }
    }
    
    /**
     * run the given method in context of the service subject.
     * if the service subject is expired or not available, it will
     * create a new subject
     * 
     * @param action the code that should be run in the context of the subject
     * @return the return object of the action
     * @param <T> the return type of the action
     * @throws LoginException if the service login was not successful
     */
    public <T> T doAsService(Callable<T> action) throws LoginException {
        try {
            return Subject.callAs(getOrCreate(), action);
        } catch (LoginException e) {
            if (isKerberosExpiredException(e)) {
                synchronized (lock) {
                    serviceSubject = loginAsService();
                }
                return Subject.callAs(serviceSubject, action);
            } else {
                throw e;
            }
        }
    }
    
    protected LoginContext createLoginContext() throws LoginException {
        return new LoginContext("ServiceLogin", null, null, this.jaasConfig);
    }

    protected boolean isKerberosExpiredException(Exception e) {
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof GSSException) {
                String msg = cause.getMessage();
                return msg != null && (
                        msg.contains("Replay") || msg.contains("expired") || msg.contains("invalid")
                );
            }
            cause = cause.getCause();
        }
        return false;
    }

    private static void setSystemProperties(String krb5ConfigPath, String keytabPath) {
        System.setProperty("java.security.krb5.conf", krb5ConfigPath);
        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
        System.setProperty("sun.security.krb5.debug", "true"); // Enable for debugging
        System.setProperty("sun.security.krb5.allowS4U2ProxyAndDelegate", "true");
        System.setProperty("java.security.debug", "gssloginconfig,configfile,logincontext");
        System.setProperty("java.security.krb5.keytab", keytabPath);
    }

    private static Configuration getJaasConfig(String keytabPath, String servicePrincipal) {
        return new Configuration() {
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                Map<String, Object> options = Map.of(
                        "keyTab", keytabPath,
                        "principal", servicePrincipal,
                        "storeKey", "true",
                        "useKeyTab", "true",
                        "doNotPrompt", "true",
                        "isInitiator", "true",
                        "refreshKrb5Config", "true"
                );
                return new AppConfigurationEntry[]{
                        new AppConfigurationEntry(
                                "com.sun.security.auth.module.Krb5LoginModule",
                                AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                                options
                        )
                };
            }
        };
    }
}
