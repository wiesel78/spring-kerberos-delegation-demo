package com.example.backend;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/todos")
public class TodoController {
    private final ArrayList<TodoDto> todos = new ArrayList<>();

    public TodoController() {
        // Sample data
        todos.add(TodoDto.builder()
                .id(UUID.randomUUID())
                .title("Sample Todo 1")
                .description("This is a sample todo item.")
                .done(false)
                .createdAt("2023-10-01T12:00:00Z")
                .build());
        todos.add(TodoDto.builder()
                .id(UUID.randomUUID())
                .title("Sample Todo 2")
                .description("This is another sample todo item.")
                .done(true)
                .createdAt("2023-10-02T12:00:00Z")
                .build());
    }

    @GetMapping
    public ResponseEntity<List<TodoDto>> getAllTodos() {
        return ResponseEntity.ok(todos);
    }

    @PostMapping
    public ResponseEntity<TodoDto> createTodo(@RequestBody TodoDto dto) {

        if (dto == null || dto.getTitle() == null || dto.getDescription() == null) {
            return ResponseEntity.badRequest().build();
        }

        dto.setId(UUID.randomUUID());
        dto.setCreatedAt(new Date().toString());
        todos.add(dto);

        var location = URI.create("/api/v1/todos/" + dto.getId());
        return ResponseEntity.created(location).body(dto);
    }
}
