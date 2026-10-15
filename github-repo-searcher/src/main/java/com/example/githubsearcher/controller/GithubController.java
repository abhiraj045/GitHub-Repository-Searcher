package com.example.githubsearcher.controller;

import com.example.githubsearcher.dto.RepositoriesResponse;
import com.example.githubsearcher.dto.RepositoryDto;
import com.example.githubsearcher.dto.SearchRequest;
import com.example.githubsearcher.dto.SearchResponse;
import com.example.githubsearcher.service.GithubRepositoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "GitHub Repositories")
@RequestMapping("/api/github")
@Validated
public class GithubController {

    private final GithubRepositoryService service;

    public GithubController(GithubRepositoryService service) {
        this.service = service;
    }

    @Operation(summary = "Search GitHub and save the results",
            description = "Calls the GitHub search API, then inserts new repositories and updates existing ones.")

    @PostMapping("/search")
    public SearchResponse search(@Valid @RequestBody SearchRequest request) {
        List<RepositoryDto> repos = service.searchAndSave(request);
        String message = repos.isEmpty()
                ? "No repositories found for the given search criteria"
                : "Repositories fetched and saved successfully";
        return new SearchResponse(message, repos);
    }

    @Operation(summary = "Get stored repositories",
            description = "Optional filters: language, minStars. Sort by stars (default), forks or updated.")

    @GetMapping("/repositories")
    public RepositoriesResponse repositories(
            @RequestParam(required = false) String language,
            @RequestParam(required = false) @Min(value = 0, message = "minStars must be 0 or greater") Integer minStars,
            @RequestParam(defaultValue = "stars") String sort) {
        return new RepositoriesResponse(service.findStored(language, minStars, sort));
    }
}
