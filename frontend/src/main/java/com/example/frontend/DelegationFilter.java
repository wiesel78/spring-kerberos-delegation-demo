package com.example.frontend;

import com.sun.security.jgss.ExtendedGSSCredential;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ietf.jgss.*;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.Base64;
import java.util.Map;

public class DelegationFilter extends OncePerRequestFilter {

    private final static String KRB_OID_TAG = "1.2.840.113554.1.2.2";
    private final static String SPNEGO_OID_TAG = "1.3.6.1.5.5.2";

    private final String servicePrincipal;
    private final String targetSpn;

    private final Configuration jaasConfig;

    public DelegationFilter(String servicePrincipal, String keytabPath, String targetSpn, String krb5ConfigPath) {

        this.servicePrincipal = servicePrincipal;
        this.targetSpn = targetSpn;

        setSystemProperties(krb5ConfigPath, keytabPath);
        this.jaasConfig = getJaasConfig(keytabPath, this.servicePrincipal);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {


        var authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (request.getServletPath().startsWith("/unauth")) {
            // Allow unauthenticated access to the blub endpoint
            filterChain.doFilter(request, response);
            return;
        }

        // if no auth header is present, initiate the negotiate authentication
        if (authHeader == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Negotiate");
            return;
        }

        if (!authHeader.startsWith("Negotiate ")) {
            throw new AuthenticationCredentialsNotFoundException("Negotiate header is incorrect");
        }

        try {
            processKerberosRequest(authHeader, targetSpn.replace("/", "@"));

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private Subject loginAsService() throws LoginException {
        LoginContext lc = new LoginContext("ServiceLogin", null, null, this.jaasConfig);
        lc.login();
        return lc.getSubject();
    }

    private byte[] extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Negotiate ")) {
            return Base64.getDecoder().decode(authHeader.substring("Negotiate ".length()));
        }
        throw new IllegalArgumentException("Invalid Authorization Header");
    }

    private GSSCredential performS4U2Proxy(GSSManager manager, GSSName userName, Oid krbOid) throws GSSException {
        GSSCredential serviceCred = manager.createCredential(
                manager.createName(servicePrincipal, GSSName.NT_USER_NAME),
                GSSCredential.INDEFINITE_LIFETIME,
                krbOid,
                GSSCredential.INITIATE_ONLY
        );

        GSSCredential s4u2selfCred = ((ExtendedGSSCredential) serviceCred).impersonate(userName);

        System.out.println("Successfully performed S4U2Self for: " + userName);

        return s4u2selfCred;
    }

    void processKerberosRequest(String authHeader, String targetSpn) throws Exception {
        byte[] userTicket = extractTokenFromHeader(authHeader);
        Subject serviceSubject = loginAsService();

        Subject.doAs(serviceSubject, (PrivilegedExceptionAction<Subject>) () -> {
            Oid krbOid = new Oid(KRB_OID_TAG);
            Oid spnegoOid = new Oid(SPNEGO_OID_TAG);

            GSSManager manager = GSSManager.getInstance();

            GSSCredential serviceCredential = manager.createCredential(
                    null,
                    GSSCredential.DEFAULT_LIFETIME,
                    new Oid[]{spnegoOid, krbOid},
                    GSSCredential.ACCEPT_ONLY);
            GSSContext userContext = manager.createContext(serviceCredential);
            userContext.acceptSecContext(userTicket, 0, userTicket.length);

            if (userContext.isEstablished()) {
                System.out.println("Authenticated user: " + userContext.getSrcName());

                // constrained delegation or S4U2Proxy
                GSSCredential impersonationCred = userContext.getDelegCred();

                if (impersonationCred == null) {
                    // do it explicitly
                    impersonationCred = performS4U2Proxy(manager, userContext.getSrcName(), krbOid);
                }

                createDelegatedServiceTicket(manager, impersonationCred, targetSpn, krbOid);
            } else {
                throw new SecurityException("Could not establish context with user");
            }

            userContext.dispose();
            serviceCredential.dispose();
            return null;
        });
    }

    private void createDelegatedServiceTicket(
            GSSManager manager,
            GSSCredential delegatedCred,
            String targetSpn,
            Oid krbOid
    ) throws GSSException {

        GSSName backendServiceName = manager.createName(targetSpn, GSSName.NT_HOSTBASED_SERVICE);
        GSSContext backendContext = manager.createContext(
                backendServiceName,
                krbOid,
                delegatedCred,
                GSSContext.DEFAULT_LIFETIME
        );

        backendContext.requestMutualAuth(true);
        backendContext.requestCredDeleg(false);

        byte[] outToken = backendContext.initSecContext(new byte[0], 0, 0);

        if (outToken != null) {

            // save the token in the details field for later use
            String negotiateHeader = "Negotiate " + Base64.getEncoder().encodeToString(outToken);
            var authorities = AuthorityUtils.createAuthorityList("ROLE_USER");
            var auth = new UsernamePasswordAuthenticationToken(delegatedCred.getName(), delegatedCred, authorities);
            auth.setDetails(negotiateHeader);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        backendContext.dispose();
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
