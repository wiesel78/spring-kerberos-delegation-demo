package com.example.frontend;

import com.sun.security.jgss.ExtendedGSSCredential;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ietf.jgss.*;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

public class DelegationFilter extends OncePerRequestFilter {

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

    private final String servicePrincipal;
    private final Configuration jaasConfig;

    public DelegationFilter(String servicePrincipal, String keytabPath, String krb5ConfigPath) {

        this.servicePrincipal = servicePrincipal;

        setSystemProperties(krb5ConfigPath, keytabPath);
        this.jaasConfig = getJaasConfig(keytabPath, this.servicePrincipal);
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
            var serviceSubject = loginAsService();
            var authToken = new DelegationAuthToken(authHeader);
            var userContext = authenticateUser(serviceSubject, authToken.getUserToken());
            var delegatedToken = createDelegationCredentials(serviceSubject, userContext);

            authToken.setDelegationCredential(delegatedToken);

            if (authToken.isAuthenticated()) {
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

            userContext.dispose();

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Logs in as the service principal using JAAS.
     *
     * @return the Subject representing the service principal
     * @throws LoginException if the keytab or principal is invalid
     */
    private Subject loginAsService() throws LoginException {
        LoginContext lc = new LoginContext("ServiceLogin", null, null, this.jaasConfig);
        lc.login();
        return lc.getSubject();
    }

    /**
     * authenticate and validate the user ticket from the request header.
     * this has to happen every request.
     *
     * @param serviceSubject the subject created with loginAsService
     * @param userTicket user ticket from the authorization header
     * @return user context that can be used to create the delegated credential
     */
    private GSSContext authenticateUser(Subject serviceSubject, byte[] userTicket) {
        return Subject.callAs(serviceSubject, () -> {
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
    }

    /**
     * create the credential with that you can create multiple spnego/tgs tickets for
     * backend services. this can be cached, but you have to validate the userContext
     * before on every request.
     *
     * @param serviceSubject the subject created with loginAsService
     * @param userContext context represents the authenticated and validated user session
     * @return credential that can be used to create backend service tokens
     * @throws GSSException if serviceSubject or userContext is invalid or kdc is misconfigured
     */
    private GSSCredential createDelegationCredentials(Subject serviceSubject, GSSContext userContext) throws GSSException {

        GSSCredential impersonationCred = userContext.getDelegCred();

        if(impersonationCred != null) {
            return impersonationCred;
        }

        return Subject.callAs(serviceSubject, () -> {
            var manager = GSSManager.getInstance();
            Oid krbOid = new Oid(KRB_OID_TAG);

            GSSCredential serviceCred = manager.createCredential(
                    manager.createName(servicePrincipal, GSSName.NT_USER_NAME),
                    GSSCredential.INDEFINITE_LIFETIME,
                    krbOid,
                    GSSCredential.INITIATE_ONLY
            );

            return ((ExtendedGSSCredential) serviceCred).impersonate(userContext.getSrcName());
        });
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
