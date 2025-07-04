package com.example.frontend;


import org.ietf.jgss.GSSException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/todos")
public class TodoController {

    private final WebClient.Builder clientBuilder;

    @Value("${backend.spn}")
    private String backendSpn;

    public TodoController(WebClient.Builder clientBuilder) {
        this.clientBuilder = clientBuilder;
    }

    @GetMapping
    public ResponseEntity<List<TodoDto>> getAllTodos() throws GSSException {
        var webClient = getWebClient();
        var response = webClient
                .get()
                .uri("/api/v1/todos")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(TodoDto[].class)
                .block();

        if (response == null) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        var todosList = List.of(response);

        return ResponseEntity.ok(todosList);
    }

    @PostMapping
    public ResponseEntity<TodoDto> createTodo(@RequestBody TodoDto dto) throws GSSException {

        var webClient = getWebClient();
        var response = webClient
                .post()
                .uri("/api/v1/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(TodoDto.class)
                .block();

        if (response == null) {
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.created(URI.create("/api/v1/todos/" + response.getId()))
                .body(response);
    }

    private WebClient getWebClient() throws GSSException {
        var auth = (DelegationAuthToken)SecurityContextHolder.getContext().getAuthentication();
        var authHeader = DelegationFilter.createBackendToken(auth.getDelegationCredential(), backendSpn);
        return clientBuilder
                .defaultHeader(HttpHeaders.AUTHORIZATION, authHeader)
                .build();
    }
}
