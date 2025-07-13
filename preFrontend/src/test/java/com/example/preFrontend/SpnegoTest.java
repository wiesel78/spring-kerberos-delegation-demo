package com.example.preFrontend;

import com.kerb4j.client.SpnegoClient;
import org.ietf.jgss.GSSException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.URI;
import java.security.PrivilegedActionException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class SpnegoTest {

    @LocalServerPort
    private int port;

    @Test
    public void testSpnego() {
        var client = SpnegoClient.loginWithUsernamePassword("user1@IAM.DEV", "P@ssw0rd123");
        try(var context = client.createContext(URI.create("http://frontend.iam.dev").toURL())){

            var spnegoHeader = context.createTokenAsAuthroizationHeader();

            var response = WebClient.create("http://localhost:" + port)
                    .get()
                    .uri("/api/v1/todos")
                    .header("Authorization", spnegoHeader)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnError(e -> System.out.println("Error: " + e.getMessage()))
                    .block();

            System.out.println(response);
        } catch (PrivilegedActionException e) {
            System.out.println("Error creating SPNEGO context: " + e.getMessage());
        } catch (GSSException | IOException e) {
            throw new RuntimeException(e);
        }

    }

}
