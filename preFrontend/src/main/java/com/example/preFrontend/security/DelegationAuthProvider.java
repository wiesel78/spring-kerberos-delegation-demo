package com.example.preFrontend.security;

import com.example.preFrontend.configs.KerberosConfig;
import com.sun.security.jgss.ExtendedGSSCredential;
import lombok.extern.slf4j.Slf4j;
import org.ietf.jgss.*;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.util.Base64;
import java.util.Map;

@Component
@Slf4j
public class DelegationAuthProvider implements AuthenticationProvider {
    
    private final static String KRB_OID_TAG = "1.2.840.113554.1.2.2";
    private final static String SPNEGO_OID_TAG = "1.3.6.1.5.5.2";

    private final static Oid KRB_OID;
    private final static Oid SPNEGO_OID;

    static {
        try {
            KRB_OID = new Oid(KRB_OID_TAG);
            SPNEGO_OID = new Oid(SPNEGO_OID_TAG);
        } catch (GSSException e) {
            throw new RuntimeException("Failed to initialize OIDs", e);
        }
    }

    private final KerberosConfig kerberosConfig;
    private final SubjectManager subjectManager;

    
    public DelegationAuthProvider(KerberosConfig kerberosConfig, SubjectManager subjectManager) {
        this.kerberosConfig = kerberosConfig;
        this.subjectManager = subjectManager;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        
        try {
            var authToken = (DelegationAuthToken) authentication;

            var userContext = authenticateUser(authToken.getUserToken());
            var delegatedToken = createDelegationCredentials(userContext);

            authToken.setDelegationCredential(delegatedToken);
            if (authToken.isAuthenticated()) {
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

            userContext.dispose();
            
            return authToken;
        } catch (Exception e) {
            log.warn("Authentication failed.", e);
            throw new BadCredentialsException(e.getMessage());
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return DelegationAuthToken.class.isAssignableFrom(authentication);
    }

    /**
     * authenticate and validate the user ticket from the request header.
     * this has to happen every request.
     *
     * @param userTicket user ticket from the authorization header
     * @return user context that can be used to create the delegated credential
     */
    private GSSContext authenticateUser(byte[] userTicket) {
        try {
            return subjectManager.doAsService(() -> {
                GSSManager manager = GSSManager.getInstance();

                GSSCredential serviceCredential = manager.createCredential(
                        null,
                        GSSCredential.DEFAULT_LIFETIME,
                        new Oid[]{SPNEGO_OID, KRB_OID},
                        GSSCredential.ACCEPT_ONLY);
                GSSContext userContext = manager.createContext(serviceCredential);
                userContext.acceptSecContext(userTicket, 0, userTicket.length);

                if (!userContext.isEstablished()) {
                    throw new SecurityException("Could not establish context with user");
                }

                serviceCredential.dispose();
                return userContext;
            });
        } catch (LoginException e) {
            log.error("Service login failed", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * create the credential with that you can create multiple spnego/tgs tickets for
     * backend services. this can be cached, but you have to validate the userContext
     * before on every request.
     *
     * @param userContext context represents the authenticated and validated user session
     * @return credential that can be used to create backend service tokens
     */
    private GSSCredential createDelegationCredentials(GSSContext userContext) {

        try {
            GSSCredential impersonationCred = userContext.getDelegCred();

            if(impersonationCred != null) {
                return impersonationCred;
            }
        } catch (Exception e) {
            // ignore
        }

        try {
            return subjectManager.doAsService(() -> {
                var manager = GSSManager.getInstance();
                Oid krbOid = new Oid(KRB_OID_TAG);
    
                GSSCredential serviceCred = manager.createCredential(
                        manager.createName(kerberosConfig.getServicePrincipal(), GSSName.NT_USER_NAME),
                        GSSCredential.INDEFINITE_LIFETIME,
                        krbOid,
                        GSSCredential.INITIATE_ONLY
                );
    
                return ((ExtendedGSSCredential) serviceCred).impersonate(userContext.getSrcName());
            });
        } catch (LoginException e) {
            log.error("Service login failed", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a backend service token using the delegated credentials.
     *
     * @param delegatedCred credential object created with createDelegationCredentials
     * @param targetSpn the SPN of the backend service
     * @return a base64 encoded SPNEGO token for the backend service
     * @throws GSSException if the kdc is misconfigured or the SPN is invalid
     */
    public static String createBackendToken(
            GSSCredential delegatedCred,
            String targetSpn
    ) throws GSSException {
        var spn = targetSpn.replace("/", "@");

        GSSManager manager = GSSManager.getInstance();
        GSSName backendServiceName = manager.createName(spn, GSSName.NT_HOSTBASED_SERVICE);
        GSSContext backendContext = manager.createContext(
                backendServiceName,
                KRB_OID,
                delegatedCred,
                GSSContext.DEFAULT_LIFETIME
        );

        backendContext.requestMutualAuth(true);
        backendContext.requestCredDeleg(false);

        byte[] outToken = backendContext.initSecContext(new byte[0], 0, 0);

        backendContext.dispose();
        return "Negotiate " + Base64.getEncoder().encodeToString(outToken);
    }

}
