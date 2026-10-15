package com.example.githubsearcher.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

/** Subset of the GitHub "search repositories" response that we care about. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubSearchResponse(
        @JsonProperty("total_count") long totalCount,
        List<Item> items) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            Long id,
            String name,
            @JsonProperty("full_name") String fullName,
            String description,
            Owner owner,
            String language,
            @JsonProperty("stargazers_count") Integer stars,
            @JsonProperty("forks_count") Integer forks,
            @JsonProperty("watchers_count") Integer watchers,
            @JsonProperty("open_issues_count") Integer openIssues,
            @JsonProperty("html_url") String htmlUrl,
            @JsonProperty("created_at") Instant createdAt,
            @JsonProperty("updated_at") Instant updatedAt,
            @JsonProperty("pushed_at") Instant pushedAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Owner(String login) {
    }
}
