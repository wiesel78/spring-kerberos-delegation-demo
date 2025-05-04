package com.example.backend;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TodoDto {
    private UUID id;
    private String title;
    private String description;
    private boolean done;
    private String createdAt;
}
