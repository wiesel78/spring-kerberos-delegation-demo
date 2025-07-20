package com.example.preFrontend.security;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class DelegationCredentialCacheTest {
    private DelegationCredentialCache cache;
    
    @BeforeEach
    void setUp() {
        cache = new DelegationCredentialCache();
        cache.clear();
    }

    @Test
    void isValid_null_false() {
        assertFalse(cache.isValid(null));
    }
    
    @Test
    void isValid_notValid_false() throws GSSException {
        var credential = mock(GSSCredential.class);
        
        doReturn(0).when(credential).getRemainingLifetime();
        
        assertFalse(cache.isValid(credential));
    }
    
    @Test
    void isValid_valid_true() throws GSSException {
        var credential = mock(GSSCredential.class);

        doReturn(1).when(credential).getRemainingLifetime();

        assertTrue(cache.isValid(credential));
    }

    @Test
    void clear_emptyMap_nothingHappens() {
        assertDoesNotThrow(() -> cache.clear());
    }
    
    @Test
    void clear_someTicketsInMap_emptyMap() {
        cache.getOrCreate("1", s -> mock(GSSCredential.class));
        cache.getOrCreate("2", s -> mock(GSSCredential.class));
        
        assertDoesNotThrow(() -> cache.clear());
        assertNull(cache.getOrCreate("1", s -> null));
        assertNull(cache.getOrCreate("2", s -> null));
    }
    
    @Test
    void remove_notExistingTicket_nothingHappens() {
        assertDoesNotThrow(() -> cache.remove("1"));
    }

    @Test
    void remove_existingTicket_nothingHappens() {
        cache.getOrCreate("1", s -> mock(GSSCredential.class));
        
        assertDoesNotThrow(() -> cache.remove("1"));
        assertNull(cache.getOrCreate("1", s -> null));
    }
    
    @Test
    void getOrCreate_notInMap_newOneWasCreated() throws GSSException {
        var ticket = mock(GSSCredential.class);
        
        doReturn(1).when(ticket).getRemainingLifetime();
        
        cache.getOrCreate("1", s -> ticket);
        
        assertSame(ticket, cache.getOrCreate("1", s -> null));
    }

    @Test
    void getOrCreate_inMapButInvalid_newOneWasCreated() throws GSSException {
        var invalidTicket = mock(GSSCredential.class);
        var validTicket = mock(GSSCredential.class);

        doReturn(0).when(invalidTicket).getRemainingLifetime();
        doReturn(1).when(validTicket).getRemainingLifetime();

        cache.getOrCreate("1", s -> invalidTicket);

        assertSame(validTicket, cache.getOrCreate("1", s -> validTicket));
    }
    
    @Test
    void getOrCreate_getTwoTimeTheSameTicket_getTheCachedTicketAtSecondTime() throws GSSException {
        var ticket = mock(GSSCredential.class);

        doReturn(1).when(ticket).getRemainingLifetime();

        cache.getOrCreate("1", s -> ticket);

        assertSame(ticket, cache.getOrCreate("1", s -> null));
    }
}