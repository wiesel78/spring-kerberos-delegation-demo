package com.example.frontend;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/todos")
public class TodoController {

    private final WebClient.Builder clientBuilder;

    public TodoController(WebClient.Builder clientBuilder) {
        this.clientBuilder = clientBuilder;
    }

    @GetMapping
    public ResponseEntity<List<TodoDto>> getAllTodos() {
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
    public ResponseEntity<TodoDto> createTodo(@RequestBody TodoDto dto) {

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

    private WebClient getWebClient() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var authHeader = (String)auth.getDetails();
        return clientBuilder
                .defaultHeader(HttpHeaders.AUTHORIZATION, authHeader)
                .build();
    }
}
