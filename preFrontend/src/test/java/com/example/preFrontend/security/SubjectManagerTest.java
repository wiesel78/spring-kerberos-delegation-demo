package com.example.preFrontend.security;

import com.example.preFrontend.configs.KerberosConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubjectManagerTest {

    private SubjectManager manager;

    @BeforeEach
    void setUp() {
        KerberosConfig kerberosConfig = new KerberosConfig();
        kerberosConfig.setServicePrincipal("HTTP/frontend.iam.dev");
        kerberosConfig.setKeytab("src/main/resources/kerberos/krb5.keytab");
        kerberosConfig.setKrb5ConfigPath("src/main/resources/kerberos/krb5.conf");
        
        manager = new SubjectManager(kerberosConfig);
    }
    
    @Test
    void isValid_null_false() {
        assertFalse(manager.isValid(null));
    }
    
    @Test
    void isValid_noPrivateKey_false() {
        var subject = mock(Subject.class);
        
        when(subject.getPrivateCredentials(KerberosTicket.class))
                .thenReturn(Collections.emptySet());
        
        assertFalse(manager.isValid(subject));
    }
    
    @Test
    void isValid_expiredTicket_false() {
        var subject = mock(Subject.class);
        var expiredTicket = mock(KerberosTicket.class);
        
        when(expiredTicket.isCurrent()).thenReturn(false);
        when(subject.getPrivateCredentials(KerberosTicket.class))
                .thenReturn(Set.of(expiredTicket));

        assertFalse(manager.isValid(subject));
    }
    
    @Test
    void isValid_atLeastOnValidTicket_true() {
        var subject = mock(Subject.class);
        var expiredTicket = mock(KerberosTicket.class);
        var validTicket = mock(KerberosTicket.class);

        when(expiredTicket.isCurrent()).thenReturn(false);
        when(validTicket.isCurrent()).thenReturn(true);
        when(subject.getPrivateCredentials(KerberosTicket.class))
                .thenReturn(Set.of(expiredTicket, validTicket));

        assertTrue(manager.isValid(subject));
    }
    
    @Test
    void loginAsService_loginContextFailed_throwLoginException() throws LoginException {
        var managerSpy = spy(manager);
        
        doThrow(LoginException.class).when(managerSpy).createLoginContext();
        
        assertThrows(LoginException.class, managerSpy::loginAsService);
    }
    
    @Test
    void loginAsService_loginFailed_throwLoginException() throws LoginException {
        var managerSpy = spy(manager);
        var loginContext = mock(LoginContext.class);
        
        doThrow(LoginException.class).when(loginContext).login();
        doReturn(loginContext).when(managerSpy).createLoginContext();

        assertThrows(LoginException.class, managerSpy::loginAsService);
    }

    @Test
    void getOrCreate_subjectNotExist_returnNewValidSubject() throws LoginException {
        var managerSpy = spy(manager);
        var subject = mock(Subject.class);
        
        doReturn(subject).when(managerSpy).loginAsService();
        
        var result = managerSpy.getOrCreate();
        
        assertSame(subject, result);
        verify(managerSpy, times(2)).isValid(null);
        verify(managerSpy, times(1)).loginAsService();
    }

    @Test
    void getOrCreate_subjectExpired_returnNewValidSubject() throws LoginException {
        var managerSpy = spy(manager);
        var subject = mock(Subject.class);
        var expiredTicket = mock(KerberosTicket.class);

        when(expiredTicket.isCurrent()).thenReturn(false);
        when(subject.getPrivateCredentials(KerberosTicket.class))
                .thenReturn(Set.of(expiredTicket));
        doReturn(subject).when(managerSpy).loginAsService();

        var result = managerSpy.getOrCreate();

        assertSame(subject, result);
        verify(managerSpy, times(2)).isValid(null);
        verify(managerSpy, times(1)).loginAsService();
    }
    
    @Test
    void getOrCreate_subjectValid_returnExistingSubject() throws LoginException {
        var managerSpy = spy(manager);
        var subject = mock(Subject.class);
        var validTicket = mock(KerberosTicket.class);

        when(validTicket.isCurrent()).thenReturn(true);
        when(subject.getPrivateCredentials(KerberosTicket.class))
                .thenReturn(Set.of(validTicket));
        doReturn(subject).when(managerSpy).loginAsService();

        // create the session
        managerSpy.getOrCreate();
        
        // get the existing subject
        var result = managerSpy.getOrCreate();

        assertSame(subject, result);
        verify(managerSpy, times(2)).isValid(null);
        verify(managerSpy, times(1)).isValid(subject);
        verify(managerSpy, times(1)).loginAsService();
    }

    @Test
    void doAsService_throwRuntimeException_notRunActionTwice() throws LoginException {
        var managerSpy = spy(manager);
        var subject = mock(Subject.class);
        
        doReturn(subject).when(managerSpy).loginAsService();
        doReturn(subject).when(managerSpy).getOrCreate();

        assertThrows(RuntimeException.class, () -> 
                managerSpy.doAsService(() -> { throw new RuntimeException("error"); }));

        verify(managerSpy, times(1)).getOrCreate();
        verify(managerSpy, times(0)).loginAsService();
    }

    @Test
    void doAsService_throwLoginException_runActionTwice() throws LoginException {
        var managerSpy = spy(manager);
        var subject = mock(Subject.class);
        var kerberosExpiredException = new LoginException("Kerberos ticket expired");
        Callable<Integer> action = () -> 42;

        doReturn(subject).when(managerSpy).loginAsService();
        doReturn(subject).when(managerSpy).getOrCreate();
        doReturn(true).when(managerSpy).isKerberosExpiredException(kerberosExpiredException);
        
        try(MockedStatic<Subject> staticSubject = mockStatic(Subject.class)) {
            AtomicInteger callCount = new AtomicInteger();

            staticSubject.when(() -> Subject.callAs(any(), eq(action)))
                .thenAnswer(invocation -> {
                    if (callCount.getAndIncrement() == 0) {
                        throw kerberosExpiredException;
                    } else {
                        return 42;
                    }
                });
            var result = managerSpy.doAsService(action);
            assertEquals(42, result.intValue());
            staticSubject.verify(() -> Subject.callAs(any(), eq(action)), times(2));
            verify(managerSpy, times(1)).getOrCreate();
            verify(managerSpy, times(1)).loginAsService();
        }
    }
    
    @Test
    void doAsService_validTicket_runActionOnce() throws LoginException {
        var managerSpy = spy(manager);
        var subject = mock(Subject.class);
        Callable<Integer> action = () -> 42;

        doReturn(subject).when(managerSpy).getOrCreate();

        try(MockedStatic<Subject> staticSubject = mockStatic(Subject.class)) {
            staticSubject.when(() -> Subject.callAs(any(), eq(action))).thenReturn(42);
            
            var result = managerSpy.doAsService(action);
            
            assertEquals(42, result.intValue());
            staticSubject.verify(() -> Subject.callAs(any(), eq(action)), times(1));
        }
    }
}