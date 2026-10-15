package com.example.githubsearcher.controller;

import com.example.githubsearcher.dto.RepositoryDto;
import com.example.githubsearcher.dto.SearchRequest;
import com.example.githubsearcher.exception.GithubApiException;
import com.example.githubsearcher.service.GithubRepositoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GithubController.class)
class GithubControllerTest {

    private static final String SEARCH_URL = "/api/github/search";
    private static final String REPOS_URL = "/api/github/repositories";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GithubRepositoryService service;

    private RepositoryDto dto(long id, String name, int stars) {
        return new RepositoryDto(id, name, "user/" + name, "An example repository", "user", "Java",
                stars, 120, 5, 2, "https://github.com/user/" + name,
                Instant.parse("2020-01-01T00:00:00Z"),
                Instant.parse("2024-01-01T12:00:00Z"),
                Instant.parse("2024-01-02T00:00:00Z"));
    }

    // ---------- POST /api/github/search ----------
    @Test
    void search_returnsRepositoriesAndSuccessMessage() throws Exception {
        when(service.searchAndSave(any(SearchRequest.class)))
                .thenReturn(List.of(dto(123456L, "spring-boot-example", 450)));

        mockMvc.perform(post(SEARCH_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"springboot\",\"language\":\"Java\",\"sort\":\"stars\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Repositories fetched and saved successfully"))
                .andExpect(jsonPath("$.repositories[0].id").value(123456))
                .andExpect(jsonPath("$.repositories[0].name").value("spring-boot-example"))
                .andExpect(jsonPath("$.repositories[0].owner").value("user"))
                .andExpect(jsonPath("$.repositories[0].language").value("Java"))
                .andExpect(jsonPath("$.repositories[0].stars").value(450))
                .andExpect(jsonPath("$.repositories[0].forks").value(120))
                .andExpect(jsonPath("$.repositories[0].lastUpdated").value("2024-01-01T12:00:00Z"));
    }

    @Test
    void search_passesRequestFieldsToService() throws Exception {
        when(service.searchAndSave(any(SearchRequest.class))).thenReturn(List.of());

        mockMvc.perform(post(SEARCH_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"spring\",\"language\":\"Java\",\"sort\":\"forks\",\"perPage\":10}"));

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(service).searchAndSave(captor.capture());
        assertThat(captor.getValue().query()).isEqualTo("spring");
        assertThat(captor.getValue().language()).isEqualTo("Java");
        assertThat(captor.getValue().sort()).isEqualTo("forks");
        assertThat(captor.getValue().perPage()).isEqualTo(10);
    }

    @Test
    void search_emptyResultReturns200WithEmptyListAndMessage() throws Exception {
        when(service.searchAndSave(any(SearchRequest.class))).thenReturn(List.of());

        mockMvc.perform(post(SEARCH_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"zzzzzzzz\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("No repositories found for the given search criteria"))
                .andExpect(jsonPath("$.repositories").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"query\":\"\"}", "{\"query\":\"   \"}"})
    void search_blankOrMissingQueryReturns400(String body) throws Exception {
        mockMvc.perform(post(SEARCH_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("query must not be blank")));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 101})
    void search_perPageOutOfRangeReturns400(int perPage) throws Exception {
        mockMvc.perform(post(SEARCH_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"spring\",\"perPage\":" + perPage + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("perPage")));
    }

    @Test
    void search_malformedJsonReturns400() throws Exception {
        mockMvc.perform(post(SEARCH_URL).contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("not valid JSON")));
    }

    @Test
    void search_invalidSortReturns400() throws Exception {
        when(service.searchAndSave(any(SearchRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid sort value 'abc'. Allowed values: stars, forks, updated"));

        mockMvc.perform(post(SEARCH_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"spring\",\"sort\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Invalid sort")));
    }

    @Test
    void search_githubRateLimitReturns429() throws Exception {
        when(service.searchAndSave(any(SearchRequest.class)))
                .thenThrow(new GithubApiException(HttpStatus.TOO_MANY_REQUESTS, "GitHub API rate limit exceeded."));

        mockMvc.perform(post(SEARCH_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"spring\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").value(containsString("rate limit")));
    }

    @Test
    void search_githubDownReturns503() throws Exception {
        when(service.searchAndSave(any(SearchRequest.class)))
                .thenThrow(new GithubApiException(HttpStatus.SERVICE_UNAVAILABLE, "Could not reach the GitHub API."));

        mockMvc.perform(post(SEARCH_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"spring\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    // ---------- GET /api/github/repositories ----------
    @Test
    void repositories_usesDefaultsWhenNoParamsGiven() throws Exception {
        when(service.findStored(null, null, "stars"))
                .thenReturn(List.of(dto(1L, "a", 900), dto(2L, "b", 500)));

        mockMvc.perform(get(REPOS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositories.length()").value(2))
                .andExpect(jsonPath("$.repositories[0].name").value("a"));

        verify(service).findStored(null, null, "stars");
    }

    @Test
    void repositories_passesAllFiltersToService() throws Exception {
        when(service.findStored("Java", 100, "forks")).thenReturn(List.of(dto(1L, "a", 900)));

        mockMvc.perform(get(REPOS_URL)
                .param("language", "Java")
                .param("minStars", "100")
                .param("sort", "forks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositories[0].stars").value(900));

        verify(service).findStored("Java", 100, "forks");
    }

    @Test
    void repositories_emptyResultReturns200WithEmptyList() throws Exception {
        when(service.findStored(any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get(REPOS_URL).param("language", "Cobol"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositories").isEmpty());
    }

    @Test
    void repositories_invalidSortReturns400() throws Exception {
        when(service.findStored(any(), any(), eq("abc")))
                .thenThrow(new IllegalArgumentException("Invalid sort value 'abc'. Allowed values: stars, forks, updated"));

        mockMvc.perform(get(REPOS_URL).param("sort", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Invalid sort")));
    }

    @Test
    void repositories_negativeMinStarsReturns400() throws Exception {
        mockMvc.perform(get(REPOS_URL).param("minStars", "-5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void repositories_nonNumericMinStarsReturns400() throws Exception {
        mockMvc.perform(get(REPOS_URL).param("minStars", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("minStars")));
    }

    // ---------- general error behaviour ----------
    @Test
    void wrongHttpMethodReturns405() throws Exception {
        mockMvc.perform(post(REPOS_URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void unknownUrlReturns404() throws Exception {
        mockMvc.perform(get("/api/github/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unexpectedErrorReturns500WithoutLeakingDetails() throws Exception {
        when(service.findStored(any(), any(), any())).thenThrow(new RuntimeException("db password is secret"));

        mockMvc.perform(get(REPOS_URL))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Something went wrong on our side"));
    }
}
