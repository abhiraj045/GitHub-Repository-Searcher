package com.example.githubsearcher.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SearchRequest(
        @NotBlank(message = "query must not be blank") String query,
        String language,
        String sort,
        @Min(value = 1, message = "perPage must be between 1 and 100")
        @Max(value = 100, message = "perPage must be between 1 and 100") Integer perPage) {
}
