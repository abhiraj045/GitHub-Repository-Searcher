package com.example.githubsearcher.client;

import com.example.githubsearcher.dto.GithubSearchResponse;
import com.example.githubsearcher.dto.SortField;
import com.example.githubsearcher.exception.GithubApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;

/**
 * Thin wrapper around GET https://api.github.com/search/repositories.
 */
@Component
public class GithubApiClient implements GithubSearchClient {

    private final RestClient restClient;

    public GithubApiClient(RestClient.Builder builder,
            @Value("${github.api.base-url}") String baseUrl,
            @Value("${github.api.token:}") String token) {
        RestClient.Builder b = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader(HttpHeaders.USER_AGENT, "github-repo-searcher");
        if (token != null && !token.isBlank()) {
            b.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token.trim());
        }
        this.restClient = b.build();
    }

    @Override
    public GithubSearchResponse search(String query, String language, SortField sort, int perPage) {
        String q = buildQuery(query, language);
        try {
            GithubSearchResponse body = restClient.get()
                    .uri(uri -> uri.path("/search/repositories")
                            .queryParam("q", "{q}")
                            .queryParam("sort", sort.getApiValue())
                            .queryParam("order", "desc")
                            .queryParam("per_page", perPage)
                            .build(q))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(GithubSearchResponse.class);

            if (body == null) {
                throw new GithubApiException(HttpStatus.BAD_GATEWAY, "GitHub returned an empty response");
            }
            return body;

        } catch (RestClientResponseException ex) {
            throw translate(ex);
        } catch (ResourceAccessException ex) {
            throw new GithubApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Could not reach the GitHub API. Please try again later.", ex);
        } catch (GithubApiException ex) {
            throw ex;
        } catch (Exception ex) {
            // e.g. response body that is not valid JSON / does not match the expected shape
            throw new GithubApiException(HttpStatus.BAD_GATEWAY,
                    "GitHub returned an invalid response", ex);
        }
    }

    /**
     * "springboot language:Java" - language is quoted when it contains spaces.
     */
    private String buildQuery(String query, String language) {
        StringBuilder sb = new StringBuilder(query.trim());
        if (language != null && !language.isBlank()) {
            String lang = language.trim();
            sb.append(" language:").append(lang.contains(" ") ? "\"" + lang + "\"" : lang);
        }
        return sb.toString();
    }

    private GithubApiException translate(RestClientResponseException ex) {
        int code = ex.getStatusCode().value();
        HttpHeaders headers = ex.getResponseHeaders();

        if (code == 403 || code == 429) {
            return new GithubApiException(HttpStatus.TOO_MANY_REQUESTS, rateLimitMessage(headers), ex);
        }
        if (code == 422) {
            return new GithubApiException(HttpStatus.BAD_REQUEST,
                    "GitHub rejected the search query. Check the query and language values.", ex);
        }
        if (code == 401) {
            return new GithubApiException(HttpStatus.BAD_GATEWAY,
                    "GitHub rejected the configured access token (github.api.token)", ex);
        }
        if (code == 503) {
            return new GithubApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "GitHub is temporarily unavailable. Please try again later.", ex);
        }
        return new GithubApiException(HttpStatus.BAD_GATEWAY,
                "GitHub API returned an unexpected status: " + code, ex);
    }

    private String rateLimitMessage(HttpHeaders headers) {
        String msg = "GitHub API rate limit exceeded.";
        if (headers != null) {
            String reset = headers.getFirst("X-RateLimit-Reset");
            String retryAfter = headers.getFirst("Retry-After");
            try {
                if (retryAfter != null) {
                    msg += " Retry after " + retryAfter + " seconds.";
                } else if (reset != null) {
                    msg += " Limit resets at " + Instant.ofEpochSecond(Long.parseLong(reset)) + ".";
                }
            } catch (NumberFormatException ignored) {
                // keep the generic message
            }
        }
        return msg + " Set GITHUB_TOKEN to get a higher limit.";
    }
}
