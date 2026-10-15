package com.example.githubsearcher.service;

import com.example.githubsearcher.client.GithubApiClient;
import com.example.githubsearcher.dto.GithubSearchResponse;
import com.example.githubsearcher.dto.RepositoryDto;
import com.example.githubsearcher.dto.SearchRequest;
import com.example.githubsearcher.dto.SortField;
import com.example.githubsearcher.entity.GithubRepository;
import com.example.githubsearcher.exception.GithubApiException;
import com.example.githubsearcher.repository.GithubRepositoryRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.example.githubsearcher.mapper.GithubRepositoryMapper;

@ExtendWith(MockitoExtension.class)
class GithubRepositoryServiceTest {

    @Mock
    private GithubApiClient githubClient;

    @Mock
    private GithubRepositoryRepository repository;

    @Captor
    private ArgumentCaptor<List<GithubRepository>> savedCaptor;

    @Captor
    private ArgumentCaptor<Sort> sortCaptor;

    private GithubRepositoryService service;

    @BeforeEach
    void setUp() {
        service = new GithubRepositoryService(githubClient, repository, new GithubRepositoryMapper(), 30);
    }

    // ---------- helpers ----------
    private GithubSearchResponse.Item item(Long id, String name, Integer stars) {
        return new GithubSearchResponse.Item(
                id, name, "user/" + name, "An example repository",
                new GithubSearchResponse.Owner("user"), "Java",
                stars, 120, 5, 2,
                "https://github.com/user/" + name,
                Instant.parse("2020-01-01T00:00:00Z"),
                Instant.parse("2024-01-01T12:00:00Z"),
                Instant.parse("2024-01-02T00:00:00Z"));
    }

    private GithubSearchResponse response(GithubSearchResponse.Item... items) {
        return new GithubSearchResponse(items.length, List.of(items));
    }

    private GithubRepository existingEntity(Long id, String name, int stars) {
        GithubRepository e = new GithubRepository();
        e.setId(id);
        e.setName(name);
        e.setStars(stars);
        return e;
    }

    // ---------- searchAndSave ----------
    @Test
    void searchAndSave_insertsNewRepository() {
        when(githubClient.search("springboot", "Java", SortField.STARS, 30))
                .thenReturn(response(item(1L, "spring-boot-example", 450)));
        when(repository.findAllById(List.of(1L))).thenReturn(List.of());
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<RepositoryDto> result
                = service.searchAndSave(new SearchRequest("springboot", "Java", "stars", null));

        assertThat(result).hasSize(1);
        RepositoryDto dto = result.get(0);
        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.name()).isEqualTo("spring-boot-example");
        assertThat(dto.owner()).isEqualTo("user");
        assertThat(dto.language()).isEqualTo("Java");
        assertThat(dto.stars()).isEqualTo(450);
        assertThat(dto.forks()).isEqualTo(120);
        assertThat(dto.lastUpdated()).isEqualTo(Instant.parse("2024-01-01T12:00:00Z"));
    }

    @Test
    void searchAndSave_updatesExistingRepositoryInsteadOfDuplicating() {
        GithubRepository existing = existingEntity(1L, "old-name", 10);
        when(githubClient.search("springboot", "Java", SortField.STARS, 30))
                .thenReturn(response(item(1L, "new-name", 500)));
        when(repository.findAllById(List.of(1L))).thenReturn(List.of(existing));
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        service.searchAndSave(new SearchRequest("springboot", "Java", "stars", null));

        verify(repository).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue()).hasSize(1);
        assertThat(savedCaptor.getValue().get(0)).isSameAs(existing); // same object, not a new row
        assertThat(existing.getName()).isEqualTo("new-name");
        assertThat(existing.getStars()).isEqualTo(500);
    }

    @Test
    void searchAndSave_handlesMixOfNewAndExisting() {
        GithubRepository existing = existingEntity(1L, "old", 10);
        when(githubClient.search("spring", null, SortField.STARS, 30))
                .thenReturn(response(item(1L, "old", 99), item(2L, "brand-new", 5)));
        when(repository.findAllById(List.of(1L, 2L))).thenReturn(List.of(existing));
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<RepositoryDto> result
                = service.searchAndSave(new SearchRequest("spring", null, null, null));

        assertThat(result).extracting(RepositoryDto::id).containsExactly(1L, 2L);
        verify(repository).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue()).hasSize(2);
        assertThat(savedCaptor.getValue().get(0)).isSameAs(existing);
        assertThat(existing.getStars()).isEqualTo(99);
    }

    @Test
    void searchAndSave_returnsEmptyListWhenGithubFindsNothing() {
        when(githubClient.search(any(), any(), any(), anyInt()))
                .thenReturn(new GithubSearchResponse(0, List.of()));

        List<RepositoryDto> result
                = service.searchAndSave(new SearchRequest("zzzzzz", null, null, null));

        assertThat(result).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    void searchAndSave_handlesNullItemsInGithubResponse() {
        when(githubClient.search(any(), any(), any(), anyInt()))
                .thenReturn(new GithubSearchResponse(0, null));

        List<RepositoryDto> result
                = service.searchAndSave(new SearchRequest("zzzzzz", null, null, null));

        assertThat(result).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    void searchAndSave_skipsMalformedItemsWithoutId() {
        when(githubClient.search("spring", null, SortField.STARS, 30))
                .thenReturn(response(item(null, "bad", 5), item(2L, "good", 7)));
        when(repository.findAllById(List.of(2L))).thenReturn(List.of());
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<RepositoryDto> result
                = service.searchAndSave(new SearchRequest("spring", null, null, null));

        assertThat(result).extracting(RepositoryDto::id).containsExactly(2L);
    }

    @Test
    void searchAndSave_treatsMissingCountsAsZero() {
        when(githubClient.search("spring", null, SortField.STARS, 30))
                .thenReturn(response(item(3L, "no-stars", null)));
        when(repository.findAllById(List.of(3L))).thenReturn(List.of());
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<RepositoryDto> result
                = service.searchAndSave(new SearchRequest("spring", null, null, null));

        assertThat(result.get(0).stars()).isZero();
    }

    @Test
    void searchAndSave_usesDefaultsWhenSortAndPerPageMissing() {
        when(githubClient.search("spring", null, SortField.STARS, 30))
                .thenReturn(new GithubSearchResponse(0, List.of()));

        service.searchAndSave(new SearchRequest("spring", null, null, null));

        verify(githubClient).search("spring", null, SortField.STARS, 30);
    }

    @Test
    void searchAndSave_passesSortLanguageAndPerPageToGithub() {
        when(githubClient.search("spring", "Java", SortField.FORKS, 50))
                .thenReturn(new GithubSearchResponse(0, List.of()));

        service.searchAndSave(new SearchRequest("spring", "Java", "forks", 50));

        verify(githubClient).search("spring", "Java", SortField.FORKS, 50);
    }

    @Test
    void searchAndSave_propagatesGithubErrorsAndSavesNothing() {
        when(githubClient.search(any(), any(), any(), anyInt()))
                .thenThrow(new GithubApiException(HttpStatus.TOO_MANY_REQUESTS, "rate limit exceeded"));

        assertThatThrownBy(()
                -> service.searchAndSave(new SearchRequest("spring", null, null, null)))
                .isInstanceOf(GithubApiException.class)
                .hasMessageContaining("rate limit");

        verifyNoInteractions(repository);
    }

    @Test
    void searchAndSave_rejectsInvalidSortBeforeCallingGithub() {
        assertThatThrownBy(()
                -> service.searchAndSave(new SearchRequest("spring", null, "popularity", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid sort");

        verifyNoInteractions(githubClient, repository);
    }

    // ---------- findStored ----------
    @ParameterizedTest
    @CsvSource({
        ",         stars",
        "stars,    stars",
        "forks,    forks",
        "updated,  lastUpdated",
        "FORKS,    forks"
    })
    void findStored_sortsDescendingByRequestedField(String sortParam, String expectedField) {
        when(repository.findAll(ArgumentMatchers.<Specification<GithubRepository>>any(), any(Sort.class)))
                .thenReturn(List.of());

        service.findStored("Java", 100, sortParam);

        verify(repository).findAll(ArgumentMatchers.<Specification<GithubRepository>>any(), sortCaptor.capture());
        Sort.Order order = sortCaptor.getValue().getOrderFor(expectedField);
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void findStored_rejectsInvalidSort() {
        assertThatThrownBy(() -> service.findStored(null, null, "abc"))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(repository);
    }

    @Test
    void findStored_mapsEntitiesToDtos() {
        GithubRepository entity = existingEntity(7L, "demo", 321);
        entity.setOwnerName("octocat");
        when(repository.findAll(ArgumentMatchers.<Specification<GithubRepository>>any(), any(Sort.class)))
                .thenReturn(List.of(entity));

        List<RepositoryDto> result = service.findStored(null, null, "stars");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(7L);
        assertThat(result.get(0).owner()).isEqualTo("octocat");
        assertThat(result.get(0).stars()).isEqualTo(321);
    }
}
