package com.example.frontend;

import lombok.Getter;
import org.ietf.jgss.GSSCredential;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Base64;

public class DelegationAuthToken extends AbstractAuthenticationToken {

    /**
     * the spnego token with the "Negotiate " prefix you can find in the
     * Authorization header of the request
     */
    private final String authHeader;

    /**
     * with that credential you can create a new SPNEGO context
     * to the backend service
     */
    @Getter
    private GSSCredential delegationCredential;

    public DelegationAuthToken(String authHeader) {
        super(null);
        this.authHeader = authHeader;
    }

    /**
     * Returns the SPNEGO token without the "Negotiate " prefix.
     *
     * @return the SPNEGO token as a byte array
     */
    public byte[] getUserToken(){
        return Base64.getDecoder().decode(authHeader.substring("Negotiate ".length()));
    }

    public void setDelegationCredential(GSSCredential delegationCredential) {
        this.delegationCredential = delegationCredential;
        super.setAuthenticated(this.delegationCredential != null);
    }


    @Override
    public Object getCredentials() {
        return delegationCredential;
    }

    @Override
    public Object getPrincipal() {
        try {
            return delegationCredential.getName();
        } catch (Exception e) {
            return "";
        }
    }
}
