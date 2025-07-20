package com.example.preFrontend.security;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * after the incoming spnego token was validated, it has to be
 * done at every request once, the delegated credentials can be 
 * created, but it is not necessary to create the ticket every time,
 * it can be cached. this is this cache is for.
 */
@Component
public class DelegationCredentialCache {
    private final ConcurrentMap<String, GSSCredential> tickets;

    public DelegationCredentialCache() {
        this.tickets = new ConcurrentHashMap<>();
    }

    /**
     * clear the whole ticket map and remove all
     * containing tickets.
     */
    public void clear() {
        tickets.clear();
    }

    /**
     * remove the item under the given key from the
     * ticket map.
     * 
     * @param key map key for a specific ticket. supposed to be the principal name.
     */
    public void remove(String key) {
        tickets.remove(key);
    }

    /**
     * check for credentials expiration date.
     * 
     * @param credential delegated gss credential
     * @return true, if ticket exists and is not expired, false, otherwise
     */
    public boolean isValid(GSSCredential credential) {
        if(credential == null){
            return false;
        }
        
        try {
            return credential.getRemainingLifetime() > 0;
        } catch (GSSException e) {
            return false;
        }
    }

    /**
     * get a existing and not expired ticket or create a new one with the 
     * given create method.
     * 
     * @param key key for the related ticket in the tickets map
     * @param action creator method to create a new delegated credential
     * @return cached ticket, if it exist and is not expired, new ticket, otherwise
     */
    public GSSCredential getOrCreate(String key, Function<String, GSSCredential> action) {
        var ticket = tickets.get(key);
        if(isValid(ticket)) {
            return ticket;
        }
        
        return tickets.compute(key, (name, gssCredential) -> {
            if(isValid(gssCredential)) {
                return gssCredential;
            }
            
            return action.apply(name);
        });
    }
}
