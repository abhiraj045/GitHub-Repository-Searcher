package com.example.githubsearcher.dto;

import java.util.List;

public record SearchResponse(String message, List<RepositoryDto> repositories) {
}
