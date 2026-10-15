package com.example.githubsearcher.repository;

import com.example.githubsearcher.entity.GithubRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class GithubRepositoryRepositoryTest {

    @Autowired
    private GithubRepositoryRepository repository;

    private final Specification<GithubRepository> all = (root, query, cb) -> cb.conjunction();

    @BeforeEach
    void seed() {
        repository.saveAll(List.of(
                repo(1L, "alpha", "Java", 500, 50, "2024-03-01T00:00:00Z"),
                repo(2L, "beta", "Java", 100, 200, "2024-01-01T00:00:00Z"),
                repo(3L, "gamma", "Python", 900, 10, "2024-02-01T00:00:00Z"),
                repo(4L, "delta", "java", 50, 5, "2023-01-01T00:00:00Z")));
    }

    private GithubRepository repo(long id, String name, String language, int stars, int forks, String updated) {
        GithubRepository r = new GithubRepository();
        r.setId(id);
        r.setName(name);
        r.setOwnerName("owner");
        r.setLanguage(language);
        r.setStars(stars);
        r.setForks(forks);
        r.setLastUpdated(Instant.parse(updated));
        return r;
    }

    private List<Long> ids(Specification<GithubRepository> spec, Sort sort) {
        return repository.findAll(spec, sort).stream().map(GithubRepository::getId).toList();
    }

    @Test
    void languageFilterIsCaseInsensitive() {
        assertThat(ids(GithubRepositorySpecifications.hasLanguage("JAVA"), Sort.by("id")))
                .containsExactly(1L, 2L, 4L);
    }

    @Test
    void minStarsIsInclusive() {
        assertThat(ids(GithubRepositorySpecifications.hasMinStars(100), Sort.by("id")))
                .containsExactly(1L, 2L, 3L);
    }

    @Test
    void languageAndMinStarsCombine() {
        Specification<GithubRepository> spec = GithubRepositorySpecifications.hasLanguage("Java")
                .and(GithubRepositorySpecifications.hasMinStars(100));
        assertThat(ids(spec, Sort.by(Sort.Direction.DESC, "stars"))).containsExactly(1L, 2L);
    }

    @Test
    void noFiltersReturnsEverything() {
        assertThat(ids(all, Sort.by("id"))).containsExactly(1L, 2L, 3L, 4L);
    }

    @Test
    void noMatchReturnsEmptyList() {
        assertThat(ids(GithubRepositorySpecifications.hasLanguage("Cobol"), Sort.by("id"))).isEmpty();
    }

    @Test
    void sortsByStarsDescending() {
        assertThat(ids(all, Sort.by(Sort.Direction.DESC, "stars"))).containsExactly(3L, 1L, 2L, 4L);
    }

    @Test
    void sortsByForksDescending() {
        assertThat(ids(all, Sort.by(Sort.Direction.DESC, "forks"))).containsExactly(2L, 1L, 3L, 4L);
    }

    @Test
    void sortsByLastUpdatedDescending() {
        assertThat(ids(all, Sort.by(Sort.Direction.DESC, "lastUpdated"))).containsExactly(1L, 3L, 2L, 4L);
    }

    @Test
    void savingSameGithubIdUpdatesRowInsteadOfDuplicating() {
        repository.saveAndFlush(repo(1L, "alpha-renamed", "Java", 999, 50, "2024-03-01T00:00:00Z"));

        assertThat(repository.count()).isEqualTo(4);
        GithubRepository updated = repository.findById(1L).orElseThrow();
        assertThat(updated.getStars()).isEqualTo(999);
        assertThat(updated.getName()).isEqualTo("alpha-renamed");
    }
}
